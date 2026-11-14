# Openfy PowerShell Launcher
Write-Host "========================================================" -ForegroundColor Green
Write-Host "             OPENFY MUSIC STREAMING" -ForegroundColor White
Write-Host "   Spotify UI with RVX YouTube Music Audio Engine" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Green
Write-Host ""

Write-Host "[1/3] Starting Openfy Backend (FastAPI on http://127.0.0.1:8000)..." -ForegroundColor Yellow
Start-Process -FilePath "cmd.exe" -ArgumentList '/k "set PYTHONPATH=backend&& python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload"'

Start-Sleep -Seconds 2

Write-Host "[2/3] Starting Openfy Frontend (Vite on http://localhost:5173)..." -ForegroundColor Yellow
Start-Process -FilePath "cmd.exe" -ArgumentList '/k "cd frontend && npm.cmd run dev"'

Start-Sleep -Seconds 3

Write-Host "[3/3] Opening browser at http://localhost:5173..." -ForegroundColor Green
Start-Process "http://localhost:5173"

Write-Host "Openfy is running successfully!" -ForegroundColor Green
