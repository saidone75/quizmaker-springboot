@echo off
setlocal

mvn clean package -DskipTests -Dlicense.skip=true -Pprod

endlocal
