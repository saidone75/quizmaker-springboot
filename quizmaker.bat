@ECHO OFF
SETLOCAL

SET APP_NAME=quizmaker
SET IMAGE_NAME=quizmaker:latest
SET CONTAINER_NAME=quizmaker
SET DOCKERFILE=docker\Dockerfile

REM Optional: expose port 8080
SET PORTS=-p 8080:8080

REM Check parameter
IF "%~1"=="" (
    echo Usage: %~nx0 {build^|build_start^|start^|stop^|purge^|tail}
    GOTO END
)

IF /I "%~1"=="build" (
    CALL :build
    GOTO END
)

IF /I "%~1"=="build_start" (
    CALL :down
    CALL :build
    CALL :start
    CALL :tail
    GOTO END
)

IF /I "%~1"=="start" (
    CALL :start
    CALL :tail
    GOTO END
)

IF /I "%~1"=="stop" (
    CALL :down
    GOTO END
)

IF /I "%~1"=="purge" (
    CALL :down
    CALL :purge
    GOTO END
)

IF /I "%~1"=="tail" (
    CALL :tail
    GOTO END
)

echo Usage: %~nx0 {build^|build_start^|start^|stop^|purge^|tail}

:END
ENDLOCAL
EXIT /B %ERRORLEVEL%

:build
docker build -t %IMAGE_NAME% . -f %DOCKERFILE%
EXIT /B %ERRORLEVEL%

:start
docker ps -a --format "{{.Names}}" | findstr /I /X "%CONTAINER_NAME%" >NUL
IF %ERRORLEVEL%==0 (
    docker start %CONTAINER_NAME%
) ELSE (
    docker run -d %PORTS% --name %CONTAINER_NAME% %IMAGE_NAME%
)
EXIT /B %ERRORLEVEL%

:down
docker ps -a --format "{{.Names}}" | findstr /I /X "%CONTAINER_NAME%" >NUL
IF %ERRORLEVEL%==0 (
    docker stop %CONTAINER_NAME% >NUL 2>&1
    docker rm %CONTAINER_NAME% >NUL 2>&1
)
EXIT /B 0

:tail
docker logs -f %CONTAINER_NAME%
EXIT /B %ERRORLEVEL%

:purge
docker image rm -f %IMAGE_NAME%
EXIT /B 0