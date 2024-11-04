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

# DO NOT EDIT - See: https://jetty.org/docs/index.html

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

[ini]
jetty.requestlog.dir?=logs

[ini-template]
# tag::documentation[]
## Format string
# jetty.requestlog.formatString=%{client}a - %u %{dd/MMM/yyyy:HH:mm:ss ZZZ|GMT}t "%r" %s %O "%{Referer}i" "%{User-Agent}i"

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

## Timezone of the log file rollover
# jetty.requestlog.timezone=GMT
# end::documentation[]
