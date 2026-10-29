@echo off
echo ============================================================
echo   BHP Logistics - Fleet Manager Local Development Server
echo ============================================================
echo.

if "%JWT_SECRET%"=="" set "JWT_SECRET=bhp-logistics-fleet-manager-dev-jwt-secret-key-min-32-bytes!"

echo [1/2] Android Network Configuration:
echo [Info] Android Emulator connects directly via http://10.0.2.2:8080 natively.

if not exist "C:\Android\Sdk\platform-tools\adb.exe" goto :start_server

"C:\Android\Sdk\platform-tools\adb.exe" get-state >nul 2>&1
if not errorlevel 1 (
    "C:\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080 >nul 2>&1
    echo [Info] Connected Android device detected. Configured ADB reverse tcp:8080.
    goto :start_server
)

start /b "" "C:\Android\Sdk\platform-tools\adb.exe" wait-for-device reverse tcp:8080 tcp:8080 >nul 2>&1
echo [Info] Waiting for devices in background (none currently connected; startup continuing).

:start_server
echo.
echo [2/2] Starting Spring Boot Backend Server (Port 8080)...
cd /d "%~dp0backend"
call mvnw.cmd spring-boot:run

pause
