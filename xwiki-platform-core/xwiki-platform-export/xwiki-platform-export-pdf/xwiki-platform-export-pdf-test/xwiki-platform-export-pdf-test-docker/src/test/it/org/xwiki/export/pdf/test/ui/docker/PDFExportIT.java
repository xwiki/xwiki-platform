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
package org.xwiki.export.pdf.test.ui.docker;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.export.pdf.test.po.PDFDocument;
import org.xwiki.export.pdf.test.po.PDFExportOptionsModal;
import org.xwiki.export.pdf.test.po.PDFImage;
import org.xwiki.flamingo.skin.test.po.ExportModal;
import org.xwiki.flamingo.skin.test.po.OtherFormatPane;
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
 * @since 14.5RC1
 */
@UITest
class PDFExportIT
{
    @Test
    void exportAsPDF(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration) throws Exception
    {
        setup.createUserAndLogin("John", "pass");

        PDFExportOptionsModal exportOptions =
            exportDocumentAsPDF(setup, new LocalDocumentReference(Arrays.asList("PDFExportIT", "Parent"), "WebHome"));

        try (PDFDocument pdf = exportOptions.export(getHostURL(testConfiguration))) {
            // We should have 3 pages: cover page, table of contents and the actual content.
            assertEquals(3, pdf.getNumberOfPages());

            // Verify the cover page.

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

            // Verify the table of contents page.
            // TODO

            // Verify the content page.
            // TODOo
        }
    }

    private URL getHostURL(TestConfiguration testConfiguration) throws Exception
    {
        return new URL(String.format("http://%s:%d", testConfiguration.getServletEngine().getHostIP(),
            testConfiguration.getServletEngine().getPort()));
    }

    private PDFExportOptionsModal exportDocumentAsPDF(TestUtils setup, LocalDocumentReference documentReference)
    {
        ViewPage viewPage = setup.gotoPage(documentReference);

        // The export modal is present but hidden on page load. We instantiate the page object before opening the modal
        // in order to prevent the fade effect (see BaseModal).
        new ExportModal();
        viewPage.clickMoreActionsSubMenuEntry("tmExport");

        new OtherFormatPane().clickExportButton("Export as PDF");

        return new PDFExportOptionsModal();
    }
}
