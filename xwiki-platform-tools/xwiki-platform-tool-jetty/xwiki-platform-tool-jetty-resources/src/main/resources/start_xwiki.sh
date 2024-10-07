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
#   JETTY_OPTS - optional parameters passed to Jetty's start.jar. For example to list the configuration that will
#       execute, try setting it to "--list-config". See
#       http://www.eclipse.org/jetty/documentation/current/start-jar.html for more options.
#   JETTY_PORT - the port on which to start Jetty.
#   JETTY_STOP_PORT - the port on which Jetty listens for a Stop command.
# ----------------------------------------------------------------------------------------------------------------

usage() {
  echo "Usage: start_xwiki.sh <optional parameters>"
  echo "-p, --port: The Jetty HTTP port to use. Overrides any value from JETTY_PORT. Defaults to 8080."
  echo "-sp, --stopport: The Jetty stop port to use. Overrides any value from JETTY_STOP_PORT. Defaults to 8079."
  echo "-ld, --lockdir: The directory where the executing process id is stored to verify that that only one instance"
  echo "    is started. Defaults to /var/tmp."
  echo "-j, --jmx: Allows monitoring/managing Jetty through JMX."
  echo "-ni, --noninteractive: Don't ask questions to the user. Useful when called in an automated script."
  echo ""
  echo "Example: start_xwiki.sh -p 8080 -sp 8079"
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

# If no XWIKI_OPTS env variable has been defined use default values.
if [ -z "$XWIKI_OPTS" ] ; then
  XWIKI_OPTS="-Xmx1024m"
fi

# The port on which to start Jetty can be defined in an environment variable called JETTY_PORT
if [ -z "$JETTY_PORT" ]; then
  JETTY_PORT=8080
fi

# The port on which Jetty listens for a Stop command can be defined in an environment variable called JETTY_STOP_PORT
if [ -z "$JETTY_STOP_PORT" ]; then
  JETTY_STOP_PORT=8079
fi

# Make sure the standard Java tmpdir is isolated per instance (by default Jetty provides applications work dir in the Java tmpdir)
JAVA_TMP="${PRGDIR}/tmp"
XWIKI_OPTS="$XWIKI_OPTS -Djava.io.tmpdir=${JAVA_TMP}"
# Make sure the Java tmpdir exist since Jenkins does not create it
if [ ! -d ${JAVA_TMP} ]; then
  mkdir ${JAVA_TMP}
fi

# The location where to store the process id
XWIKI_LOCK_DIR="${JAVA_TMP}"

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
    -j|--jmx)
      JETTY_OPTS="$JETTY_OPTS --module=jmx"
      shift
      ;;
    -ni|--noninteractive)
      XWIKI_NONINTERACTIVE=true
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

if [ -e $XWIKI_LOCK_FILE ]; then
  # Note that there could be rare cases when the computer was rebooted without Jetty stopped and when it restarted
  # another process used the same process id...
  if ps -p `cat $XWIKI_LOCK_FILE` > /dev/null; then
    echo An XWiki instance is already running on port ${JETTY_PORT}. Aborting...
    echo Consider calling stop_xwiki.sh to stop it.
    exit 1
  else
    echo An XWiki lock file exists at ${XWIKI_LOCK_FILE} but no XWiki is executing. Removing lock file...
    rm -f $XWIKI_LOCK_FILE
  fi
fi

# Location where XWiki stores generated data and where database files are.
XWIKI_DATA_DIR=${xwikiDataDir}
XWIKI_OPTS="$XWIKI_OPTS -Dxwiki.data.dir=$XWIKI_DATA_DIR"

# Catch any Out Of Memory to make easier to analyze it
XWIKI_OPTS="$XWIKI_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$XWIKI_DATA_DIR"

# Ensure the data directory exists so that XWiki can use it for storing permanent data.
mkdir -p $XWIKI_DATA_DIR 2>/dev/null

# Ensure the logs directory exists as otherwise Jetty reports an error
mkdir -p $XWIKI_DATA_DIR/logs 2>/dev/null

