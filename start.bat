@echo off
title Openfy - Spotify UI + RVX YT Music Engine
echo ========================================================
echo               OPENFY MUSIC STREAMING
echo     Spotify UI with RVX YouTube Music Audio Engine
echo ========================================================
echo.

echo [1/3] Starting Openfy Backend (FastAPI on http://127.0.0.1:8000)...
start "Openfy Backend" cmd /k "set PYTHONPATH=backend&& python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload"

timeout /t 2 /nobreak >nul

echo [2/3] Starting Openfy Frontend (Vite on http://localhost:5173)...
start "Openfy Frontend" cmd /k "cd frontend && npm.cmd run dev"

timeout /t 3 /nobreak >nul

echo [3/3] Launching Openfy in default browser...
start http://localhost:5173

echo.
echo ========================================================
echo Openfy is running! Enjoy ad-free music streaming!
echo Backend:  http://127.0.0.1:8000
echo Frontend: http://localhost:5173
echo ========================================================
pause
