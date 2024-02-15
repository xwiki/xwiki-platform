/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.test.ui;

/**
 * Execute upgrade tests.
 * 
 * @version $Id$
 */
public class Upgrade1210Test extends UpgradeTest
{
    @Override
    protected void setupLogs()
    {
        validateConsole.getLogCaptureConfiguration().registerExpected(
            // Caused by the fact that we upgrade from an old version of XWiki having these deprecated uses
            "Deprecated usage of getter [com.xpn.xwiki.api.Document.getName]",

            // Those deprecated are related to the Velocity upgrade performed in 12.0 (XCOMMONS-1529)
            "Deprecated usage of method [org.apache.velocity.tools.generic.SortTool.sort]",
            "Deprecated usage of getter [org.xwiki.velocity.tools.CollectionsTool.getSet]",
            "Deprecated usage of method [org.apache.velocity.tools.generic.MathTool.toInteger]",
            "Deprecated usage of method [org.xwiki.velocity.tools.CollectionsTool.sort]",

            // The currently installed flavor is not valid anymore before the upgrade
            "Invalid extension [org.xwiki.platform:xwiki-platform-distribution-flavor-mainwiki/10.11.1] on namespace "
                + "[wiki:xwiki] (InvalidExtensionException: Dependency [org.xwiki.platform:xwiki-platform-oldcore-"
                + "[10.11.1]] is incompatible with the core extension [org.xwiki.platform:xwiki-platform-legacy-oldcore");
    }
}
