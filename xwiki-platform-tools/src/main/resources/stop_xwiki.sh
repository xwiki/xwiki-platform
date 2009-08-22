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

JETTY_HOME=jetty

# The port on which to stop Jetty can be passed to this script as the first argument
if [ -n "$1" ]; then
  JETTY_STOPPORT=$1
else
  JETTY_STOPPORT=8079
fi

# Specify port and key to stop a running Jetty instance
JAVA_OPTS="-DSTOP.KEY=xwiki -DSTOP.PORT=$JETTY_STOPPORT"

java $JAVA_OPTS -Djetty.home=$JETTY_HOME -jar $JETTY_HOME/start.jar --stop
