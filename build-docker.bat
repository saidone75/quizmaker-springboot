@echo off
chcp 65001 >nul
set DIST_DIR=quizmaker
set IMAGE_NAME=quizmaker:latest
set JAR_NAME=quizmaker.jar

if exist %DIST_DIR% rmdir /S /Q %DIST_DIR%
mkdir %DIST_DIR%\log

echo [1/4] Building Docker image...
docker build -t %IMAGE_NAME% . -f docker/Dockerfile
if errorlevel 1 (
    echo Error during image build.
    exit /b 1
)

echo [2/4] Creating temporary container...
for /f %%i in ('docker create %IMAGE_NAME%') do set CONTAINER_ID=%%i

echo [3/4] Extracting %JAR_NAME% file from container...
docker cp %CONTAINER_ID%:/%JAR_NAME% .\%DIST_DIR%\%JAR_NAME%
if errorlevel 1 (
    echo Error copying file %JAR_NAME%.
    docker rm %CONTAINER_ID% >nul
    exit /b 1
)

echo [4/4] Cleaning up temporary container...
docker rm %CONTAINER_ID% >nul

echo.
echo ✅ %JAR_NAME% successfully extracted in directory %DIST_DIR%!
