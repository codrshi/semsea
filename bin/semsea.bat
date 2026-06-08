@echo off
setlocal

set "JAR=%~dp0semsea.jar"
if not exist "%JAR%" set "JAR=%~dp0..\target\semsea.jar"
if not exist "%JAR%" (
    echo semsea.jar not found.
    echo Build it first with: mvn package
    exit /b 1
)

java --enable-native-access=ALL-UNNAMED -jar "%JAR%" %*
endlocal
