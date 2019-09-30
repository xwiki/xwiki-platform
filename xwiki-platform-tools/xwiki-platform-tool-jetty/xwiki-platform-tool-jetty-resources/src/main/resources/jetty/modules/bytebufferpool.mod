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
Configures the ByteBufferPool used by ServerConnectors.

[xml]
etc/jetty-bytebufferpool.xml

[ini-template]
### Server ByteBufferPool Configuration
## Minimum capacity to pool ByteBuffers
#jetty.byteBufferPool.minCapacity=0

## Maximum capacity to pool ByteBuffers
#jetty.byteBufferPool.maxCapacity=65536

## Capacity factor
#jetty.byteBufferPool.factor=1024

## Maximum queue length for each bucket (-1 for unbounded)
#jetty.byteBufferPool.maxQueueLength=-1

## Maximum heap memory retainable by the pool (-1 for unlimited)
#jetty.byteBufferPool.maxHeapMemory=-1

## Maximum direct memory retainable by the pool (-1 for unlimited)
#jetty.byteBufferPool.maxDirectMemory=-1
