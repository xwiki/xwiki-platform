#!/bin/bash
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

# ----------------------------------------------------------------------------------------------------------------
# Optional ENV vars
# -----------------
#   XWIKI_OPTS - parameters passed to the Java VM when running XWiki e.g. to increase the memory allocated to the
#       JVM to 1GB, use set XWIKI_OPTS=-Xmx1024m
#   JETTY_STOP_PORT - the port on which Jetty listens for a Stop command.
#
# Optional Parameters
# -------------------
#   -p, --port: The Jetty HTTP port that was used to start XWiki. Defaults to 8080.
#   -sp, --stopport: The Jetty stop port to use. Overrides any value from JETTY_STOP_PORT. Defaults to 8079.
#   -ld, --lockdir: The directory where the executing process id is stored to verify that that only one instance is
#       started. Defaults to /var/tmp.
#
# Example
# -------
#   stop_xwiki.sh -sp 8079
# ----------------------------------------------------------------------------------------------------------------

waitForLockDeletion() {
  # Wait till the XWiki lock file is removed by the start script
  # Wait 20 seconds at most and exits if the lock file hasn't been removed after that.
  local ctr=0;
  while [ $ctr -lt 200 ]; do
    ctr=$((ctr+1));
    [ ! -e $1 ] && return 0;
    sleep .1;
  done
  return 100
}

# Ensure that the commands below are always started in the directory where this script is located.
# To do this we compute the location of the current script.
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

# The port on which to start Jetty can be defined in an environment variable called JETTY_PORT
if [ -z "$JETTY_PORT" ]; then
  JETTY_PORT=8080
fi

# The port on which Jetty listens for a Stop command can be defined in an environment variable called JETTY_STOP_PORT
if [ -z "$JETTY_STOP_PORT" ]; then
  JETTY_STOP_PORT=8079
fi

# The location where to store the process id
XWIKI_LOCK_DIR="/var/tmp"

# Parse script parameters
while [[ $# > 0 ]]; do
  key="$1"
  shift
  case $key in
    -p|--port)
      JETTY_PORT="$1"
      shift
      ;;
    -sp|--stopport)
      JETTY_STOP_PORT="$1"
      shift
      ;;
    -ld|--lockdir)
      XWIKI_LOCK_DIR="$1"
      shift
      ;;
    *)
      # unknown option
      ;;
  esac
done

# Check if a lock file already exists for the specified port  which means an XWiki instance is already running
XWIKI_LOCK_FILE="${XWIKI_LOCK_DIR}/xwiki-${JETTY_PORT}.lck"

# Specify port and key to stop a running Jetty instance
XWIKI_OPTS="$XWIKI_OPTS -DSTOP.KEY=xwiki -DSTOP.PORT=$JETTY_STOP_PORT"

# Specify Jetty's home directory
XWIKI_OPTS="$XWIKI_OPTS -Djetty.home=$JETTY_HOME"

[ ! -e $XWIKI_LOCK_FILE ] && echo "Lock [${XWIKI_LOCK_FILE}] missing" && exit 0;

echo 'Attempting to stop XWiki cleanly...';

java $XWIKI_OPTS -jar $JETTY_HOME/start.jar --stop
waitForLockDeletion $XWIKI_LOCK_FILE && exit 0;

echo 'Failed to stop XWiki cleanly, attempting kill...';

kill `cat $XWIKI_LOCK_FILE`
waitForLockDeletion $XWIKI_LOCK_FILE && exit 0;

echo 'Failed to kill XWiki, attempting kill -9...';

kill -9 `cat $XWIKI_LOCK_FILE`
waitForLockDeletion $XWIKI_LOCK_FILE && exit 0;

if ps -p `cat $XWIKI_LOCK_FILE` > /dev/null; then
  echo Failed to kill XWiki, giving up!
fi
rm -f $XWIKI_LOCK_FILE