@echo off
echo Starting Data Analysis Assistant...
echo.

:: Set project paths
set PROJECT_ROOT=%~dp0
set BACKEND_DIR=%PROJECT_ROOT%backend
set FRONTEND_DIR=%PROJECT_ROOT%frontend

echo Project Root: %PROJECT_ROOT%
echo Backend Directory: %BACKEND_DIR%
echo Frontend Directory: %FRONTEND_DIR%
echo.

:: Check if directories exist
if not exist "%BACKEND_DIR%" (
    echo ERROR: Backend directory does not exist!
    pause
    exit /b 1
)

if not exist "%FRONTEND_DIR%" (
    echo ERROR: Frontend directory does not exist!
    pause
    exit /b 1
)

echo Directories exist. Proceeding...
echo.

:: Kill existing processes first (only those started by this script)
echo Stopping existing services...
taskkill /F /FI "WINDOWTITLE eq Data Analysis Assistant - iFlow CLI*" 2>nul
taskkill /F /FI "WINDOWTITLE eq Data Analysis Assistant - Backend*" 2>nul
taskkill /F /FI "WINDOWTITLE eq Data Analysis Assistant - Frontend*" 2>nul

:: Wait for processes to terminate
timeout /t 2 /nobreak >nul

echo Existing services stopped.
echo.

:: Start iFlow CLI in a new window
echo Starting iFlow CLI on port 8090...
start "Data Analysis Assistant - iFlow CLI" cmd /k "iflow --experimental-acp --port 8090"

:: Wait a bit for iFlow to start
timeout /t 3

:: Start backend in a new window
echo Starting backend service on port 9090...
start "Data Analysis Assistant - Backend" cmd /k "cd /d "%BACKEND_DIR%" && mvn spring-boot:run"

:: Wait a bit for backend to start
timeout /t 5

:: Start frontend in a new window with hot reload
echo Starting frontend application with hot reload...
start "Data Analysis Assistant - Frontend" cmd /k "cd /d "%FRONTEND_DIR%" && npm start"

echo.
echo All services have been started in separate windows:
echo   1. iFlow CLI (port 8090)
echo   2. Backend (port 9090)
echo   3. Frontend (with hot reload)
echo.
echo Check the opened windows for any errors.
pause
