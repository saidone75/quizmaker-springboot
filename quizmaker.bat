@ECHO OFF
SETLOCAL

SET COMPOSE_CMD=docker compose -f docker\docker-compose.yml

REM Check parameter
IF "%~1"=="" (
    echo Usage: %~nx0 {build^|build_start^|start^|stop^|restart^|purge^|tail}
    GOTO END
)

IF /I "%~1"=="build" (
    CALL :build
    GOTO END
)

IF /I "%~1"=="build_start" (
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

IF /I "%~1"=="restart" (
    CALL :down
    CALL :start
    CALL :tail
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

echo Usage: %~nx0 {build^|build_start^|start^|stop^|restart^|purge^|tail}

:END
ENDLOCAL
EXIT /B %ERRORLEVEL%

:build
%COMPOSE_CMD% build --no-cache
EXIT /B %ERRORLEVEL%

:start
%COMPOSE_CMD% up -d
EXIT /B %ERRORLEVEL%

:down
%COMPOSE_CMD% down
EXIT /B %ERRORLEVEL%

:tail
%COMPOSE_CMD% logs -f
EXIT /B %ERRORLEVEL%

:purge
%COMPOSE_CMD% down --rmi local --remove-orphans
EXIT /B %ERRORLEVEL%