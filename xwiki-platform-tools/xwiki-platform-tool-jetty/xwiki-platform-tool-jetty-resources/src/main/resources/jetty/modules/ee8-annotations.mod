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
Enables Annotation scanning for deployed web applications.

[environment]
ee8

[depend]
plus
ee8-plus

[ini]
ee8.asm.version?=9.7
ee8.jakarta.annotation.api.version?=1.3.5

[lib]
lib/jetty-ee8-annotations-${jetty.version}.jar
lib/ee8-annotations/asm-${ee8.asm.version}.jar
lib/ee8-annotations/asm-analysis-${ee8.asm.version}.jar
lib/ee8-annotations/asm-commons-${ee8.asm.version}.jar
lib/ee8-annotations/asm-tree-${ee8.asm.version}.jar
lib/ee8-annotations/jakarta.annotation-api-${ee8.jakarta.annotation.api.version}.jar

[jpms]
add-modules:org.objectweb.asm

