# AGENTS.md

## Windows OpenCode Shell Rules

- Windows host uses native Windows commands.
- Never append PowerShell `2>&1` to Gradle, npm, adb or git commands.
- Prefer `cmd.exe /d /s /c` for .bat/.cmd tools.
- Long-running commands must be executed one at a time.
- Never use Start-Process with redirected stdout/stderr.
- Never invoke interactive commands requiring stdin from the OpenCode tool.
- Gradle verification should use `--console=plain --no-daemon`.
- Do not mistake PowerShell NativeCommandError stream wrapping for a project compilation error; inspect the actual native process exit code/output.
- If a command appears stuck after its child process has completed, stop it and retry via cmd.exe rather than waiting indefinitely.

## Windows OpenCode Process Safety

The external PowerShell session owns and starts the ADB server before OpenCode starts. Do NOT start, stop, restart, or daemonize ADB from OpenCode.

1. NEVER run `adb start-server` or `adb kill-server`.
2. Ordinary finite ADB commands are allowed (e.g. `adb devices -l`, `adb install -r ...`, `adb shell am force-stop ...`, `adb shell monkey ...`, `adb shell pidof ...`).
3. Never start persistent background processes from the shell tool.
4. Never run `Start-Process` with redirected stdout/stderr.
5. Never append `2>&1` to native commands.
6. Never use `| Out-Null` for native build/deploy commands.
7. Run ONE native command per shell tool call.
8. For Gradle always use:

   cmd.exe /d /s /c "gradlew.bat test --console=plain --no-daemon -Pkotlin.compiler.execution.strategy=in-process --max-workers=1"

   cmd.exe /d /s /c "gradlew.bat assembleDebug --console=plain --no-daemon -Pkotlin.compiler.execution.strategy=in-process --max-workers=1"

   Purpose: avoid the Gradle daemon, Kotlin compiler daemon, and persistent descendant processes.
9. Do not use Gradle continuous mode.
10. Do not start Android Studio, emulators, dev servers or watch processes from OpenCode.
11. If a native command has already produced its expected final output but the OpenCode shell tool remains running, do not wait indefinitely. Stop the call and report `OPENCODE_WINDOWS_SHELL_HANG`.
12. Do not interpret an OpenCode shell hang as an application failure.
