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

# DO NOT EDIT - See: https://eclipse.dev/jetty/documentation/

[description]
Logs requests using CustomRequestLog and AsyncRequestLogWriter.

[tags]
requestlog
logging

[depend]
server

[xml]
etc/jetty-requestlog.xml

[files]
logs/

[ini-template]
# tag::documentation[]
## Request log line format string.
#jetty.requestlog.formatString=%{client}a - %u %{dd/MMM/yyyy:HH:mm:ss ZZZ|GMT}t "%r" %s %O "%{Referer}i" "%{User-Agent}i"

## The logging directory (relative to $JETTY_BASE).
# jetty.requestlog.dir=logs

## The request log file path (may be absolute and/or outside $JETTY_BASE).
# jetty.requestlog.filePath=${jetty.requestlog.dir}/yyyy_MM_dd.request.log

## Date format for the files that are rolled over (uses SimpleDateFormat syntax).
# jetty.requestlog.filenameDateFormat=yyyy_MM_dd

## How many days to retain old log files.
# jetty.requestlog.retainDays=90

## Whether to append to existing file or create a new one.
# jetty.requestlog.append=false

## The timezone of the log file name.
# jetty.requestlog.timezone=GMT
# end::documentation[]
