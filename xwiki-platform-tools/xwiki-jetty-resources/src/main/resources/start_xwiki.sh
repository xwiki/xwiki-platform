#!/bin/sh

# Ensure that the commands below are always started in the directory where this script is
# located. To do this we compute the location of the current script.

PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
PRGDIR=`dirname "$PRG"`
cd "$PRGDIR"

JETTY_HOME=.
JAVA_OPTS=-Xmx300m

# The port on which to start Jetty can be passed to this script as the first argument
if [ -n "$1" ]; then
  JETTY_PORT=$1
else
  JETTY_PORT=8080
fi

echo Starting Jetty on port $JETTY_PORT ...
echo Logs are in the $PRGDIR/xwiki.log file

# Ensure the logs directory exists as otherwise Jetty reports an error
mkdir -p logs 2>/dev/null

# Ensure the work directory exists so that Jetty uses it for its temporary files.
mkdir -p work 2>/dev/null

LANG=fr_FR.ISO8859-1 java $JAVA_OPTS -Dfile.encoding=iso-8859-1 -Djetty.port=$JETTY_PORT -Djetty.home=$JETTY_HOME -jar $JETTY_HOME/start.jar
