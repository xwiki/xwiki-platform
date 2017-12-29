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

# We're making the following changes to the default:
# * Adding more memory (default is 512MB which is not enough for XWiki)
# * By default, Tomcat does not allow the usage of encoded slash '%2F' and backslash '%5C' in URLs, as noted in
#   http://tomcat.apache.org/security-6.html#Fixed_in_Apache_Tomcat_6.0.10. We want to allow for them as it's useful to
#   be able to have '/' and '\' in wiki page names.
# * On some system /dev/random is slow to init leading to a slow Tomcat and thus Xwiki startup.
export CATALINA_OPTS="-Xmx1024m -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true -Dorg.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true -Dsecurerandom.source=file:/dev/urandom"
