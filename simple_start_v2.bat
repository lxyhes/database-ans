@echo off
setlocal EnableDelayedExpansion

:: Project Identity Configuration
set "PROJECT_NAME=Data Analysis Assistant"
set "PROJECT_ID=DAA"
set "WINDOW_TITLE_PREFIX=[%PROJECT_ID%]"

:RESTART
echo Starting %PROJECT_NAME%...
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

:: Kill existing project windows first (by window title, not process name)
echo Stopping existing %PROJECT_NAME% services...
call :StopProjectServices

timeout /t 2 /nobreak >nul

echo Existing services stopped.
echo.

:: Start iFlow CLI in a new window with project identifier
echo Starting iFlow CLI on port 8090...
start "%WINDOW_TITLE_PREFIX% %PROJECT_NAME% - iFlow CLI [Port:8090]" /D "%PROJECT_ROOT%" cmd /k "iflow --experimental-acp --port 8090"

timeout /t 3 /nobreak >nul

:: Start backend in a new window with project identifier
echo Starting backend service on port 9090...
start "%WINDOW_TITLE_PREFIX% %PROJECT_NAME% - Backend [SpringBoot]" /D "%BACKEND_DIR%" cmd /k "mvn spring-boot:run"

timeout /t 5 /nobreak >nul

:: Start frontend in a new window with project identifier
echo Starting frontend application with hot reload...
start "%WINDOW_TITLE_PREFIX% %PROJECT_NAME% - Frontend [Vite-NPM]" /D "%FRONTEND_DIR%" cmd /k "npm start"

echo.
echo ============================================
echo %PROJECT_NAME% services started:
echo   1. iFlow CLI (port 8090)
echo   2. Backend (port 9090)
echo   3. Frontend (with hot reload)
echo ============================================
echo.
echo Press R to Restart all services
echo Press Q to Quit and stop all services
echo.

set /p choice="Your choice (R/Q): "

if /i "%choice%"=="R" goto RESTART
if /i "%choice%"=="Q" goto QUIT

goto QUIT

:: Function to stop only this project's services
:StopProjectServices
:: Close windows by title (only project-related windows)
taskkill /F /FI "WINDOWTITLE eq %WINDOW_TITLE_PREFIX% %PROJECT_NAME% - iFlow CLI*" 2>nul
taskkill /F /FI "WINDOWTITLE eq %WINDOW_TITLE_PREFIX% %PROJECT_NAME% - Backend*" 2>nul
taskkill /F /FI "WINDOWTITLE eq %WINDOW_TITLE_PREFIX% %PROJECT_NAME% - Frontend*" 2>nul

:: Only kill node/java processes that are running from this project directory
:: This is done by checking the command line arguments
for /f "tokens=2 delims=," %%a in ('tasklist /FI "IMAGENAME eq node.exe" /FO CSV /NH 2^>nul') do (
    for /f "delims=" %%b in ('wmic process where "ProcessId=%%~a" get CommandLine /value 2^>nul ^| findstr /I "%FRONTEND_DIR%"') do (
        taskkill /PID %%~a /F 2>nul
    )
)

for /f "tokens=2 delims=," %%a in ('tasklist /FI "IMAGENAME eq java.exe" /FO CSV /NH 2^>nul') do (
    for /f "delims=" %%b in ('wmic process where "ProcessId=%%~a" get CommandLine /value 2^>nul ^| findstr /I "%BACKEND_DIR%"') do (
        taskkill /PID %%~a /F 2>nul
    )
)

exit /b

:QUIT
echo.
echo Stopping all %PROJECT_NAME% services...
call :StopProjectServices

echo All %PROJECT_NAME% services stopped.
timeout /t 2 /nobreak >nul
