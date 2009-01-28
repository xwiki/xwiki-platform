@echo off
set JETTY_HOME=jetty

REM Specify port and key to stop a running Jetty instance
set JAVA_OPTS=-DSTOP.KEY=xwiki -DSTOP.PORT=8079

java %JAVA_OPTS% -Djetty.home=%JETTY_HOME% -jar %JETTY_HOME%/start.jar --stop

