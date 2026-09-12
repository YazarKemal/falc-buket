@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem ============================================================
rem  FalciBuket hang-safe tablet deploy script
rem  - Finite, single-purpose native commands only
rem  - No background processes, no Start-Process, no 2>&1
rem  - Never runs: adb start-server / adb kill-server
rem  - ADB server is owned and started externally
rem ============================================================

set "PKG=com.prompthavenai.falcibuket"
set "APK=app\build\outputs\apk\debug\app-debug.apk"
set "LAUNCH_CAT=android.intent.category.LAUNCHER"

cd /d "%~dp0.."
echo [deploy] repo root: %CD%

set "TMP_DEV=%TEMP%\falcibuket_devices.txt"
set "TMP_INST=%TEMP%\falcibuket_install.txt"
set "TMP_PID=%TEMP%\falcibuket_pid.txt"
set "TMP_LOG=%TEMP%\falcibuket_logcat.txt"
set "TMP_CRASH=%TEMP%\falcibuket_crash.txt"

rem ---------- 1. detect exactly one authorized device ----------
echo [deploy] querying adb devices...
adb devices > "%TMP_DEV%" 2>nul
if errorlevel 1 (
  echo [deploy] ERROR: "adb devices" failed. Is the externally managed adb server running?
  del "%TMP_DEV%" >nul 2>nul
  exit /b 10
)

set "SERIAL="
set "COUNT=0"
set "UNAUTH=0"
set "OFFLINE=0"
for /f "usebackq tokens=1,2" %%A in ("%TMP_DEV%") do (
  if /i "%%B"=="device" (
    set /a COUNT+=1
    set "SERIAL=%%A"
  )
  if /i "%%B"=="unauthorized" set /a UNAUTH+=1
  if /i "%%B"=="offline" set /a OFFLINE+=1
)
del "%TMP_DEV%" >nul 2>nul

if !COUNT! EQU 0 (
  if !UNAUTH! GTR 0 (
    echo [deploy] ERROR: the connected device is UNAUTHORIZED. Accept the USB debugging prompt on the tablet.
    exit /b 11
  )
  if !OFFLINE! GTR 0 (
    echo [deploy] ERROR: the connected device is OFFLINE. Reconnect the tablet.
    exit /b 13
  )
  echo [deploy] ERROR: no authorized physical Android device connected.
  exit /b 11
)
if !COUNT! GTR 1 (
  echo [deploy] ERROR: multiple authorized devices connected. Refusing to pick one at random.
  echo [deploy]        Disconnect all but one tablet and retry.
  exit /b 12
)

echo [deploy] target device: !SERIAL!

rem ---------- 2. build debug APK ----------
echo [deploy] building debug APK (no daemon)...
call gradlew.bat assembleDebug --console=plain --no-daemon -Pkotlin.compiler.execution.strategy=in-process --max-workers=1
if errorlevel 1 (
  echo [deploy] ERROR: gradle build failed.
  exit /b 20
)

if not exist "%APK%" (
  echo [deploy] ERROR: APK not found after build: %APK%
  exit /b 21
)
echo [deploy] APK present: %APK%

rem ---------- 3. install (replace, never uninstall) ----------
echo [deploy] installing to !SERIAL! keeping existing data...
adb -s !SERIAL! install -r "%APK%" > "%TMP_INST%"
if errorlevel 1 (
  echo [deploy] ERROR: adb install returned a failure code.
  type "%TMP_INST%"
  del "%TMP_INST%" >nul 2>nul
  exit /b 30
)
findstr /i /c:"Success" "%TMP_INST%" >nul
if errorlevel 1 (
  echo [deploy] ERROR: adb install did not report success.
  type "%TMP_INST%"
  del "%TMP_INST%" >nul 2>nul
  exit /b 31
)
del "%TMP_INST%" >nul 2>nul
echo [deploy] install: Success

rem ---------- 4. stop old process, clear logs, launch ----------
adb -s !SERIAL! shell am force-stop %PKG%
if errorlevel 1 (
  echo [deploy] ERROR: force-stop failed.
  exit /b 40
)
adb -s !SERIAL! logcat -c >nul 2>nul

echo [deploy] launching %PKG%...
adb -s !SERIAL! shell monkey -p %PKG% -c %LAUNCH_CAT% 1 >nul
if errorlevel 1 (
  echo [deploy] ERROR: launch via monkey failed.
  exit /b 50
)

rem brief settle wait without stdin dependency
ping -n 7 127.0.0.1 >nul

rem ---------- 5. verify process is alive ----------
set "PID="
adb -s !SERIAL! shell pidof %PKG% > "%TMP_PID%" 2>nul
set /p PID=<"%TMP_PID%"
del "%TMP_PID%" >nul 2>nul
if not defined PID (
  echo [deploy] ERROR: %PKG% is not running after launch.
  exit /b 60
)
echo [deploy] process alive (pid:!PID!)

rem ---------- 6. finite crash check ----------
adb -s !SERIAL! logcat -d -t 800 > "%TMP_LOG%" 2>nul
findstr /i /c:"FATAL EXCEPTION" /c:"E AndroidRuntime" "%TMP_LOG%" > "%TMP_CRASH%"
if not errorlevel 1 (
  echo [deploy] ERROR: crash signatures found in logcat:
  type "%TMP_CRASH%"
  del "%TMP_LOG%" >nul 2>nul
  del "%TMP_CRASH%" >nul 2>nul
  exit /b 70
)
del "%TMP_LOG%" >nul 2>nul
del "%TMP_CRASH%" >nul 2>nul
echo [deploy] crash check: clean

echo [deploy] DONE: build, install, launch and crash check succeeded.
exit /b 0
