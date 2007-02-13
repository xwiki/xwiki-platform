@echo off
set LANG=fr_FR.ISO8859-1
set JETTY_HOME=.
set JETTY_PORT=8080
set JAVA_OPTS=-Xmx300m -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

java %JAVA_OPTS% -Dfile.encoding=iso-8859-1 -Djetty.home=%JETTY_HOME% -Djetty.port=%JETTY_PORT% -jar %JETTY_HOME%/start.jar