# Set up the Jetty Base directory (used for custom Jetty configuration) to be the current directory where this file is.
# Also make sure the log directory exists since Jetty won't create it.
JETTY_BASE=.
mkdir -p $JETTY_BASE/logs 2>/dev/null

# Specify Jetty's home directory to be the directory named jetty inside the jetty base directory.
JETTY_HOME=jetty
XWIKI_OPTS="$XWIKI_OPTS -Djetty.home=$JETTY_HOME -Djetty.base=$JETTY_BASE"

# Specify the encoding to use
XWIKI_OPTS="$XWIKI_OPTS -Dfile.encoding=UTF8"

# Specify port on which HTTP requests will be handled
JETTY_OPTS="$JETTY_OPTS jetty.http.port=$JETTY_PORT"
# In order to print a nice friendly message to the user when Jetty has finished loading the XWiki webapp, we pass
# the port we use as a System Property
XWIKI_OPTS="$XWIKI_OPTS -Djetty.http.port=$JETTY_PORT"

# Specify port and key to stop a running Jetty instance
JETTY_OPTS="$JETTY_OPTS STOP.KEY=xwiki STOP.PORT=$JETTY_STOP_PORT"

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

# Check version of Java (when in non-interactive mode)
JAVA_VERSION="$(java_version)"

if [[ "$JAVA_VERSION" -eq "no_java" ]]; then
  echo "No Java found. You need Java installed for XWiki to work."
  exit 1
fi
if [ "$JAVA_VERSION" -lt 17 ]; then
  echo This version of XWiki requires Java 17 or greater.
  exit 1
fi
if [ "$JAVA_VERSION" -gt 17 ]; then
  if [ ! "$XWIKI_NONINTERACTIVE" = true ] ; then
    read -p "You're using Java $JAVA_VERSION which XWiki doesn't fully support yet. Continue (y/N)? " -n 1 -r
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      exit 1
    fi
  else
    echo "You're using Java $JAVA_VERSION which XWiki doesn't fully support yet. Exiting"
    exit 1
  fi
fi

# TODO: Remove once https://jira.xwiki.org/browse/XCOMMONS-2852 is fixed. In summary we need this to allow the XWiki
# code or 3rd party code to use reflection to access private variables (setAccessible() calls).
# See https://tinyurl.com/tdhkn6mp
if [ "$JAVA_VERSION" -gt 11 ]; then
  XWIKI_OPENS_LANG="--add-opens java.base/java.lang=ALL-UNNAMED"
  XWIKI_OPENS_IO="--add-opens java.base/java.io=ALL-UNNAMED"
  XWIKI_OPENS_UTIL="--add-opens java.base/java.util=ALL-UNNAMED"
  XWIKI_OPENS_CONCURRENT="--add-opens java.base/java.util.concurrent=ALL-UNNAMED"
  XWIKI_OPTS="$XWIKI_OPENS_LANG $XWIKI_OPENS_IO $XWIKI_OPENS_UTIL $XWIKI_OPENS_CONCURRENT $XWIKI_OPTS"
fi

# We save the shell PID here because we do an exec below and exec will replace the shell with the executed command
# and thus the java process PID will actually be the shell PID.
XWIKI_PID=$$
echo $XWIKI_PID > $XWIKI_LOCK_FILE

(
  # Wait till the java process doesn't exist anymore (which will happen if the user presses crtl-c or
  # if stop_xwiki.sh is called.
  while :; do
    # Break the loop when kill returns non 0 results, i.e. when the java process doesn't exist anymore
    kill -0 $XWIKI_PID 2>/dev/null || break
    sleep 1
  done
  # Remove XWiki lock file
  rm -f $XWIKI_LOCK_FILE
) &

# This replaces the shell with the java process without starting a new process. This must be the last line
# of this script as anything after won't be executed.
exec java $XWIKI_OPTS -jar ${JETTY_HOME}/start.jar $JETTY_OPTS
