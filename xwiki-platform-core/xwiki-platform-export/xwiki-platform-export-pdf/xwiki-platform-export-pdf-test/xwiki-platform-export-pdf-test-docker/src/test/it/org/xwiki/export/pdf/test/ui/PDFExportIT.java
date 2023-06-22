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
package org.xwiki.export.pdf.test.ui;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.Network;
import org.xwiki.export.pdf.internal.docker.ContainerManager;
import org.xwiki.export.pdf.test.po.PDFDocument;
import org.xwiki.export.pdf.test.po.PDFExportAdministrationSectionPage;
import org.xwiki.export.pdf.test.po.PDFExportOptionsModal;
import org.xwiki.export.pdf.test.po.PDFImage;
import org.xwiki.export.pdf.test.po.PDFTemplateEditPage;
import org.xwiki.flamingo.skin.test.po.ExportTreeModal;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.internal.junit5.DockerTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for PDF export.
 *
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@UITest(extraJARs = {"org.xwiki.platform:xwiki-platform-resource-temporary"})
@ExtendWith(PDFExportExecutionCondition.class)
class PDFExportIT
{
    @BeforeAll
    static void configure()
    {
        // Cleanup the Chrome Docker containers used for PDF export.
        DockerTestUtils.cleanupContainersWithLabels(ContainerManager.DEFAULT_LABELS);
    }

    @Test
    @Order(1)
    void configurePDFExport(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Restrict view access for guests in order to verify that the Chrome Docker container properly authenticates
        // the user (the authentication cookies are copied and updated to match the Chrome Docker container IP address).
        setup.setWikiPreference("authenticate_view", "1");

        // Enable the 'org.xwiki.platform.html.head' UIXP which normally is provided by
        // 'org.xwiki.platform:xwiki-platform-distribution-ui-base' but we didn't add it as a test dependency because it
        // brings too many transitive dependencies that we don't need.
        setup.setWikiPreference("meta",
            "#foreach($uix in $services.uix.getExtensions(\"org.xwiki.platform.html.head\","
                + " {'sortByParameter' : 'order'}))\n" + "  $services.rendering.render($uix.execute(), 'xhtml/1.0')\n"
                + "#end");

        // Enable debug logs for the PDF export code.
        setup.gotoPage(new LocalDocumentReference("PDFExportIT", "EnableDebugLogs"), "get");

        // Make sure we start with the default settings.
        PDFExportAdministrationSectionPage adminSection = PDFExportAdministrationSectionPage.gotoPage().reset();
        adminSection.getGeneratorSelect().selectByVisibleText("Chrome Docker Container");

        if (!testConfiguration.getServletEngine().isOutsideDocker()) {
            // The servlet engine runs inside a Docker container so in order for the headless Chrome web browser (used
            // for PDF export) to access XWiki its own Docker container has to be in the same network and we also need
            // to pass the internal host name or IP address used by XWiki.
            adminSection.setDockerNetwork(Network.SHARED.getId());
            adminSection.setXWikiHost(testConfiguration.getServletEngine().getInternalIP());
        }

        adminSection.clickSave();
        assertEquals("Available", adminSection.getGeneratorStatus(true));
    }

