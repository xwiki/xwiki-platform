#!/bin/sh
# ---------------------------------------------------------------------------
# See the NOTICE file distributed with this work for additional
# information regarding copyright ownership.
#
# This is free software; you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation; either version 2.1 of
# the License, or (at your option) any later version.
#
# This software is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this software; if not, write to the Free
# Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
# 02110-1301 USA, or see the FSF site: http://www.fsf.org.
# ---------------------------------------------------------------------------

# ---------------------------------------------------------------------------
# Optional ENV vars
# -----------------
#   XWIKI_OPTS - parameters passed to the Java VM when running XWiki
#     e.g. to increase the memory allocated to the JVM to 1GB, use
#       set XWIKI_OPTS=-Xmx1024m
#   JETTY_STOP_PORT - the port on which Jetty listens for a Stop command, 8079 by default
# ---------------------------------------------------------------------------

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

# The port on which Jetty listens for a Stop command can be defined in an enviroment variable called JETTY_STOP_PORT
if [ -z "$JETTY_STOP_PORT" ]; then
  # Alternatively, it can be passed to this script as the first argument
  if [ -n "$1" ]; then
    JETTY_STOP_PORT=$1
  else
    JETTY_STOP_PORT=8079
  fi
fi

# Specify port and key to stop a running Jetty instance
XWIKI_OPTS="$XWIKI_OPTS -DSTOP.KEY=xwiki -DSTOP.PORT=$JETTY_STOP_PORT"

# Specify Jetty's home directory
XWIKI_OPTS="$XWIKI_OPTS -Djetty.home=$JETTY_HOME"

java $XWIKI_OPTS -jar $JETTY_HOME/start.jar --stop

# Wait till the XWiki lock file is removed by the start script
# Wait 10 seconds at most and exits if the lock file hasn't been removed after
# 10 seconds.
timer=0
while [ "$timer" -lt 100 -a -e xwiki.lck ]; do
  timer=$((timer+1))
  sleep .1
done
