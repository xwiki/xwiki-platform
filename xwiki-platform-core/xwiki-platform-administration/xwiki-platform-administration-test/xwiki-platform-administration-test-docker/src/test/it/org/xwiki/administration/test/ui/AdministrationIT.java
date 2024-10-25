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

import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrablePage;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify the overall Administration application features.
 *
 * @version $Id$
 * @since 4.3M1
 */
@UITest
class AdministrationIT
{
    /**
     * Validate presence of default sections for Administration UIs (Global, Page).
     */
    @Test
    void verifyAdministrationSections(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();

        // Navigate to a page in view mode.
        setup.createPage(testReference, "");

        // Verify that pages have an Admin menu and navigate to the wiki admin UI (which happens to be the global
        // admin UI too since we're on the main wiki).
        // Note that this test is currently lacking checking the wiki administration UI when the wiki is not the main
        // wiki.
        AdministrablePage page = new AdministrablePage();
        AdministrationPage wikiAdministrationPage = page.clickAdministerWiki();

        assertEquals("Global Administration: Home", wikiAdministrationPage.getDocumentTitle());
        assertTrue(wikiAdministrationPage.getBreadcrumbContent().endsWith("/Global Administration"));

        // TODO: Move these tests in their own modules, i.e. the modules that brought the Administration UI extension.
        Arrays.asList("Users", "Groups", "Rights", "Registration", "Themes", "Presentation", "Templates",
            "Localization", "Import", "Export", "Editing", "emailSend", "emailStatus", "emailGeneral")
            .stream().forEach(sectionId -> assertTrue(wikiAdministrationPage.hasSection(sectionId),
                String.format("Menu section [%s] is missing.", sectionId)));

        // These are page-only sections.
        assertTrue(wikiAdministrationPage.hasNotSection("PageAndChildrenRights"));
        assertTrue(wikiAdministrationPage.hasNotSection("PageRights"));

        // Select XWiki page administration.
        setup.gotoPage(testReference);
        page = new AdministrablePage();
        AdministrationPage pageAdministrationPage = page.clickAdministerPage();
        String fullName = setup.serializeReference(testReference.getParent()).split(":")[1];
        assertEquals("Page Administration: " + fullName, 
            pageAdministrationPage.getDocumentTitle());
        assertTrue(pageAdministrationPage.getBreadcrumbContent().endsWith("/Page Administration"));

        assertTrue(pageAdministrationPage.hasSection("Themes"));
        assertTrue(pageAdministrationPage.hasSection("Presentation"));
        assertTrue(pageAdministrationPage.hasSection("PageAndChildrenRights"));
        assertTrue(pageAdministrationPage.hasSection("PageRights"));

        // All these sections should not be present (they provide wiki-wide configuration).
        Arrays.asList("Users", "Groups", "Rights", "Registration", "Templates", "Localization", "Import", "Export",
            "Editing", "emailSend", "emailStatus", "emailGeneral")
            .stream().forEach(sectionId -> assertTrue(pageAdministrationPage.hasNotSection(sectionId),
                String.format("Menu section [%s] shouldn't be present.", sectionId)));
    }
}
