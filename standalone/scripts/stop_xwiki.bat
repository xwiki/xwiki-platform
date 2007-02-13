@echo off
set JETTY_HOME=.

java -Djetty.home=%JETTY_HOME% -jar %JETTY_HOME%/stop.jar

