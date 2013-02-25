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

# If no XWIKI_OPTS env variable has been defined use default values.
if [ -z "$XWIKI_OPTS" ] ; then
  XWIKI_OPTS="-Xmx512m -XX:MaxPermSize=196m"
fi

# The port on which to start Jetty can be passed to this script as the first argument
if [ -n "$1" ]; then
  JETTY_PORT=$1
else
  JETTY_PORT=8080
fi

# The port on which to stop Jetty can be passed to this script as the second argument
if [ -n "$2" ]; then
  JETTY_STOPPORT=$2
else
  JETTY_STOPPORT=8079
fi

echo Starting Jetty on port $JETTY_PORT ...

# Location where XWiki stores generated data and where database files are.
XWIKI_DATA_DIR=${xwikiDataDir}
XWIKI_OPTS="$XWIKI_OPTS -Dxwiki.data.dir=$XWIKI_DATA_DIR"

# Ensure the data directory exists so that XWiki can use it for storing permanent data.
mkdir -p $XWIKI_DATA_DIR 2>/dev/null

# Ensure the logs directory exists as otherwise Jetty reports an error
mkdir -p $XWIKI_DATA_DIR/logs 2>/dev/null

# Specify port on which HTTP requests will be handled
XWIKI_OPTS="$XWIKI_OPTS -Djetty.port=$JETTY_PORT"

# Specify Jetty's home directory
XWIKI_OPTS="$XWIKI_OPTS -Djetty.home=$JETTY_HOME"

# Specify port and key to stop a running Jetty instance
XWIKI_OPTS="$XWIKI_OPTS -DSTOP.KEY=xwiki -DSTOP.PORT=$JETTY_STOPPORT"

# Specify the encoding to use
XWIKI_OPTS="$XWIKI_OPTS -Dfile.encoding=UTF8"

# In order to avoid getting a "java.lang.IllegalStateException: Form too large" error
# when editing large page in XWiki we need to tell Jetty to allow for large content
# since by default it only allows for 20K. We do this by passing the
# org.eclipse.jetty.server.Request.maxFormContentSize property.
# Note that setting this value too high can leave your server vulnerable to denial of
# service attacks.
XWIKI_OPTS="$XWIKI_OPTS -Dorg.eclipse.jetty.server.Request.maxFormContentSize=1000000"

# Create a lock file to signify that XWiki is running
touch xwiki.lck

java $XWIKI_OPTS $3 $4 $5 $6 $7 $8 $9 -jar $JETTY_HOME/start.jar

# Remove XWiki lock file
rm -f xwiki.lck
