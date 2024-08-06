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

# DO NOT EDIT - See: https://jetty.org/docs/index.html

[description]
Adds support for servlet specification web applications to the server classpath.
Without this, only Jetty-specific handlers may be deployed.

[environment]
ee8

[depend]
ee-webapp
ee8-servlet
ee8-security

[xml]
etc/jetty-ee8-webapp.xml

[lib]
lib/jetty-ee8-webapp-${jetty.version}.jar

[ini-template]
## Add to the environment wide default jars and packages protected or hidden from webapps.
## System (aka Protected) classes cannot be overridden by a webapp.
## Server (aka Hidden) classes cannot be seen by a webapp
## Lists of patterns are comma separated and may be either:
##  + a qualified classname e.g. 'com.acme.Foo' 
##  + a package name e.g. 'net.example.'
##  + a jar file e.g. '${jetty.base.uri}/lib/dependency.jar' 
##  + a directory of jars,resource or classes e.g. '${jetty.base.uri}/resources' 
##  + A pattern preceded with a '-' is an exclusion, all other patterns are inclusions
##
## The +=, operator appends to a CSV list with a comma as needed.
##
#jetty.webapp.addProtectedClasses+=,org.example.
#jetty.webapp.addHiddenClasses+=,org.example.

[ini]
contextHandlerClass=org.eclipse.jetty.ee8.webapp.WebAppContext

[jpms]
add-modules:java.instrument
