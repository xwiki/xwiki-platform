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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.export.pdf.test.po.ExportModal;
import org.xwiki.export.pdf.test.po.PDFDocument;
import org.xwiki.export.pdf.test.po.PDFExportOptionsModal;
import org.xwiki.export.pdf.test.po.PDFImage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for PDF export.
 *
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@UITest
class PDFExportIT
{
    @BeforeAll
    static void configure(TestUtils setup)
    {
        setup.gotoPage(new LocalDocumentReference("PDFExportIT", "EnableDebugLogs"), "get");
    }

    @Test
    void exportAsPDF(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration) throws Exception
    {
        setup.createUserAndLogin("John", "pass");

        ViewPage viewPage =
            setup.gotoPage(new LocalDocumentReference(Arrays.asList("PDFExportIT", "Parent"), "WebHome"));
        PDFExportOptionsModal exportOptions = ExportModal.open(viewPage).clickExportAsPDFButton();

        try (PDFDocument pdf = exportOptions.export(getHostURL(testConfiguration))) {
            // We should have 3 pages: cover page, table of contents and the actual content.
            assertEquals(3, pdf.getNumberOfPages());

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
            // The header shows the page title and the footer shows the page number and the page count.
            assertTrue(tocPageText.startsWith("Parent\n2 / 3\n"),
                "Unexpected header and footer on table of contents page: " + coverPageText);
            assertTrue(tocPageText.contains("Table of Contents\nChapter 1\nSection 1\n"),
                "Unexpected table of contents: " + tocPageText);

            // The table of contents should have internal links (anchors) to each section.
            Map<String, String> tocPageLinks = pdf.getLinksFromPage(1);
            assertEquals(2, tocPageLinks.size());
            assertEquals(Arrays.asList("HChapter1", "HSection1"),
                tocPageLinks.values().stream().collect(Collectors.toList()));

            // No images on the table of contents page.
            assertEquals(0, pdf.getImagesFromPage(1).size());

            //
            // Verify the content page.
            //

            String contentPageText = pdf.getTextFromPage(2);
            assertTrue(contentPageText.startsWith("Parent\n3 / 3\n"),
                "Unexpected header and footer on the content page: " + contentPageText);
            assertTrue(
                contentPageText.contains("Chapter 1\n"
                    + "Content of first chapter. Current user is xwiki:XWiki.John.\nLink to child page.\nloaded!\n"),
                "Parent page content missing: " + contentPageText);
            assertTrue(contentPageText.contains("Section 1\nContent of first section.\n"),
                "Child page content missing: " + contentPageText);

            // The content of the parent page has a link to the child page.
            Map<String, String> contentPageLinks = pdf.getLinksFromPage(2);
            assertEquals(1, contentPageLinks.size());
            assertEquals(setup.getURL(Arrays.asList("PDFExportIT", "Parent"), "Child") + "/",
                contentPageLinks.get("child page."));

            // The content of the child page shows an image.
            List<PDFImage> contentPageImages = pdf.getImagesFromPage(2);
            assertEquals(1, contentPageImages.size());
            assertEquals(512, contentPageImages.get(0).getWidth());
            assertEquals(512, contentPageImages.get(0).getHeight());
        }
    }

    private URL getHostURL(TestConfiguration testConfiguration) throws Exception
    {
        return new URL(String.format("http://%s:%d", testConfiguration.getServletEngine().getIP(),
            testConfiguration.getServletEngine().getPort()));
    }
}
