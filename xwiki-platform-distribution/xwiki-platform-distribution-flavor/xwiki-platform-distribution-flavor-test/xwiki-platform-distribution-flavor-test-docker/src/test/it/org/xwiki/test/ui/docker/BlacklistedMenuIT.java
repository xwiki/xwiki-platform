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
package org.xwiki.test.ui.docker;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.application.test.po.ApplicationIndexHomePage;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify that the standard flavor hides the Menu application from the Applications Panel while keeping it in the
 * Application Index. The Applications Panel blacklist is shipped by the flavor (the
 * {@code PanelsCode.ApplicationsPanelConfiguration} page of {@code xwiki-platform-distribution-flavor-common}), so it
 * can't be verified by the tests of the Menu or Panels modules.
 *
 * @version $Id$
 */
@UITest(standardFlavor = true)
class BlacklistedMenuIT
{
    @Test
    @Order(1)
    void menuIsBlacklistedFromApplicationsPanel(TestUtils setup)
    {
        // The blacklist applies to all users, superadmin included.
        setup.loginAsSuperAdmin();

        assertFalse(ApplicationsPanel.gotoPage().containsApplication("Menu"));

        // The Menu application is still listed in the Application Index.
        assertTrue(ApplicationIndexHomePage.gotoPage().containsApplication("Menu"));
    }
}
