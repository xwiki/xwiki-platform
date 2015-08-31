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
package org.xwiki.panels.test.ui;

import org.junit.*;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

/**
 * Tests related to the Panel Wizard.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class PanelWizardTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void verifyPanelWizardPresentInAdministration()
    {
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        Assert.assertTrue(administrationPage.hasSection("Panels.PanelWizard"));

        // Select space administration (XWiki space, since that space exists)
        AdministrationPage spaceAdministrationPage = AdministrationPage.gotoSpaceAdministrationPage("XWiki");

        // The Panel Wizard should be available in the space administration.
        Assert.assertTrue(spaceAdministrationPage.hasSection("Panels.PanelWizard"));
    }
}
