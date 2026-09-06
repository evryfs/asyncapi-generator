@echo off
setlocal DisableDelayedExpansion
set "JAVA_EXE=java.exe"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
"%JAVA_EXE%" -jar "%~dp0..\lib\asyncapi-generator.jar" %*
exit /b %ERRORLEVEL%
