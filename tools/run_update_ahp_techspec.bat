@echo off
setlocal

:: Get the directory where this script resides
set "SCRIPT_DIR=%~dp0"

:: Run the python script located in the same directory
python "%SCRIPT_DIR%update_ahp_techspec.py"

:: Pause to let user see the output
echo.
pause