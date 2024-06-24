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
Enables web application deployment from the $JETTY_BASE/webapps/ directory.

[depend]
webapp

[lib]
lib/jetty-deploy-${jetty.version}.jar

[files]
webapps/

[xml]
etc/jetty-deploy.xml

[ini-template]
# Monitored directory name (relative to $jetty.base)
# jetty.deploy.monitoredDir=webapps

# Defaults Descriptor for all deployed webapps
# jetty.deploy.defaultsDescriptorPath=${jetty.base}/etc/webdefault.xml

# Defer Initial Scan
# true to have the initial scan deferred until the Server component is started.
#      Note: deploy failures do not fail server startup in a deferred initial scan mode.
# false (default) to have initial scan occur as normal.
# jetty.deploy.deferInitialScan=false

# Monitored directory scan period (seconds)
# jetty.deploy.scanInterval=1

# Whether to extract *.war files
# jetty.deploy.extractWars=true
