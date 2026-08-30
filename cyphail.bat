@echo off
rem Cyphail - Grupo 4 (Kenny, Sebastian, Moya) - EIF400-II-2026-CLoria
rem Wrapper minimo: solo verifica que java este disponible y le pasa los
rem argumentos tal cual al jar. Sin logica adicional.

where java >nul 2>nul
if errorlevel 1 (
    echo ERROR: java was not found in PATH. Install a JDK and try again.
    exit /b 1
)

set "CYPHAIL_HOME=%~dp0"
java -jar "%CYPHAIL_HOME%target\cyphail.jar" %*
