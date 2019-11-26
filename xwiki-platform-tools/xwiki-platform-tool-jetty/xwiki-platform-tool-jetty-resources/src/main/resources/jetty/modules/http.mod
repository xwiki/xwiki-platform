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
Enables a HTTP connector on the server.
By default HTTP/1 is support, but HTTP2C can
be added to the connector with the http2c module.

[tags]
connector
http

[depend]
server

[xml]
etc/jetty-http.xml

[ini-template]
### HTTP Connector Configuration

## Connector host/address to bind to
# jetty.http.host=0.0.0.0

## Connector port to listen on
# jetty.http.port=8080

## Connector idle timeout in milliseconds
# jetty.http.idleTimeout=30000

## Number of acceptors (-1 picks default based on number of cores)
# jetty.http.acceptors=-1

## Number of selectors (-1 picks default based on number of cores)
# jetty.http.selectors=-1

## ServerSocketChannel backlog (0 picks platform default)
# jetty.http.acceptorQueueSize=0

## Thread priority delta to give to acceptor threads
# jetty.http.acceptorPriorityDelta=0

## Connect Timeout in milliseconds
# jetty.http.connectTimeout=15000

## HTTP Compliance: RFC7230, RFC7230_LEGACY, RFC2616, RFC2616_LEGACY, LEGACY or CUSTOMn
# jetty.http.compliance=RFC7230_LEGACY
