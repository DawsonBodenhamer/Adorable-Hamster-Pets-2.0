@echo off
:: Runs the python script targeting the 'develop' version defined in the script
python "%~dp0package_preview.py" --mode develop
pause