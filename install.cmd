@echo off
rem ---------------------------------------------------------------------------
rem  Windows entry point for the semsea installer.
rem
rem  Plain PowerShell refuses to run install.ps1 on most Windows machines
rem  because of the default execution policy (Restricted / RemoteSigned).
rem  This wrapper invokes powershell.exe with -ExecutionPolicy Bypass *for a
rem  single process only*, which is the Microsoft-blessed way to run a
rem  trusted local script without modifying machine-wide policy.
rem
rem  Usage:
rem    install.cmd                   - default install dir
rem    install.cmd -InstallDir "..."  - custom install dir
rem
rem  No administrator rights are required: the installer writes only to
rem  %LOCALAPPDATA% and the user-scoped PATH.
rem ---------------------------------------------------------------------------

setlocal

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0install.ps1" %*
set "RC=%ERRORLEVEL%"

if not "%RC%"=="0" (
    echo.
    echo Installer exited with code %RC%.
    echo See the messages above for details.
)

endlocal & exit /b %RC%
