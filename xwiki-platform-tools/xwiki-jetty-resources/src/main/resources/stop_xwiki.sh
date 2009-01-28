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

# Specify port and key to stop a running Jetty instance
JAVA_OPTS="-DSTOP.KEY=xwiki -DSTOP.PORT=8079"

java $JAVA_OPTS -Djetty.home=$JETTY_HOME -jar $JETTY_HOME/start.jar --stop
