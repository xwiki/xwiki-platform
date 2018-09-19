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
package org.xwiki.test.ui.menu;

import org.junit.Test;
import org.xwiki.application.test.po.ApplicationIndexHomePage;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.ui.AbstractTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Add specific test for menu related to this flavor.
 *
 * @version $Id$
 * @since 10.8
 */
public class BlacklistedMenuTest extends AbstractTest
{
    @Test
    public void verifyMenuIsBlacklisted()
    {
        // Log in as admin
        getUtil().loginAsAdmin();

        // Verify that the menu app is displayed in the Applications Panel
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();

        // By default the Menu app is blacklisted from the application panel, even for superadmin user
        assertFalse(applicationPanel.containsApplication("Menu"));

        // We check that the Menu App is still available in the application index
        ApplicationIndexHomePage applicationIndexHomePage = ApplicationIndexHomePage.gotoPage();
        assertTrue(applicationIndexHomePage.containsApplication("Menu"));
    }
}
