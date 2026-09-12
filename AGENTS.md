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
