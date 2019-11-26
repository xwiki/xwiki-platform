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

DO NOT EDIT - See: https://www.eclipse.org/jetty/documentation/current/startup-modules.html

[description]
Enables a NCSA style request log.

[tags]
requestlog

[depend]
server

[xml]
etc/jetty-requestlog.xml

[files]
logs/

[ini-template]
## Logging directory (relative to $jetty.base)
# jetty.requestlog.dir=logs

## File path
# jetty.requestlog.filePath=${jetty.requestlog.dir}/yyyy_mm_dd.request.log

## Date format for rollovered files (uses SimpleDateFormat syntax)
# jetty.requestlog.filenameDateFormat=yyyy_MM_dd

## How many days to retain old log files
# jetty.requestlog.retainDays=90

## Whether to append to existing file
# jetty.requestlog.append=false

## Whether to use the extended log output
# jetty.requestlog.extended=true

## Whether to log http cookie information
# jetty.requestlog.cookies=true

## Timezone of the log entries
# jetty.requestlog.timezone=GMT

## Whether to log LogLatency
# jetty.requestlog.loglatency=false
