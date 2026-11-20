@echo off
cd /d "%~dp0"
where pythonw >nul 2>nul
if %errorlevel%==0 (
    start "" pythonw sanden_pc.py
) else (
    echo Python 3.8+ not found. Install it from https://www.python.org/downloads/ and tick "Add to PATH".
    pause
)
