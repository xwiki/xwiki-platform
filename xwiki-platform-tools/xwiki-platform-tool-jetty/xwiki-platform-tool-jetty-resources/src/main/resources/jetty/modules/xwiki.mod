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

# This is the top level mod used when starting Jetty in start_xwiki.*
# It's called with "--module=xwiki" on the command line to start Jetty.

[environment]
ee8

[depend]
ext
resources
server
logging
http
http-forwarded
ee8-annotations
ee8-deploy
requestlog
ee8-websocket-javax
ee8-websocket-jetty
ee8-apache-jsp
console-capture

[xml]
etc/jetty-xwiki.xml
