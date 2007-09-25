@echo off
set LANG=fr_FR.ISO8859-1
set JETTY_HOME=.
set JETTY_PORT=8080
set JAVA_OPTS=-Xmx300m

REM Ensure the logs directory exists as otherwise Jetty reports an error
if not exist logs mkdir logs

REM Ensure the work directory exists so that Jetty uses it for its temporary files.
if not exist work mkdir work

java %JAVA_OPTS% -Dfile.encoding=iso-8859-1 -Djetty.home=%JETTY_HOME% -Djetty.port=%JETTY_PORT% -jar %JETTY_HOME%/start.jar
