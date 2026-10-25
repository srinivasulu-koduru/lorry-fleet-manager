@echo off
echo ============================================================
echo   BHP Logistics - Fleet Manager Local Development Server
echo ============================================================
echo.

echo [1/3] Setting up ADB reverse tunnel for Android Emulator (port 8080)...
if exist "C:\Android\Sdk\platform-tools\adb.exe" (
    "C:\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080
    echo ADB reverse forwarding enabled: tcp:8080 -> tcp:8080
) else (
    echo Warning: adb.exe not found at standard path. Ensure adb reverse tcp:8080 tcp:8080 is configured.
)
echo.

echo [2/3] Starting Spring Boot Backend Server...
cd /d "%~dp0backend"
call .\mvnw.cmd spring-boot:run

pause
