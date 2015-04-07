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
# Jetty HTTP Connector
#

[depend]
server

[xml]
etc/jetty-http.xml

[ini-template]
### HTTP Connector Configuration

## HTTP port to listen on
jetty.port=8080

## HTTP idle timeout in milliseconds
http.timeout=30000

## HTTP Socket.soLingerTime in seconds. (-1 to disable)
# http.soLingerTime=-1

## Parameters to control the number and priority of acceptors and selectors
# http.selectors=1
# http.acceptors=1
# http.selectorPriorityDelta=0
# http.acceptorPriorityDelta=0
