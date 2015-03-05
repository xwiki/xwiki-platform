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
package org.xwiki.messagestream.test.ui;

import org.junit.*;
import org.openqa.selenium.By;
import org.xwiki.administration.test.po.AdministrablePage;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

/**
 * Verify the overall Administration application features.
 *
 * @version $Id$
 * @since 4.3M1
 */
public class AdministrationTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void verifyGlobalAndSpaceSections()
    {
        // Because of http://jira.xwiki.org/browse/XWIKI-9763 we need to create a test page to ensure there's at least
        // one non-hidden page in the XWiki space
        // TODO: Remove this once http://jira.xwiki.org/browse/XWIKI-9763 is fixed.
        getUtil().createPage("XWiki", getTestClassName() + "-" + getTestMethodName(), "", "");

        AdministrablePage page = new AdministrablePage();
        AdministrationPage administrationPage = page.clickAdministerWiki();

        Assert.assertTrue(administrationPage.hasSection("MessageStream"));

        // Select XWiki space administration.
        AdministrationPage spaceAdministrationPage = administrationPage.selectSpaceToAdminister("XWiki");

        // Since clicking on "XWiki" in the Select box will reload the page asynchronously we need to wait for the new
        // page to be available. For this we wait for the heading to be changed to "Administration:XWiki".
        getDriver().waitUntilElementIsVisible(By.id("HAdministration:XWiki"));
        // Also wait till the page is fully loaded to be extra sure...
        spaceAdministrationPage.waitUntilPageIsLoaded();

        // All those sections should not be present
        Assert.assertTrue(spaceAdministrationPage.hasNotSection("MessageStream"));
    }
}