    @Test
    @Order(2)
    void exportAsPDF(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        setup.createUserAndLogin("John", "pass");

        ViewPage viewPage =
            setup.gotoPage(new LocalDocumentReference(Arrays.asList("PDFExportIT", "Parent"), "WebHome"));
        ExportTreeModal exportTreeModal = ExportTreeModal.open(viewPage, "PDF");
        // The title should match the selected export format.
        assertEquals("Export as PDF", exportTreeModal.getTitle());
        // Only the current page should be selected by default.
        assertEquals(Collections.singletonList("document:xwiki:PDFExportIT.Parent.WebHome"),
            exportTreeModal.getPageTree().getSelectedNodeIDs());
        assertTrue(exportTreeModal.getPageTree().getNode("document:xwiki:PDFExportIT.Parent.WebHome").isOpen());
        // Hidden pages should not be listed.
        assertFalse(exportTreeModal.getPageTree().hasNode("document:xwiki:PDFExportIT.Parent.Hidden.WebHome"));
        // Include the child page in the export.
        exportTreeModal.getPageTree().getNode("document:xwiki:PDFExportIT.Parent.Child.WebHome").select();
        assertEquals(Collections.emptyList(), exportTreeModal.getPagesValues());
        exportTreeModal.export();
        PDFExportOptionsModal exportOptions = new PDFExportOptionsModal();

        try (PDFDocument pdf = export(exportOptions, testConfiguration)) {
            // We should have 4 pages: cover page, table of contents, one page for the parent document and one page for
            // the child document.
            assertEquals(4, pdf.getNumberOfPages());

            //
            // Verify the cover page.
            //

            String coverPageText = pdf.getTextFromPage(0);
            assertTrue(coverPageText.startsWith("Parent\nVersion 1.1 authored by superadmin"),
                "Unexpected cover page text: " + coverPageText);

            // Link to the author profile.
            Map<String, String> coverPageLinks = pdf.getLinksFromPage(0);
            assertEquals(1, coverPageLinks.size());
            assertEquals(setup.getURL("XWiki", "superadmin"), coverPageLinks.get("superadmin"));

            // Author image.
            List<PDFImage> coverPageImages = pdf.getImagesFromPage(0);
            assertEquals(1, coverPageImages.size());
            assertEquals(160, coverPageImages.get(0).getWidth());
            assertEquals(160, coverPageImages.get(0).getHeight());

            //
            // Verify the table of contents page.
            //

            String tocPageText = pdf.getTextFromPage(1);
            assertTrue(tocPageText.contains("Table of Contents\nParent\nChapter 1\nChild\nSection 1\n"),
                "Unexpected table of contents: " + tocPageText);

            // The table of contents should have internal links (anchors) to each section.
            Map<String, String> tocPageLinks = pdf.getLinksFromPage(1);
            assertEquals(4, tocPageLinks.size());
            assertEquals(
                Arrays.asList("Hxwiki:PDFExportIT.Parent.WebHome", "HChapter1",
                    "Hxwiki:PDFExportIT.Parent.Child.WebHome", "HSection1"),
                tocPageLinks.values().stream().collect(Collectors.toList()));

            // No images on the table of contents page.
            assertEquals(0, pdf.getImagesFromPage(1).size());

            //
            // Verify the page corresponding to the parent document.
            //

            String contentPageText = pdf.getTextFromPage(2);
            assertTrue(contentPageText.startsWith("Parent\n3 / 4\n"),
                "Unexpected header and footer on the content page: " + contentPageText);
            assertTrue(
                contentPageText.contains("Parent\nChapter 1\n"
                    + "Content of first chapter. Current user is xwiki:XWiki.John.\nLink to child page.\nloaded!\n"),
                "Parent document content missing: " + contentPageText);

            // The content of the parent document has a link to the child document.
            Map<String, String> contentPageLinks = pdf.getLinksFromPage(2);
            assertEquals(1, contentPageLinks.size());
            assertEquals("Hxwiki:PDFExportIT.Parent.Child.WebHome", contentPageLinks.get("child page."));

            //
            // Verify the page corresponding to the child document.
            //

            contentPageText = pdf.getTextFromPage(3);
            assertTrue(contentPageText.startsWith("Child\n4 / 4\n"),
                "Unexpected header and footer on the content page: " + contentPageText);
            assertTrue(contentPageText.contains("Child\nSection 1\nContent of first section.\n"),
                "Child document content missing: " + contentPageText);

            // The content of the child document shows an image.
            List<PDFImage> contentPageImages = pdf.getImagesFromPage(3);
            assertEquals(1, contentPageImages.size());
            assertEquals(512, contentPageImages.get(0).getWidth());
            assertEquals(512, contentPageImages.get(0).getHeight());
        }
    }

    @Test
    @Order(3)
    void exportSinglePageAsPDF(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        ViewPage viewPage =
            setup.gotoPage(new LocalDocumentReference(Arrays.asList("PDFExportIT", "Parent", "Child"), "WebHome"));
        PDFExportOptionsModal exportOptions = PDFExportOptionsModal.open(viewPage);

        try (PDFDocument pdf = export(exportOptions, testConfiguration)) {
            // We should have 3 pages: cover page, table of contents and one page for the content.
            assertEquals(3, pdf.getNumberOfPages());

            //
            // Verify the table of contents page.
            //

            String tocPageText = pdf.getTextFromPage(1);
            // The document title is not included when a single page is exported.
            assertTrue(tocPageText.contains("Table of Contents\nSection 1"),
                "Unexpected table of contents: " + tocPageText);

            // The table of contents should have internal links (anchors) to each section.
            Map<String, String> tocPageLinks = pdf.getLinksFromPage(1);
            assertEquals(Collections.singletonList("HSection1"),
                tocPageLinks.values().stream().collect(Collectors.toList()));

            //
            // Verify the content page.
            //

            String contentPageText = pdf.getTextFromPage(2);
            // The document title is not included when a single page is exported.
            assertTrue(contentPageText.startsWith("Child\n3 / 3\nSection 1\nContent of first section.\n"),
                "Unexpected content: " + contentPageText);
        }
    }

