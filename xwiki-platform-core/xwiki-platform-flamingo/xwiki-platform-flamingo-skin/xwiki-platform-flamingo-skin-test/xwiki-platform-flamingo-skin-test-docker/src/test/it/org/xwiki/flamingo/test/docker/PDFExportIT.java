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
package org.xwiki.flamingo.test.docker;

import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.export.pdf.test.po.PDFDocument;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to the server-side PDF export (to be moved when the PDF export feature is moved out of oldcore).
 *
 * @version $Id$
 */
// Note: vnc is set to false since otherwise our test framework fails to take a video since no page has been accessed
// by the test. Since we don't need VNC, we turn it off and thus no video is taken.
@UITest(vnc = false)
class PDFExportIT
{
    private String prefix;

    @BeforeEach
    void beforeEach(TestConfiguration testConfiguration)
    {
        String host = testConfiguration.getServletEngine().getIP();
        int port = testConfiguration.getServletEngine().getPort();
        this.prefix = String.format("http://%s:%s/", host, port);
    }

    /**
     * Verify that the PDF export feature works on a single simple page by downloading the PDF and parsing it using
     * PDFBox.
     *
     * @see "XWIKI-7048: PDF export templates can display properties of other objects if the XWiki.PDFClass object is
     *      missing"
     */
    @Test
    void exportSingleSimplePageAsPDF() throws Exception
    {
        // We're using Dashboard.WebHome page because it has objects of type XWiki.GadgetClass and they have a title
        // property which was mistaken with the title property of XWiki.PDFClass before XWIKI-7048 was fixed. The gadget
        // title contains Velocity code that isn't wrapped in a Velocity macro so it is printed as is if not rendered in
        // the right context.
        URL pdfURL = new URL(createURL("xwiki/bin/export/Dashboard/WebHome?format=pdf"));
        try (PDFDocument document = new PDFDocument(pdfURL)) {
            String text = document.getText();
            // Note: This is the title of the Pages gadget when it's working
            assertTrue(text.contains("Pages"), "Invalid content");
            // Note: This is the title of the Pages gadget before XWIKI-7048 was fixed
            assertFalse(text.contains("$services.localization.render("), "Invalid content");
        }
    }

    /**
     * Verify that we can export content having links to attachments.
     *
     * @see "XWIKI-8978: PDF Export does not handle XWiki links to attached files properly"
     */
    @Test
    void exportContentWithAttachmentLink() throws Exception
    {
        URL pdfURL = new URL(createURL("xwiki/bin/export/Sandbox/WebHome?format=pdf"));
        try (PDFDocument document = new PDFDocument(pdfURL)) {
            Map<String, String> links = document.getLinks();
            assertTrue(links.containsKey("XWikiLogo.png"));
            assertEquals(String.format("%sxwiki/bin/download/Sandbox/WebHome/XWikiLogo.png?rev=1.1", this.prefix),
                links.get("XWikiLogo.png"));

            // The PDF document should contain the XWikiLogo.png image embedded in the Sandbox home page.
            // But it also contains the icons for the different boxes
            assertEquals(4, document.getImages().size());
        }
    }

    /**
     * Verify the PDF export with table of contents.
     *
     * @see "XWIKI-9370: PDF Export doesn't list the Table of Contents under certain circumstances"
     */
    @Test
    void exportTableOfContents() throws Exception
    {
        URL pdfURL =
            new URL(createURL("xwiki/bin/export/Sandbox/WebHome?format=pdf&pdftoc=1&attachments=1&pdfcover=0"));
        try (PDFDocument document = new PDFDocument(pdfURL)) {
            Map<String, String> links = document.getLinksFromPage(0);
            // Make sure we have a Table of Contents.
            assertTrue(links.containsKey("Mixed list"));
            // Make sure the Table of Contents links point to their corresponding heading.
            for (Map.Entry<String, String> entry : links.entrySet()) {
                assertTrue(entry.getValue().contains(entry.getKey()));
            }
        }
    }

    private String createURL(String suffix)
    {
        return String.format("%s%s", this.prefix, suffix);
    }
}
