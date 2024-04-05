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
Enable Java WebSocket APIs for deployed web applications.

[tags]
websocket

[depend]
client
annotations

[lib]
lib/websocket/websocket-core-common-${jetty.version}.jar
lib/websocket/websocket-core-client-${jetty.version}.jar
lib/websocket/websocket-core-server-${jetty.version}.jar
lib/websocket/websocket-servlet-${jetty.version}.jar
lib/websocket/jetty-javax-websocket-api-1.1.2.jar
lib/websocket/websocket-javax-client-${jetty.version}.jar
lib/websocket/websocket-javax-common-${jetty.version}.jar
lib/websocket/websocket-javax-server-${jetty.version}.jar

[jpms]
# The implementation needs to access method handles in
# classes that are in the web application classloader.
add-reads: org.eclipse.jetty.websocket.javax.common=ALL-UNNAMED