    @Test
    @Order(4)
    void exportWithCustomPDFTemplate(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
        throws Exception
    {
        setup.rest().delete(new LocalDocumentReference("WebHome",
            new EntityReference("My cool template", EntityType.SPACE, testReference.getLastSpaceReference())));

        // Create a custom PDF template, as a simple user.
        setup.createPage(testReference, "", "").createPage().createPageFromTemplate("My cool template", null, null,
            "XWiki.PDFExport.TemplateProvider");
        PDFTemplateEditPage templateEditPage = new PDFTemplateEditPage();
        templateEditPage.setCover(templateEditPage.getCover().replace("<h1>", "<h1>Book: "));
        templateEditPage
            .setTableOfContents(templateEditPage.getTableOfContents().replace("core.pdf.tableOfContents", "Chapters"));
        templateEditPage.setHeader(templateEditPage.getHeader().replace("$esc", "Chapter: $esc"));
        templateEditPage.setFooter(templateEditPage.getFooter().replaceFirst("<span ", "Page <span "));
        templateEditPage.clickSaveAndContinue();

        // Register the template in the PDF export administration section.
        setup.loginAsSuperAdmin();
        PDFExportAdministrationSectionPage adminSection = PDFExportAdministrationSectionPage.gotoPage();
        adminSection.getTemplatesInput().sendKeys("my cool").waitForSuggestions()
            .selectByVisibleText("My cool template");
        adminSection.clickSave();

        // We also have to give script rights to the template author because it was created based on the default one
        // which contains scripts.
        setup.setGlobalRights(null, "XWiki.John", "script", true);

        // Export using the custom PDF template we created.
        setup.loginAndGotoPage("John", "pass",
            setup.getURL(new LocalDocumentReference(Arrays.asList("PDFExportIT", "Parent"), "WebHome")));
        PDFExportOptionsModal exportOptions = PDFExportOptionsModal.open(new ViewPage());
        exportOptions.getTemplateSelect().selectByVisibleText("My cool template");

        try (PDFDocument pdf = export(exportOptions, testConfiguration)) {
            // Verify that the custom PDF template was used.

            // We should have 3 pages: cover page, table of contents and one content page.
            assertEquals(3, pdf.getNumberOfPages());

            // Verify the custom cover page.
            String coverPageText = pdf.getTextFromPage(0);
            assertTrue(coverPageText.contains("Book: Parent"), "Unexpected cover page text: " + coverPageText);

            // Verify the custom table of contents page.
            String tocPageText = pdf.getTextFromPage(1);
            assertTrue(tocPageText.contains("Chapters"), "Unexpected table of contents: " + tocPageText);

            // Verify the custom PDF header and footer.
            String contentPageText = pdf.getTextFromPage(2);
            assertTrue(contentPageText.startsWith("Chapter: Parent\nPage 3 / 3\n"),
                "Unexpected header and footer on the content page: " + contentPageText);
        }
    }

    @Test
    @Order(5)
    void exportHiddenPageAsPDF(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        //
        // Export directly a nested hidden page.
        //
        ViewPage viewPage =
            setup.gotoPage(new LocalDocumentReference(Arrays.asList("PDFExportIT", "Parent", "Hidden"), "WebHome"));
        PDFExportOptionsModal exportOptions = PDFExportOptionsModal.open(viewPage);
        try (PDFDocument pdf = exportOnlyContent(exportOptions, testConfiguration)) {
            assertEquals(1, pdf.getNumberOfPages());
            // The document title is not included when a single page is exported.
            assertEquals("Hidden content\n", pdf.getTextFromPage(0));
        }

        //
        // Export directly a terminal hidden page.
        //
        LocalDocumentReference grandchildReference =
            new LocalDocumentReference(Arrays.asList("PDFExportIT", "Parent", "Hidden"), "Grandchild");
        viewPage = setup.gotoPage(grandchildReference);
        exportOptions = PDFExportOptionsModal.open(viewPage);
        try (PDFDocument pdf = exportOnlyContent(exportOptions, testConfiguration)) {
            assertEquals(1, pdf.getNumberOfPages());
            // The document title is not included when a single page is exported.
            assertEquals("Once upon a time...\n", pdf.getTextFromPage(0));
        }

        //
        // Include a hidden nested page in the export (the reason it's visible in the tree is because one of its
        // descendant pages is not hidden).
        //

        // Make the grandchild page visible in order to make its parent page visible in the tree.
        setup.gotoPage(grandchildReference, "save", "xhidden", false);

        viewPage = setup.gotoPage(new LocalDocumentReference(Arrays.asList("PDFExportIT", "Parent"), "WebHome"));
        ExportTreeModal exportTreeModal = ExportTreeModal.open(viewPage, "PDF");
        // Include the hidden child page in the export.
        exportTreeModal.getPageTree().getNode("document:xwiki:PDFExportIT.Parent.Hidden.WebHome").select();
        exportTreeModal.export();
        exportOptions = new PDFExportOptionsModal();

        try (PDFDocument pdf = exportOnlyContent(exportOptions, testConfiguration)) {
            assertEquals(3, pdf.getNumberOfPages());
            String pageText = pdf.getTextFromPage(0);
            assertTrue(pageText.startsWith("Parent\nChapter 1\n"), "Unexpeced parent document content: " + pageText);
            assertEquals("Hidden\nHidden content\n", pdf.getTextFromPage(1));
            assertEquals("Grandchild\nOnce upon a time...\n", pdf.getTextFromPage(2));
        }

        // Restore the visibility of the grandchild page.
        setup.gotoPage(grandchildReference, "save", "xhidden", true);
    }

    @Test
    @Order(6)
    void updatePDFExportConfigurationWithValidation(TestUtils setup, TestConfiguration testConfiguration)
        throws Exception
    {
        setup.loginAsSuperAdmin();
        PDFExportAdministrationSectionPage adminSection = PDFExportAdministrationSectionPage.gotoPage();

        // Verify the client-side validation.
        assertTrue(adminSection.isChromeDockerContainerNameValid());
        assertTrue(adminSection.isChromeRemoteDebuggingPortValid());

        adminSection.setChromeDockerContainerName("%^&*");
        adminSection.setChromeRemoteDebuggingPort("-9222");
        adminSection.clickSave(false);

        assertFalse(adminSection.isChromeDockerContainerNameValid());
        assertFalse(adminSection.isChromeRemoteDebuggingPortValid());

        // Change the configuration.
        adminSection.setChromeDockerContainerName("headless-chrome-pdf-printer-" + new Date().getTime());
        adminSection.setChromeRemoteDebuggingPort("9223");

        assertTrue(adminSection.isChromeDockerContainerNameValid());
        assertTrue(adminSection.isChromeRemoteDebuggingPortValid());

        adminSection.clickSave();
        assertEquals("Available", adminSection.getGeneratorStatus(true));

        // Try the PDF export with the new generator.
        setup.loginAndGotoPage("John", "pass",
            setup.getURL(new LocalDocumentReference(Arrays.asList("PDFExportIT", "Parent"), "WebHome")));
        PDFExportOptionsModal exportOptions = PDFExportOptionsModal.open(new ViewPage());
        exportOptions.getCoverCheckbox().click();
        exportOptions.getTocCheckbox().click();

        try (PDFDocument pdf = export(exportOptions, testConfiguration)) {
            // One content page.
            assertEquals(1, pdf.getNumberOfPages());
            String content = pdf.getTextFromPage(0);
            assertTrue(content.contains("Chapter 1"), "Unexpected content: " + content);
        }
    }

    @Test
    @Order(7)
    void invalidTOCAnchors(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        ViewPage viewPage = setup.gotoPage(new LocalDocumentReference("PDFExportIT", "InvalidTOCAnchors"));
        PDFExportOptionsModal exportOptions = PDFExportOptionsModal.open(viewPage);

        try (PDFDocument pdf = export(exportOptions, testConfiguration)) {
            // We should have 3 pages: cover page, table of contents and one page for the content.
            assertEquals(3, pdf.getNumberOfPages());

            //
            // Verify the table of contents page.
            //

            String tocPageText = pdf.getTextFromPage(1);
            // The document title is not included when a single page is exported.
            assertTrue(tocPageText.contains("Table of Contents\nWithoutID\nDigitsAndSpace\nSymbols\nValid"),
                "Unexpected table of contents: " + tocPageText);

            // The table of contents should have internal links (anchors) to each section, provided the sections have a
            // valid id (otherwise the section title is displayed but without a link).
            Map<String, String> tocPageLinks = pdf.getLinksFromPage(1);
            assertEquals(Collections.singletonList("HValid"),
                tocPageLinks.values().stream().collect(Collectors.toList()));

            //
            // Verify the content page.
            //

            String contentPageText = pdf.getTextFromPage(2);
            // The document title is not included when a single page is exported.
            assertTrue(
                contentPageText.startsWith("InvalidTOCAnchors\n3 / 3\nWithoutID\nDigitsAndSpace\nSymbols\nValid"),
                "Unexpected content: " + contentPageText);
        }
    }

    @Test
    @Order(8)
    void refactorAnchors(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        setup.login("John", "pass");

        ViewPage viewPage =
            setup.gotoPage(new LocalDocumentReference(Arrays.asList("PDFExportIT", "Anchors"), "WebHome"));
        ExportTreeModal exportTreeModal = ExportTreeModal.open(viewPage, "PDF");
        // Include the child page in the export.
        exportTreeModal.getPageTree().getNode("document:xwiki:PDFExportIT.Anchors.Child").select();
        exportTreeModal.export();
        PDFExportOptionsModal exportOptions = new PDFExportOptionsModal();

        try (PDFDocument pdf = export(exportOptions, testConfiguration)) {
            //
            // Verify the anchors from the parent document.
            //

            Map<String, String> expectedLinks = new LinkedHashMap<>();
            expectedLinks.put("Self", "Hxwiki:PDFExportIT.Anchors.WebHome");
            // Anchor to a section from this document.
            expectedLinks.put("Usage", "HUsage");
            // Anchor to a section from this document that is not found in the PDF.
            // expectedLinks.put("Comments", setup.getBaseBinURL() + "view/PDFExportIT/Anchors/#Comments");
            // Anchors that use a query string cannot be made internal.
            expectedLinks.put("History", setup.getBaseBinURL() + "view/PDFExportIT/Anchors/?viewer=history#");
            expectedLinks.put("Diff", setup.getBaseBinURL() + "view/PDFExportIT/Anchors/?viewer=changes#diff");
            // Anchors that don't target the view action cannot be made internal.
            expectedLinks.put("Edit", setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/WebHome");
            expectedLinks.put("Edit Description",
                setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/WebHome#HDescription");
            expectedLinks.put("Edit Wiki", setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/WebHome?editor=wiki");
            expectedLinks.put("Edit Usage Wiki",
                setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/WebHome?editor=wiki#HUsage");

            // Anchor to the child document that is included in the PDF.
            expectedLinks.put("Child", "Hxwiki:PDFExportIT.Anchors.Child");
            // Anchor to a section from the child document that is included in the PDF.
            expectedLinks.put("Child Usage", "HUsage-1");
            // Anchor to a section from the child document that is not found in the PDF.
            expectedLinks.put("Child Comments", setup.getBaseBinURL() + "view/PDFExportIT/Anchors/Child#Comments");
            // Anchors that use a query string cannot be made internal.
            expectedLinks.put("Child History", setup.getBaseBinURL() + "view/PDFExportIT/Anchors/Child?viewer=history");
            expectedLinks.put("Child Diff",
                setup.getBaseBinURL() + "view/PDFExportIT/Anchors/Child?viewer=changes#diff");
            // Anchors that don't target the view action cannot be made internal.
            expectedLinks.put("Child Edit", setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/Child");
            expectedLinks.put("Child Edit Description",
                setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/Child#HDescription");
            expectedLinks.put("Child Edit Wiki", setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/Child?editor=wiki");
            expectedLinks.put("Child Edit Usage Wiki",
                setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/Child?editor=wiki#HUsage");

            // Anchor to another section from this document.
            expectedLinks.put("Description", "HDescription");
            // Anchor to another section from the child document that is included in the PDF.
            expectedLinks.put("Child Description", "HDescription-1");
            // Anchor to a section from a document that is not included in the PDF.
            expectedLinks.put("Other Description", setup.getBaseBinURL() + "view/PDFExportIT/Parent/#HDescription");

            assertEquals(expectedLinks, pdf.getLinksFromPage(2));

            //
            // Verify the anchors from the child document.
            //

            expectedLinks.clear();
            expectedLinks.put("Self", "Hxwiki:PDFExportIT.Anchors.Child");
            // Anchor to a section from this document.
            expectedLinks.put("Usage", "HUsage-1");
            // Anchor to a section from this document that is not found in the PDF.
            expectedLinks.put("Comments", setup.getBaseBinURL() + "view/PDFExportIT/Anchors/Child#Comments");
            // Anchors that use a query string cannot be made internal.
            expectedLinks.put("History", setup.getBaseBinURL() + "view/PDFExportIT/Anchors/Child?viewer=history#");
            expectedLinks.put("Diff", setup.getBaseBinURL() + "view/PDFExportIT/Anchors/Child?viewer=changes#diff");
            // Anchors that don't target the view action cannot be made internal.
            expectedLinks.put("Edit", setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/Child");
            expectedLinks.put("Edit Description",
                setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/Child#HDescription");
            expectedLinks.put("Edit Wiki", setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/Child?editor=wiki");
            expectedLinks.put("Edit Usage Wiki",
                setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/Child?editor=wiki#HUsage");

            // Anchor to the parent document that is included in the PDF.
            expectedLinks.put("Parent", "Hxwiki:PDFExportIT.Anchors.WebHome");
            // Anchor to a section from the parent document that is included in the PDF.
            expectedLinks.put("Parent Usage", "HUsage");
            // Anchor to a section from the parent document that is not found in the PDF.
            // expectedLinks.put("Parent Comments", setup.getBaseBinURL() + "view/PDFExportIT/Anchors/#Comments");
            // Anchors that use a query string cannot be made internal.
            expectedLinks.put("Parent History", setup.getBaseBinURL() + "view/PDFExportIT/Anchors/?viewer=history");
            expectedLinks.put("Parent Diff", setup.getBaseBinURL() + "view/PDFExportIT/Anchors/?viewer=changes#diff");
            // Anchors that don't target the view action cannot be made internal.
            expectedLinks.put("Parent Edit", setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/WebHome");
            expectedLinks.put("Parent Edit Description",
                setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/WebHome#HDescription");
            expectedLinks.put("Parent Edit Wiki",
                setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/WebHome?editor=wiki");
            expectedLinks.put("Parent Edit Usage Wiki",
                setup.getBaseBinURL() + "edit/PDFExportIT/Anchors/WebHome?editor=wiki#HUsage");

            // Anchor to another section from this document.
            expectedLinks.put("Description", "HDescription-1");
            // Anchor to another section from the parent document that is included in the PDF.
            expectedLinks.put("Parent Description", "HDescription");
            // Anchor to a section from a document that is not included in the PDF.
            expectedLinks.put("Other Description", setup.getBaseBinURL() + "view/PDFExportIT/Parent/#HDescription");

            assertEquals(expectedLinks, pdf.getLinksFromPage(3));
        }
    }

    @Test
    @Order(9)
    void numberedHeadings(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        ViewPage viewPage = setup.gotoPage(new LocalDocumentReference("PDFExportIT", "NumberedHeadings"));
        PDFExportOptionsModal exportOptions = PDFExportOptionsModal.open(viewPage);

        try (PDFDocument pdf = export(exportOptions, testConfiguration)) {
            // We should have 3 pages: cover page, table of contents and one page for the content.
            assertEquals(3, pdf.getNumberOfPages());

            //
            // Verify the table of contents page.
            //

            String tocPageText = pdf.getTextFromPage(1);
            assertTrue(
                tocPageText.contains("Table of Contents\n" + "1 Heading 1\n" + "1.1 Heading 1-1\n"
                    + "Heading without number\n" + "1.1.1 Heading 1-1-1\n" + "1.1.2 Heading 1-1-2\n"
                    + "1.2 Heading 1-2\n" + "1.2.1 Heading 1-2-1\n" + "1.7 Heading 1-7\n" + "1.7.1 Heading 1-7-1\n"
                    + "1.7.5 Heading 1-7-5\n" + "1.7.6 Heading 1-7-6\n" + "1.8 Heading 1-8\n" + "2 Heading 2\n"
                    + "2.1 Heading 2-1\n" + "2.1.1 Heading 2-1-1\n" + ""),
                "Unexpected table of contents: " + tocPageText);

            //
            // Verify the content page.
            //

            String contentPageText = pdf.getTextFromPage(2);
            assertEquals("NumberedHeadings\n" + "3 / 3\n" + "1 Heading 1\n" + "1.1 Heading 1-1\n"
                + "Heading without number\n" + "1.1.1 Heading 1-1-1\n" + "1.1.2 Heading 1-1-2\n" + "1.2 Heading 1-2\n"
                + "1.2.1 Heading 1-2-1\n" + "1.2.1.1 Heading 1-2-1-1\n" + "1.2.1.2 Heading 1-2-1-2\n"
                + "1.7 Heading 1-7\n" + "1.7.1 Heading 1-7-1\n" + "1.7.5 Heading 1-7-5\n" + "1.7.6 Heading 1-7-6\n"
                + "1.8 Heading 1-8\n" + "2 Heading 2\n" + "2.1 Heading 2-1\n" + "2.1.1 Heading 2-1-1\n"
                + "2.1.1.1 Heading 2-1-1-1\n" + "2.1.1.1.1 Heading 2-1-1-1-1\n" + "2.1.1.1.1.1 Heading 2-1-1-1-1-1\n"
                + "2.1.1.1.1.2 Heading 2-1-1-1-1-2\n", contentPageText);
        }
    }

    @Test
    @Order(10)
    void formFields(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        ViewPage viewPage = setup.gotoPage(new LocalDocumentReference("PDFExportIT", "FormFields"));
        PDFExportOptionsModal exportOptions = PDFExportOptionsModal.open(viewPage);

        try (PDFDocument pdf = export(exportOptions, testConfiguration)) {
            // We should have 2 pages: cover page and content page.
            assertEquals(2, pdf.getNumberOfPages());

            String content = pdf.getTextFromPage(1);
            assertEquals("FormFields\n2 / 2\n" + "Title modified\n"
                + " Blue  Yellow  Red\n" + "Paris\n" + "Comedy\nDrama\nRomance\n"
                + "description modified\n" + "Submit\n"
                + "TITLE\n" + " ENABLED\n" + "COLOR\n" + "CITY\n" + "GENRE\n" + "DESCRIPTION\n", content);
        }
    }

    @Test
    @Order(11)
    void liveTable(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        // Create a child page because we want to verify that the PDF export preserves the live table sort (we sort by
        // last modification date and the child page we create should be the most recent).
        LocalDocumentReference parent =
            new LocalDocumentReference(Arrays.asList("PDFExportIT", "LiveTable"), "WebHome");
        setup.createPage(new LocalDocumentReference("Child", parent.getParent()), "");

        ViewPage viewPage = setup.gotoPage(parent);

        // Change the state of the Live Table in order to verify that the PDF export preserves it.
        LiveTableElement liveTable = new LiveTableElement("docs");
        liveTable.sortDescending("Date");
        liveTable.filterColumn("xwiki-livetable-docs-filter-2", "live");

        PDFExportOptionsModal exportOptions = PDFExportOptionsModal.open(viewPage);

        try (PDFDocument pdf = export(exportOptions, testConfiguration)) {
            // We should have 2 pages: cover page and content page.
            assertEquals(2, pdf.getNumberOfPages());

            String content = pdf.getTextFromPage(1);
            // Verify the pagination (result count), the headers and the filters.
            assertTrue(
                content
                    .contains("Results 1 - 2 out of 2 per page of 15\nPage Location Date Last Author Actions\nlive\n"),
                "Unexpected content: " + content);
            // Verify the results and the order.
            int childIndex = content.indexOf("Child PDFExportITLiveTable\nChild");
            int parentIndex = content.indexOf("WebHome PDFExportITLiveTable");
            assertTrue(childIndex < parentIndex, "Unexpected content: " + content);
        }
    }

    @Test
    @Order(12)
    void codeMacro(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        ViewPage viewPage = setup.gotoPage(new LocalDocumentReference("PDFExportIT", "CodeMacro"));
        PDFExportOptionsModal exportOptions = PDFExportOptionsModal.open(viewPage);

        try (PDFDocument pdf = export(exportOptions, testConfiguration)) {
            // We should have 2 pages: cover page and content page.
            assertEquals(2, pdf.getNumberOfPages());

            String content = pdf.getTextFromPage(1);
            // A line break is inserted whenever a long line is wrapped, so we need to remove line breaks in order to
            // verify that the entire code macro content is present.
            assertTrue(content.replace("\n", "").contains(
                "// This is a very long comment that gets cut when exported to PDF because it exceeds the print page "
                    + "width and the code macro preserves spaces which means it has to be displayed on a single line."),
                "Unexpected content: " + content);
        }
    }

    @Test
    @Order(13)
    void resizedTable(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        ViewPage viewPage = setup.gotoPage(new LocalDocumentReference("PDFExportIT", "ResizedTable"));
        PDFExportOptionsModal exportOptions = PDFExportOptionsModal.open(viewPage);

        try (PDFDocument pdf = export(exportOptions, testConfiguration)) {
            // We should have 2 pages: cover page and content page. If the resized table uses absolute widths then it
            // ends up with a very small column that spans lots of print pages. By checking that we have only 2 pages we
            // verify that the absolute widths have been replaced with relative widths.
            assertEquals(2, pdf.getNumberOfPages());

            String rawContent = pdf.getTextFromPage(1);
            // A line break is inserted whenever a long line is wrapped (e.g. inside a table cell), so we need to
            // replace line breaks with spaces in order to verify that a specific text is present.
            String content = rawContent.replace("\n", " ");
            // Verify the text from the start of the first table cells is present.
            assertTrue(content.contains("Lorem ipsum dolor sit amet,"), "Unexpected content: " + rawContent);
            assertTrue(content.contains("Augue lacus viverra vitae congue eu consequat ac."),
                "Unexpected content: " + rawContent);
            // Verify the text from the end of the last table cells is present.
            assertTrue(content.contains("Varius sit amet mattis vulputate enim nulla aliquet."),
                "Unexpected content: " + rawContent);
            assertTrue(content.contains("Felis imperdiet proin fermentum leo vel orci."),
                "Unexpected content: " + rawContent);
        }
    }

    @Test
    @Order(14)
    void pageRevision(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration) throws Exception
    {
        setup.createPage(testReference, "Parent initial content.", "Parent Initial Title");
        setup.rest().savePage(testReference, "Parent modified content.", "Parent Modified Title");

        EntityReference childReference =
            new EntityReference("Child", EntityType.DOCUMENT, testReference.getLastSpaceReference());
        setup.createPage(childReference, "Child initial content.", "Child Initial Title");
        setup.rest().savePage(childReference, "Child modified content.", "Child Modified Title");

        ViewPage viewPage = setup.gotoPage(testReference);
        assertEquals("Parent modified content.", viewPage.getContent());

        viewPage = viewPage.openHistoryDocExtraPane().viewVersion("1.1");
        assertEquals("Parent initial content.", viewPage.getContent());

        ExportTreeModal exportTreeModal = ExportTreeModal.open(viewPage, "PDF");
        // Include the child page in the export because we want to check that the revision specified in the query string
        // applies only to the current page.
        exportTreeModal.getPageTree().getNode("document:" + setup.serializeReference(childReference)).select();
        exportTreeModal.export();

        try (PDFDocument pdf = export(new PDFExportOptionsModal(), testConfiguration)) {
            // We should have 4 pages: cover page, table of contents, one page for the parent document and one page for
            // the child document.
            assertEquals(4, pdf.getNumberOfPages());

            //
            // Verify the cover page.
            //

            String coverPageText = pdf.getTextFromPage(0);
            assertTrue(coverPageText.startsWith("Parent Initial Title\nVersion 1.1 authored by John"),
                "Unexpected cover page text: " + coverPageText);

            //
            // Verify the page corresponding to the parent document.
            //

            String contentPageText = pdf.getTextFromPage(2);
            assertTrue(contentPageText.startsWith("Parent Initial Title\n3 / 4\n"),
                "Unexpected header and footer on the content page: " + contentPageText);
            assertTrue(contentPageText.contains("Parent Initial Title\nParent initial content.\n"),
                "Unexpected parent page content: " + contentPageText);

            //
            // Verify the page corresponding to the child document.
            //

            contentPageText = pdf.getTextFromPage(3);
            assertTrue(contentPageText.startsWith("Child Modified Title\n4 / 4\n"),
                "Unexpected header and footer on the content page: " + contentPageText);
            assertTrue(contentPageText.contains("Child Modified Title\nChild modified content.\n"),
                "Unexpected child page content: " + contentPageText);
        }
    }

    private URL getHostURL(TestConfiguration testConfiguration) throws Exception
    {
        return new URL(String.format("http://%s:%d", testConfiguration.getServletEngine().getIP(),
            testConfiguration.getServletEngine().getPort()));
    }

    private PDFDocument export(PDFExportOptionsModal exportOptions, TestConfiguration testConfiguration)
        throws Exception
    {
        return exportOptions.export(getHostURL(testConfiguration), "John", "pass");
    }

    private PDFDocument exportOnlyContent(PDFExportOptionsModal exportOptions, TestConfiguration testConfiguration)
        throws Exception
    {
        exportOptions.getCoverCheckbox().click();
        exportOptions.getTocCheckbox().click();
        exportOptions.getHeaderCheckbox().click();
        exportOptions.getFooterCheckbox().click();
        return export(exportOptions, testConfiguration);
    }
}
