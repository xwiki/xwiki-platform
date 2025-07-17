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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrablePage;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.administration.test.po.PresentationAdministrationSectionPage;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate the Presentation section of the Administration application.
 *
 * @version $Id$
 */
@UITest
class PresentationIT
{
    @AfterAll
    void afterAll(TestUtils setup)
    {
        // Reset the administration settings.
        setup.loginAsSuperAdmin();
        AdministrationSectionPage.gotoPage("Presentation");

        PresentationAdministrationSectionPage presentationSectionPage =
            new PresentationAdministrationSectionPage();
        presentationSectionPage.setShowAnnotations(PresentationAdministrationSectionPage.ShowTabValue.NO);
        presentationSectionPage.setShowComments(PresentationAdministrationSectionPage.ShowTabValue.DEFAULT);
        presentationSectionPage.setShowAttachments(PresentationAdministrationSectionPage.ShowTabValue.DEFAULT);
        presentationSectionPage.setShowHistory(PresentationAdministrationSectionPage.ShowTabValue.DEFAULT);
        presentationSectionPage.setShowInformation(PresentationAdministrationSectionPage.ShowTabValue.DEFAULT);
        presentationSectionPage.clickSave();
    }

    /**
     * Validate that the show information setting of the Presentation section of the administration has an effect.
     */
    @Test
    void showPageInformationTabSettings(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        ViewPage viewPage = setup.createPage(testReference, "");
        // Check that the information tab is displayed by default.
        assertTrue(viewPage.hasInformationDocExtraPane());
        AdministrablePage administrablePage = new AdministrablePage();

        // Navigate to the Presentation administration section.
        administrablePage.clickAdministerWiki().clickSection("Look & Feel", "Presentation");
        PresentationAdministrationSectionPage presentationSectionPage =
            new PresentationAdministrationSectionPage();
        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.DEFAULT,
            presentationSectionPage.getShowInformation());

        presentationSectionPage.setShowInformation(PresentationAdministrationSectionPage.ShowTabValue.NO);
        presentationSectionPage.clickSave();

        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.NO,
            presentationSectionPage.getShowInformation());

        // Check that the information tab is no longer displayed.
        viewPage = setup.gotoPage(testReference);
        assertFalse(viewPage.hasInformationDocExtraPane());
    }

    @Test
    void showPageAttachmentsTab(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        setup.createPage(testReference, "");
        AttachmentsViewPage viewPage = new AttachmentsViewPage();
        // Check that the attachments tab is available by default.
        assertTrue(viewPage.isAttachmentsDocExtraPaneAvailable());

        AdministrablePage administrablePage = new AdministrablePage();
        administrablePage.clickAdministerWiki().clickSection("Look & Feel", "Presentation");
        PresentationAdministrationSectionPage presentationSectionPage =
            new PresentationAdministrationSectionPage();
        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.DEFAULT,
            presentationSectionPage.getShowAttachments());

        presentationSectionPage.setShowAttachments(PresentationAdministrationSectionPage.ShowTabValue.NO);
        presentationSectionPage.clickSave();

        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.NO,
            presentationSectionPage.getShowAttachments());

        // Check that the attachments tab is no longer available.
        setup.gotoPage(testReference);
        viewPage = new AttachmentsViewPage();
        assertFalse(viewPage.isAttachmentsDocExtraPaneAvailable());
    }
}
