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
Redirects the JVM console stderr and stdout to a rolling log file.

[tags]
logging

[depends]
logging

[xml]
etc/console-capture.xml

[files]
logs/

[ini-template]
# tag::documentation[]
## Logging directory (relative to $JETTY_BASE).
# jetty.console-capture.dir=./logs

## Whether to append to existing file.
# jetty.console-capture.append=true

## How many days to retain old log files.
# jetty.console-capture.retainDays=90

## Timezone ID of the log timestamps, as specified by java.time.ZoneId.
# jetty.console-capture.timezone=GMT
# end::documentation[]
