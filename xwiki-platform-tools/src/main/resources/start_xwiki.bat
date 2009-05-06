@echo off
set LANG=fr_FR.ISO8859-1
set JETTY_HOME=jetty
set JETTY_PORT=8080
set JAVA_OPTS=-Xmx300m

REM Ensure the logs directory exists as otherwise Jetty reports an error
if not exist %JETTY_HOME%\logs mkdir %JETTY_HOME%\logs

REM Ensure the work directory exists so that Jetty uses it for its temporary files.
if not exist %JETTY_HOME%\work mkdir %JETTY_HOME%\work

REM Specify port and key to stop a running Jetty instance
set JAVA_OPTS=%JAVA_OPTS% -DSTOP.KEY=xwiki -DSTOP.PORT=8079

java %JAVA_OPTS% -Dfile.encoding=UTF8 -Djetty.home=%JETTY_HOME% -Djetty.port=%JETTY_PORT% -jar %JETTY_HOME%/start.jar
