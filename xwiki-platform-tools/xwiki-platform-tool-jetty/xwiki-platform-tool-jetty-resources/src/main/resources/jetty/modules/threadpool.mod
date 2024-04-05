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
Enables and configures the Server ThreadPool.

[depends]
logging

[provides]
threadpool|default

[xml]
etc/jetty-threadpool.xml

[ini-template]
# tag::documentation[]
## Thread name prefix.
#jetty.threadPool.namePrefix=qtp<hashCode>

## Minimum number of pooled threads.
#jetty.threadPool.minThreads=10

## Maximum number of pooled threads.
#jetty.threadPool.maxThreads=200

## Number of reserved threads (-1 for heuristic).
#jetty.threadPool.reservedThreads=-1

## Whether to use virtual threads, if the runtime supports them.
## Deprecated, use Jetty module 'threadpool-virtual' instead.
#jetty.threadPool.useVirtualThreads=false

## Thread idle timeout (in milliseconds).
#jetty.threadPool.idleTimeout=60000

## The max number of idle threads that are evicted in one idleTimeout period.
#jetty.threadPool.maxEvictCount=1

## Whether to output a detailed dump.
#jetty.threadPool.detailedDump=false
# end::documentation[]
