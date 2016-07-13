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
# Base Server Module
#

[optional]
jvm
ext
resources

[lib]
lib/javax.servlet-api-3.1.0.jar
lib/jetty-schemas-3.1.jar
lib/jetty-http-${jetty.version}.jar
lib/jetty-server-${jetty.version}.jar
lib/jetty-xml-${jetty.version}.jar
lib/jetty-util-${jetty.version}.jar
lib/jetty-io-${jetty.version}.jar

[xml]
etc/jetty.xml

[ini-template]
##
## Server Threading Configuration
##
# minimum number of threads
threads.min=10
# maximum number of threads
threads.max=200
# thread idle timeout in milliseconds
threads.timeout=60000
# buffer size for output
jetty.output.buffer.size=32768
# request header buffer size
jetty.request.header.size=8192
# response header buffer size
jetty.response.header.size=8192
# should jetty send the server version header?
jetty.send.server.version=true
# should jetty send the date header?
jetty.send.date.header=false
# What host to listen on (leave commented to listen on all interfaces)
#jetty.host=myhost.com
# Dump the state of the Jetty server, components, and webapps after startup
jetty.dump.start=false
# Dump the state of the Jetty server, before stop
jetty.dump.stop=false


