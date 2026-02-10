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

:: Start backend in a new window
echo Starting backend service on port 9090...
start "Data Analysis Assistant - Backend" cmd /k "cd /d "%BACKEND_DIR%" && mvn spring-boot:run -Dserver.port=9090"

:: Wait a bit for backend to start
timeout /t 5

:: Start frontend in a new window
echo Starting frontend application...
start "Data Analysis Assistant - Frontend" cmd /k "cd /d "%FRONTEND_DIR%" && npm start"

echo.
echo Both services have been started in separate windows.
echo Check the opened windows for any errors.
pause