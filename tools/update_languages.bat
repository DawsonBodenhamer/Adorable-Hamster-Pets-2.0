@echo off
title Update Language Files
echo Running language update script...

:: %~dp0 dynamically gets the folder path where this .bat file is located
python "%~dp0update_languages.py"

echo.
pause