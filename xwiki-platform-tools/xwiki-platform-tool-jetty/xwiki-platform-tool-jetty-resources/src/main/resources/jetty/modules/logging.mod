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
# Jetty std err/out logging
#

[depend]
server

[xml]
etc/jetty-logging.xml

[files]
logs/

[lib]
lib/logging/**.jar
resources/

[ini-template]
## Logging Configuration
# Configure jetty logging for default internal behavior STDERR output
# -Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.StdErrLog

# Configure jetty logging for slf4j
# -Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.Slf4jLog

# Configure jetty logging for java.util.logging
# -Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.JavaUtilLog

# STDERR / STDOUT Logging
# Number of days to retain logs
# jetty.log.retain=90
# Directory for logging output
# Either a path relative to ${jetty.base} or an absolute path
# jetty.logs=logs
