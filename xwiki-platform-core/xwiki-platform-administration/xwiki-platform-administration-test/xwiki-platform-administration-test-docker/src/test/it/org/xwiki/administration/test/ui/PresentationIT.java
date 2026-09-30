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

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrablePage;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.administration.test.po.PresentationAdministrationSectionPage;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.TestUtils.RestTestUtils;
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
    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

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
        presentationSectionPage.setShowDocumentTabs(PresentationAdministrationSectionPage.ShowTabValue.DEFAULT);
        presentationSectionPage.setCopyright("");
        presentationSectionPage.setVersion("");
        presentationSectionPage.clickSave();
    }

    /**
     * Validate that the show information setting of the Presentation section of the administration has an effect.
     */
    @Test
    @Order(1)
    void showPageInformationTabSettings(TestUtils setup, TestReference testReference)
    {
        ViewPage viewPage = setup.createPage(testReference, "");
        // Check that the information tab is displayed by default.
        assertTrue(viewPage.hasInformationDocExtraPane());
        PresentationAdministrationSectionPage presentationSectionPage = gotoPresentationAdministration();
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
    @Order(2)
    void showPageAttachmentsTab(TestUtils setup, TestReference testReference)
    {
        setup.createPage(testReference, "");
        AttachmentsViewPage viewPage = new AttachmentsViewPage();
        // Check that the attachments tab is available by default.
        assertTrue(viewPage.isAttachmentsDocExtraPaneAvailable());

        PresentationAdministrationSectionPage presentationSectionPage = gotoPresentationAdministration();
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

    @Test
    @Order(3)
    void showPageCommentsTab(TestUtils setup, TestReference testReference)
    {
        ViewPage viewPage = setup.createPage(testReference, "");
        // Check that the comments tab is displayed by default.
        assertTrue(viewPage.hasCommentsDocExtraPane());
        PresentationAdministrationSectionPage presentationSectionPage = gotoPresentationAdministration();
        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.DEFAULT,
            presentationSectionPage.getShowComments());

        presentationSectionPage.setShowComments(PresentationAdministrationSectionPage.ShowTabValue.NO);
        presentationSectionPage.clickSave();

        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.NO,
            presentationSectionPage.getShowComments());

        // Check that the comments tab is no longer displayed.
        viewPage = setup.gotoPage(testReference);
        assertFalse(viewPage.hasCommentsDocExtraPane());
    }

    @Test
    @Order(4)
    void showPageHistoryTab(TestUtils setup, TestReference testReference)
    {
        ViewPage viewPage = setup.createPage(testReference, "");
        // Check that the history tab is displayed by default.
        assertTrue(viewPage.hasHistoryDocExtraPane());
        PresentationAdministrationSectionPage presentationSectionPage = gotoPresentationAdministration();
        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.DEFAULT,
            presentationSectionPage.getShowHistory());

        presentationSectionPage.setShowHistory(PresentationAdministrationSectionPage.ShowTabValue.NO);
        presentationSectionPage.clickSave();

        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.NO,
            presentationSectionPage.getShowHistory());

        // Check that the history tab is no longer displayed.
        viewPage = setup.gotoPage(testReference);
        assertFalse(viewPage.hasHistoryDocExtraPane());
    }

    @Test
    @Order(5)
    void customizeCopyright(TestUtils setup, TestReference testReference)
    {
        PresentationAdministrationSectionPage presentationSectionPage = gotoPresentationAdministration();
        // Check that there is no copyright in the footer by default
        assertTrue(presentationSectionPage.getFooterCopyright().isEmpty());
        presentationSectionPage.setCopyright("test-copyright");
        presentationSectionPage.clickSave();
        // The page is reloaded, we can see directly on this page if the copyright is correctly applied.
        assertEquals("test-copyright", presentationSectionPage.getFooterCopyright());
    }

    @Test
    @Order(6)
    void customizeVersion(TestUtils setup, TestReference testReference) throws Exception
    {
        PresentationAdministrationSectionPage presentationSectionPage = gotoPresentationAdministration();
        String defaultVersion = presentationSectionPage.getFooterVersion();
        // The default version should contain the xwiki-platform project version.
        String version = setup.getVersion();
        assertTrue(defaultVersion.contains(version));
        presentationSectionPage.setVersion("test-version");
        presentationSectionPage.clickSave();
        // The page is reloaded, we can see directly on this page if the version is correctly applied.
        assertEquals("test-version", presentationSectionPage.getFooterVersion());
    }

    /**
     * The "Show page tabs" setting must hide the whole tab area, whatever the individual tabs are set to.
     */
    @Test
    @Order(7)
    void hideAllTabs(TestUtils setup, TestReference testReference)
    {
        PresentationAdministrationSectionPage presentationSectionPage = gotoPresentationAdministration();
        presentationSectionPage.setShowComments(PresentationAdministrationSectionPage.ShowTabValue.YES);
        presentationSectionPage.setShowHistory(PresentationAdministrationSectionPage.ShowTabValue.YES);
        presentationSectionPage.clickSave();

        ViewPage viewPage = setup.createPage(testReference, "");
        assertTrue(viewPage.hasCommentsDocExtraPane());
        assertTrue(viewPage.hasHistoryDocExtraPane());

        presentationSectionPage = gotoPresentationAdministration();
        assertFalse(presentationSectionPage.isHiddenNoticeDisplayed());
        presentationSectionPage.setShowDocumentTabs(PresentationAdministrationSectionPage.ShowTabValue.NO);
        presentationSectionPage.clickSave();

        presentationSectionPage = gotoPresentationAdministration();
        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.NO,
            presentationSectionPage.getShowDocumentTabs());
        // The individual settings are kept but the section says that they are not applied.
        assertTrue(presentationSectionPage.isHiddenNoticeDisplayed());
        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.YES,
            presentationSectionPage.getShowComments());

        viewPage = setup.gotoPage(testReference);
        assertFalse(viewPage.hasCommentsDocExtraPane());
        assertFalse(viewPage.hasHistoryDocExtraPane());

        // Restore the tabs and check that the individual settings are applied again.
        presentationSectionPage = gotoPresentationAdministration();
        presentationSectionPage.setShowDocumentTabs(PresentationAdministrationSectionPage.ShowTabValue.DEFAULT);
        presentationSectionPage.clickSave();

        viewPage = setup.gotoPage(testReference);
        assertTrue(viewPage.hasCommentsDocExtraPane());
    }

    /**
     * A tab contributed to the docextra extension point must get its own field in the Presentation section, and hiding
     * it there must win over the {@code show} parameter of the extension.
     */
    @Test
    @Order(8)
    void hideCustomTab(TestUtils setup, TestReference testReference) throws Exception
    {
        DocumentReference tabReference =
            new DocumentReference("CustomTab", testReference.getLastSpaceReference());
        String tabId = tabReference.toString();
        createCustomDocExtraTab(setup, tabReference, "Custom tab content.",
            Map.of("show", "true", "name", "customTab", "title", "Custom Tab", "itemnumber", "-1", "order", "1"));
        try {
            hideCustomTab(setup, testReference, tabId);
        } finally {
            setup.rest().delete(tabReference);
        }
    }

    private void hideCustomTab(TestUtils setup, TestReference testReference, String tabId)
    {
        ViewPage viewPage = setup.createPage(testReference, "");
        assertTrue(viewPage.hasDocExtraPane(tabId));

        PresentationAdministrationSectionPage presentationSectionPage = gotoPresentationAdministration();
        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.DEFAULT,
            presentationSectionPage.getCustomTabVisibility(tabId));
        presentationSectionPage.setCustomTabVisibility(tabId,
            PresentationAdministrationSectionPage.ShowTabValue.NO);
        presentationSectionPage.clickSave();

        presentationSectionPage = gotoPresentationAdministration();
        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.NO,
            presentationSectionPage.getCustomTabVisibility(tabId));

        viewPage = setup.gotoPage(testReference);
        assertFalse(viewPage.hasDocExtraPane(tabId));

        // Restore the inherited value so that the extension's own 'show' parameter applies again.
        presentationSectionPage = gotoPresentationAdministration();
        presentationSectionPage.setCustomTabVisibility(tabId,
            PresentationAdministrationSectionPage.ShowTabValue.DEFAULT);
        presentationSectionPage.clickSave();

        viewPage = setup.gotoPage(testReference);
        assertTrue(viewPage.hasDocExtraPane(tabId));
    }

    /**
     * A space can hide a single tab without restating the visibility of the others, and the tabs it does not mention
     * keep the value set on the wiki.
     */
    @Test
    @Order(9)
    void hideCustomTabInOneSpaceOnly(TestUtils setup, TestReference testReference) throws Exception
    {
        SpaceReference space = testReference.getLastSpaceReference();
        DocumentReference tabReference = new DocumentReference("SpaceCustomTab", space);
        String tabId = tabReference.toString();
        createCustomDocExtraTab(setup, tabReference, "Custom tab content.",
            Map.of("show", "true", "name", "spaceCustomTab", "title", "Space Custom Tab", "itemnumber", "-1",
                "order", "1"));
        try {
            DocumentReference insideSpace = new DocumentReference("Inside", space);
            DocumentReference outsideSpace =
                new DocumentReference("Outside", new SpaceReference("PresentationITOutside", space.getWikiReference()));
            setup.createPage(insideSpace, "");
            setup.createPage(outsideSpace, "");

            // Hide the tab on the wiki, and show the comments tab there too so that we can check that the space level
            // does not discard the wiki level for the tabs it does not mention.
            PresentationAdministrationSectionPage presentationSectionPage = gotoPresentationAdministration();
            presentationSectionPage.setCustomTabVisibility(tabId,
                PresentationAdministrationSectionPage.ShowTabValue.NO);
            presentationSectionPage.clickSave();

            assertFalse(setup.gotoPage(insideSpace).hasDocExtraPane(tabId));
            assertFalse(setup.gotoPage(outsideSpace).hasDocExtraPane(tabId));

            // Show it back for that space only.
            AdministrationSectionPage.gotoSpaceAdministration(space, "Presentation");
            presentationSectionPage = new PresentationAdministrationSectionPage();
            assertEquals(PresentationAdministrationSectionPage.ShowTabValue.DEFAULT,
                presentationSectionPage.getCustomTabVisibility(tabId));
            presentationSectionPage.setCustomTabVisibility(tabId,
                PresentationAdministrationSectionPage.ShowTabValue.YES);
            presentationSectionPage.clickSave();

            assertTrue(setup.gotoPage(insideSpace).hasDocExtraPane(tabId));
            assertFalse(setup.gotoPage(outsideSpace).hasDocExtraPane(tabId));

            // Reset the wiki level.
            presentationSectionPage = gotoPresentationAdministration();
            presentationSectionPage.setCustomTabVisibility(tabId,
                PresentationAdministrationSectionPage.ShowTabValue.DEFAULT);
            presentationSectionPage.clickSave();
        } finally {
            setup.rest().delete(tabReference);
        }
    }

    private void createCustomDocExtraTab(TestUtils setup, DocumentReference reference, String content,
        Map<String, String> parameters) throws Exception
    {
        setup.rest().delete(reference);
        setup.rest().savePage(reference, "", "");

        org.xwiki.rest.model.jaxb.Object object = setup.rest().object(reference, "XWiki.UIExtensionClass", 0);
        object.getProperties().add(RestTestUtils.property("content", content));
        object.getProperties().add(RestTestUtils.property("extensionPointId", "org.xwiki.plaftorm.template.docextra"));
        object.getProperties().add(RestTestUtils.property("name", reference.toString()));
        object.getProperties().add(RestTestUtils.property("parameters", parameters.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue()).reduce((a, b) -> a + "\n" + b).orElse("")));
        object.getProperties().add(RestTestUtils.property("scope", "wiki"));
        setup.rest().add(object);
    }

    private static PresentationAdministrationSectionPage gotoPresentationAdministration()
    {
        AdministrablePage administrablePage = new AdministrablePage();

        // Navigate to the Presentation administration section.
        administrablePage.clickAdministerWiki().clickSection("Look & Feel", "Presentation");
        return new PresentationAdministrationSectionPage();
    }
}
