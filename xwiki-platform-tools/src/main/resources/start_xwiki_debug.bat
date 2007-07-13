@echo off
set LANG=fr_FR.ISO8859-1
set JETTY_HOME=.
set JETTY_PORT=8080
set JAVA_OPTS=-Xmx300m -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

# Jetty requires a logs directory to exist
if not exist logs mkdir logs

java %JAVA_OPTS% -Dfile.encoding=iso-8859-1 -Djetty.home=%JETTY_HOME% -Djetty.port=%JETTY_PORT% -jar %JETTY_HOME%/start.jar
