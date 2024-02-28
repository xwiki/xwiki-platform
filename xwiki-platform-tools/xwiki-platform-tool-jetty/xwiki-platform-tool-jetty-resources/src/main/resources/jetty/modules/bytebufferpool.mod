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
Configures the ByteBufferPool used by ServerConnectors.
Use module "bytebufferpool-logarithmic" for a pool may hold less granulated sized buffers.

[tags]
bytebufferpool

[xml]
etc/jetty-bytebufferpool.xml

[ini-template]
## Minimum capacity of a single ByteBuffer.
#jetty.byteBufferPool.minCapacity=0

## Maximum capacity of a single ByteBuffer.
## Requests for ByteBuffers larger than this value results
## in the ByteBuffer being allocated but not pooled.
#jetty.byteBufferPool.maxCapacity=65536

## Bucket capacity factor.
## ByteBuffers are allocated out of buckets that have
## a capacity that is multiple of this factor.
#jetty.byteBufferPool.factor=4096

## Maximum size for each bucket (-1 for unbounded).
#jetty.byteBufferPool.maxBucketSize=-1

## Maximum heap memory held idle by the pool (0 for heuristic, -1 for unlimited).
#jetty.byteBufferPool.maxHeapMemory=0

## Maximum direct memory held idle by the pool (0 for heuristic, -1 for unlimited).
#jetty.byteBufferPool.maxDirectMemory=0

## Maximum heap memory retained whilst in use by the pool (0 for heuristic, -1 for unlimited, -2 for no retained).
#jetty.byteBufferPool.retainedHeapMemory=0

## Maximum direct memory retained whilst in use by the pool (0 for heuristic, -1 for unlimited, -2 for no retained).
#jetty.byteBufferPool.retainedDirectMemory=0
