@echo off
setlocal

:: Get the directory where this script resides
set "SCRIPT_DIR=%~dp0"

:: Run with Verbose logging (-v) AND Structure Only mode (--structure-only)
python "%SCRIPT_DIR%update_ahp_techspec.py" -v --structure-only

:: Pause to view output
echo.
pause