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

#
# Request Log module
#

[depend]
server

[xml]
etc/jetty-requestlog.xml

[files]
logs/

[ini-template]
## Request Log Configuration
# Filename for Request Log output (relative to jetty.base)
# requestlog.filename=/logs/yyyy_mm_dd.request.log
# Date format for rollovered files (uses SimpleDateFormat syntax)
# requestlog.filenameDateFormat=yyyy_MM_dd
# How many days to retain the logs
# requestlog.retain=90
# If an existing log with the same name is found, just append to it
# requestlog.append=true
# Use the extended log output
# requestlog.extended=true
# Log http cookie information as well
# requestlog.cookies=true
# Set the log output timezone
# requestlog.timezone=GMT

