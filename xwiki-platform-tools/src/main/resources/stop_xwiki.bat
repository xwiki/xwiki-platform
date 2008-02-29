@echo off
set JETTY_HOME=jetty
set JETTY_PORT=8080

java -Djetty.home=%JETTY_HOME% -Djetty.port=%JETTY_PORT% -jar %JETTY_HOME%/stop.jar

