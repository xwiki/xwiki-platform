#!/bin/sh

export LANG=fr_FR.ISO8859-1
JETTY_HOME=.
JETTY_PORT=8080
JAVA_OPTS=-Xmx300m

echo Starting Jetty on port $JETTY_PORT ...
echo Logs are in the logs/ directory

mkdir -p logs 2>/dev/null
nohup java $JAVA_OPTS -Dfile.encoding=iso-8859-1 -Djetty.port=$JETTY_PORT -Djetty.home=$JETTY_HOME -jar $JETTY_HOME/start.jar > logs/xwiki_output.log 2> logs/xwiki_errors.log &

