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
package org.xwiki.administration.test.ui;

import java.util.Arrays;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.administration.test.po.AdministrablePage;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

import static org.junit.Assert.*;

/**
 * Verify the overall Administration application features.
 *
 * @version $Id$
 * @since 4.3M1
 */
public class AdministrationIT extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    /**
     * This method makes the following tests :
     *
     * <ul>
     * <li>Validate presence of default sections for global and space sections.</li>
     * <li>Validate presence of application administration sections at global level only.</li>
     * </ul>
     */
    @Test
    public void verifyGlobalAndSpaceSections()
    {
        // Navigate to a (non existent for test performance reasons) page in view mode.
        getUtil().gotoPage("NonExistentSpace", "NonExistentPage");

        // Verify that pages have an Admin menu and navigate to the admin UI.
        AdministrablePage page = new AdministrablePage();
        AdministrationPage administrationPage = page.clickAdministerWiki();

        assertEquals("Global Administration: Home", administrationPage.getDocumentTitle());
        assertTrue(administrationPage.getBreadcrumbContent().endsWith("/Global Administration"));

        // TODO: Move these tests in their own modules, i.e. the modules that brought the Administration UI extension.
        Arrays.asList("Users", "Groups", "Rights", "Registration", "Themes", "Presentation", "Templates",
            "Localization", "Import", "Export", "Editing", "emailSend", "emailStatus", "emailGeneral", "analytics")
            .stream().forEach(new Consumer<String>()
            {
                @Override
                public void accept(String sectionId)
                {
                    assertTrue(String.format("Seection %s is missing.", sectionId),
                        administrationPage.hasSection(sectionId));
                }
            });

        // These are page-only sections.
        assertTrue(administrationPage.hasNotSection("PageAndChildrenRights"));
        assertTrue(administrationPage.hasNotSection("PageRights"));

        // Select XWiki space administration.
        AdministrationPage spaceAdministrationPage = AdministrationPage.gotoSpaceAdministrationPage("XWiki");

        assertEquals("Page Administration: XWiki", spaceAdministrationPage.getDocumentTitle());
        assertTrue(spaceAdministrationPage.getBreadcrumbContent().endsWith("/Page Administration"));

        assertTrue(spaceAdministrationPage.hasSection("Themes"));
        assertTrue(spaceAdministrationPage.hasSection("Presentation"));
        assertTrue(spaceAdministrationPage.hasSection("PageAndChildrenRights"));
        assertTrue(spaceAdministrationPage.hasSection("PageRights"));

        // All these sections should not be present (they provide global configuration).
        Arrays.asList("Users", "Groups", "Rights", "Registration", "Templates", "Localization", "Import", "Export",
                "Editing", "emailSend", "emailStatus", "emailGeneral", "analytics")
            .stream().forEach(new Consumer<String>()
            {
                @Override
                public void accept(String sectionId)
                {
                    assertTrue(String.format("Seection %s is present.", sectionId),
                        administrationPage.hasNotSection(sectionId));
                }
            });
    }
}
