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
package org.xwiki.test.misc;

import java.net.URL;
import java.util.Map;

import org.xwiki.export.pdf.test.po.PDFDocument;

import junit.framework.TestCase;

public class PDFTest extends TestCase
{
    /**
     * Verify that the PDF export feature works on a single simple page by downloading the PDF and parsing it using
     * PDFBox.
     * 
     * @see "XWIKI-7048: PDF export templates can display properties of other objects if the XWiki.PDFClass object is
     *      missing"
     */
    public void testExportSingleSimplePageAsPDF() throws Exception
    {
        // We're using Dashboard.WebHome page because it has objects of type XWiki.GadgetClass and they have a title
        // property which was mistaken with the title property of XWiki.PDFClass before XWIKI-7048 was fixed. The gadget
        // title contains Velocity code that isn't wrapped in a Velocity macro so it is printed as is if not rendered in
        // the right context.
        URL pdfURL = new URL("http://localhost:8080/xwiki/bin/export/Dashboard/WebHome?format=pdf");
        try (PDFDocument document = new PDFDocument(pdfURL)) {
            String text = document.getText();
            // Note: This is the title of the Pages gadget when it's working
            assertTrue("Invalid content", text.contains("Pages"));
            // Note: This is the title of the Pages gadget before XWIKI-7048 was fixed
            assertFalse("Invalid content", text.contains("$services.localization.render("));
        }
    }

    /**
     * Verify that we can export content having links to attachments.
     * 
     * @see "XWIKI-8978: PDF Export does not handle XWiki links to attached files properly"
     */
    public void testExportContentWithAttachmentLink() throws Exception
    {
        URL pdfURL = new URL("http://localhost:8080/xwiki/bin/export/Sandbox/WebHome?format=pdf");
        try (PDFDocument document = new PDFDocument(pdfURL)) {
            Map<String, String> links = document.getLinks();
            assertTrue(links.containsKey("XWikiLogo.png"));
            assertEquals("http://localhost:8080/xwiki/bin/download/Sandbox/WebHome/XWikiLogo.png?rev=1.1",
                links.get("XWikiLogo.png"));

            // Ideally we should be asserting for a value of 1 (for the embedded XWikiLogo.png image) but it seems the
            // PDF contains 2 image objects (for some reason I don't understand ATM - they seem to be variations of the
            // same image - the logo - in color, in black and white, etc).
            assertEquals(2, document.getImages().size());
        }
    }

    /**
     * Verify the PDF export with table of contents.
     * 
     * @see "XWIKI-9370: PDF Export doesn't list the Table of Contents under certain circumstances"
     */
    public void testTableOfContents() throws Exception
    {
        URL pdfURL = new URL(
            "http://localhost:8080/xwiki/bin/export/Sandbox/WebHome?format=pdf&pdftoc=1&attachments=1&pdfcover=0");
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
}
