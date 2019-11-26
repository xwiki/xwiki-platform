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
Enables the Server thread pool.

[xml]
etc/jetty-threadpool.xml

[ini-template]

### Server Thread Pool Configuration
## Minimum Number of Threads
#jetty.threadPool.minThreads=10

## Maximum Number of Threads
#jetty.threadPool.maxThreads=200

## Number of reserved threads (-1 for heuristic)
# jetty.threadPool.reservedThreads=-1

## Thread Idle Timeout (in milliseconds)
#jetty.threadPool.idleTimeout=60000

## Whether to Output a Detailed Dump
#jetty.threadPool.detailedDump=false
