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
public class Upgrade1410Test extends UpgradeTest
{
    @Override
    protected void setupLogs()
    {
        validateConsole.getLogCaptureConfiguration().registerExpected(
            // The PDF export extensions installed by the previous version depend on the ScriptSafeProvider class
            // which is not internal anymore since 16.2, so they cannot be initialized until the flavor is upgraded
            // (see https://jira.xwiki.org/browse/XWIKI-22043). The error about xwiki-platform-export-pdf-default is
            // already globally excluded, but not the one about the UI extension depending on it.
            "Failed to initialize local extension [org.xwiki.platform:xwiki-platform-export-pdf-ui/"
        );
    }
}
