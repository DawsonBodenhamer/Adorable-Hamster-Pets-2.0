@echo off
:: Runs the python script targeting 'develop' AND 'legacy' versions
python "%~dp0package_preview.py" --mode all
pause