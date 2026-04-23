@ECHO OFF
SETLOCAL

SET DIRNAME=%~dp0
IF "%DIRNAME%"=="" SET DIRNAME=.
SET APP_BASE_NAME=%~n0
SET APP_HOME=%DIRNAME%

SET DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"
SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

IF EXIST "%JAVA_HOME%\bin\java.exe" GOTO execute

ECHO ERROR: JAVA_HOME is not set correctly.
ECHO Please set the JAVA_HOME variable in your environment to match the
ECHO location of your Java installation.
GOTO fail

:execute
IF NOT EXIST "%CLASSPATH%" (
  ECHO ERROR: Missing %CLASSPATH%
  ECHO Run Android Studio sync once to generate/download wrapper JAR, or add gradle-wrapper.jar manually.
  GOTO fail
)

"%JAVA_HOME%\bin\java.exe" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
IF "%ERRORLEVEL%"=="0" GOTO end

:fail
EXIT /B 1

:end
ENDLOCAL
