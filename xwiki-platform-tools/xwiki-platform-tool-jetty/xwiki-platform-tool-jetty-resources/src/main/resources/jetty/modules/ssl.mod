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
# SSL Keystore module
#

[depend]
server

[xml]
etc/jetty-ssl.xml

[files]
http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/plain/jetty-server/src/main/config/etc/keystore|etc/keystore

[ini-template]
### SSL Keystore Configuration
# define the port to use for secure redirection
jetty.secure.port=8443

## Setup a demonstration keystore and truststore
jetty.keystore=etc/keystore
jetty.truststore=etc/keystore

## Set the demonstration passwords.
## Note that OBF passwords are not secure, just protected from casual observation
## See http://www.eclipse.org/jetty/documentation/current/configuring-security-secure-passwords.html
jetty.keystore.password=OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4
jetty.keymanager.password=OBF:1u2u1wml1z7s1z7a1wnl1u2g
jetty.truststore.password=OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4

### Set the client auth behavior
## Set to true if client certificate authentication is required
# jetty.ssl.needClientAuth=true
## Set to true if client certificate authentication is desired
# jetty.ssl.wantClientAuth=true

## Parameters to control the number and priority of acceptors and selectors
# ssl.selectors=1
# ssl.acceptors=1
# ssl.selectorPriorityDelta=0
# ssl.acceptorPriorityDelta=0
