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
#   JETTY_OPTS - optional parameters passed to Jetty's start.jar. See
#       http://www.eclipse.org/jetty/documentation/9.2.3.v20140905/start-jar.html for options.
#   JETTY_PORT - the port on which Jetty was started.
#   JETTY_STOP_PORT - the port on which Jetty listens for a Stop command.
# ----------------------------------------------------------------------------------------------------------------

usage() {
  echo "Usage: stop_xwiki.sh <optional parameters>"
  echo "-p, --port: The Jetty HTTP port that was used to start XWiki. Defaults to 8080."
  echo "-sp, --stopport: The Jetty stop port to use. Overrides any value from JETTY_STOP_PORT. Defaults to 8079."
  echo "-ld, --lockdir: The directory where the executing process id is stored to verify that that only one instance"
  echo "    is started. Defaults to /var/tmp."
  echo "-wt, --waittime: Number of seconds to wait for the XWiki lock file to be removed before exiting."
  echo ""
  echo "Example: stop_xwiki.sh -p 8080 -sp 8079"
}

waitForLockDeletion() {
  # Wait till the XWiki lock file is removed by the start script
  # Wait 30 seconds (or the time defined by the user) at most and exits if the lock file hasn't been removed after
  # that.
  local ctr=0;
  while [ $ctr -lt $(($XWIKI_WAIT_TIME*10)) ]; do
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

# The number of seconds to wait for the XWiki lock file to be removed before exiting.
XWIKI_WAIT_TIME=30

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
    -wt|--waittime)
      XWIKI_WAIT_TIME="$1"
      shift
      ;;
    -h|--help)
      usage
      exit 1
      ;;
    *)
      # unknown option
      usage
      exit 1
      ;;
  esac
done

# Check if a lock file already exists for the specified port  which means an XWiki instance is already running
XWIKI_LOCK_FILE="${XWIKI_LOCK_DIR}/xwiki-${JETTY_PORT}.lck"

# Location where XWiki stores generated data and where database files are.
XWIKI_DATA_DIR=${xwikiDataDir}

# Specify Jetty's home and base directories
JETTY_HOME=jetty
JETTY_BASE=.
XWIKI_OPTS="$XWIKI_OPTS -Djetty.home=$JETTY_HOME -Djetty.base=$JETTY_BASE"

# Specify port and key to stop a running Jetty instance
JETTY_OPTS="$JETTY_OPTS STOP.KEY=xwiki STOP.PORT=$JETTY_STOP_PORT"

# Check version of Java

# Returns the Java version.
# 8 for 1.8.0_nn, 9 for 9-ea etc, and "no_java" for undetected
java_version() {
  local result
  local java_cmd
  if [[ -n $(type -p java) ]]; then
    java_cmd=java
  elif [[ (-n "$JAVA_HOME") && (-x "$JAVA_HOME/bin/java") ]]; then
    java_cmd="$JAVA_HOME/bin/java"
  fi
  local IFS=$'\n'
  # remove \r for Cygwin
  local lines=$("$java_cmd" -Xms32M -Xmx32M -version 2>&1 | tr '\r' '\n')
  if [[ -z $java_cmd ]]; then
    result=no_java
  else
    for line in $lines; do
      if [[ (-z $result) && ($line = *"version \""*) ]]; then
        local ver=$(echo $line | sed -e 's/.*version "\(.*\)"\(.*\)/\1/; 1q')
        # on macOS, sed doesn't support '?'
        if [[ $ver = "1."* ]]; then
          result=$(echo $ver | sed -e 's/1\.\([0-9]*\)\(.*\)/\1/; 1q')
        else
          result=$(echo $ver | sed -e 's/\([0-9]*\)\(.*\)/\1/; 1q')
        fi
      fi
    done
  fi
  echo "$result"
}
JAVA_VERSION="$(java_version)"
if [[ "$JAVA_VERSION" -eq "no_java" ]]; then
  echo "No Java found. You need Java installed to use this script."
  exit 0
fi
if [ "$JAVA_VERSION" -lt ${xwiki.java.version} ]; then
  echo This script requires Java ${xwiki.java.version} or greater.
  exit 0
fi

[ ! -e $XWIKI_LOCK_FILE ] && echo "Lock file [${XWIKI_LOCK_FILE}] is missing. Aborting stop." && exit 0

if ps -p `cat $XWIKI_LOCK_FILE` > /dev/null; then
  # An XWiki instance is still running

  echo Attempting to stop XWiki cleanly on port ${JETTY_PORT}...
  java $XWIKI_OPTS -jar $JETTY_HOME/start.jar --stop $JETTY_OPTS
  waitForLockDeletion $XWIKI_LOCK_FILE && exit 0

  echo 'Failed to stop XWiki cleanly, attempting kill...'
  kill `cat $XWIKI_LOCK_FILE`
  waitForLockDeletion $XWIKI_LOCK_FILE && exit 0

  echo 'Failed to kill XWiki, attempting kill -9...'
  kill -9 `cat $XWIKI_LOCK_FILE`
  waitForLockDeletion $XWIKI_LOCK_FILE && exit 0

  if ps -p `cat $XWIKI_LOCK_FILE` > /dev/null; then
    echo "Failed to kill XWiki, giving up!"
  fi
  rm -f $XWIKI_LOCK_FILE
else
  echo "Lock file [${XWIKI_LOCK_FILE}] doesn't point to any running XWiki instance. Aborting stop." && exit 0
fi
