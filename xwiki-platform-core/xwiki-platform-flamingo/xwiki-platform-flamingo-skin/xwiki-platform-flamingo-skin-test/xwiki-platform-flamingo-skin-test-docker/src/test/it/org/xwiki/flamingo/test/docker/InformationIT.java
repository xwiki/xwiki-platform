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

import java.util.Arrays;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.DocumentSyntaxPicker;
import org.xwiki.test.ui.po.DocumentSyntaxPicker.SyntaxConversionConfirmationModal;
import org.xwiki.test.ui.po.DocumentSyntaxPropertyPane;
import org.xwiki.test.ui.po.InformationPane;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the document information tab at the bottom of the page.
 *
 * @version $Id$
 * @since 12.6.3
 * @since 12.9RC1
 */
@UITest
public class InformationIT
{
    /**
     * Change the document syntax from the Information tab while viewing the document.
     */
    @Test
    @Order(1)
    void changeDocumentSyntax(TestUtils setup, TestReference testReference) throws Exception
    {
        // Enable the plain syntax.
        setup.addObject("Rendering", "RenderingConfig", "Rendering.RenderingConfigClass", "disabledSyntaxes",
            "xdom+xml/current,xhtml/1.0,xwiki/2.0,xhtml/5,html/5.0");
        setup.deletePage(testReference);

        try {
            ViewPage viewPage = setup.createPage(testReference, "one **two** three", "");
            assertEquals("one two three", viewPage.getContent());
            assertTrue(viewPage.contentContainsElement(By.xpath(".//strong[. = 'two']")));

            InformationPane informationPane = viewPage.openInformationDocExtraPane();
            assertEquals("XWiki 2.1", informationPane.getSyntax());

            DocumentSyntaxPropertyPane syntaxPane = informationPane.editSyntax();
            DocumentSyntaxPicker syntaxPicker = syntaxPane.getSyntaxPicker();

            assertEquals(Arrays.asList("plain/1.0", "xwiki/2.1"), syntaxPicker.getAvailableSyntaxes());
            assertEquals("xwiki/2.1", syntaxPicker.getSelectedSyntax());

            SyntaxConversionConfirmationModal confirmationModal = syntaxPicker.selectSyntaxById("plain/1.0");
            confirmationModal.confirmSyntaxConversion();

            // Verify that the document content was refreshed using the new syntax.
            assertEquals("one two three", viewPage.getContent());
            assertFalse(viewPage.contentContainsElement(By.xpath(".//strong[. = 'two']")));

            // Cancel the syntax change and verify that the content is refreshed.
            syntaxPane.clickCancel();
            assertEquals("XWiki 2.1", syntaxPane.getDisplayValue());
            assertEquals("one two three", viewPage.getContent());
            assertTrue(viewPage.contentContainsElement(By.xpath(".//strong[. = 'two']")));

            // Change the syntax again, this time without doing the conversion.
            syntaxPicker = syntaxPane.clickEdit().getSyntaxPicker();
            confirmationModal = syntaxPicker.selectSyntaxById("plain/1.0");
            confirmationModal.rejectSyntaxConversion();

            // Verify that the document content was refreshed using the new syntax.
            assertEquals("one **two** three", viewPage.getContent());

            syntaxPane.clickSave();
            assertEquals("Plain 1.0", syntaxPane.getDisplayValue());

            // Verify that the syntax change has been persisted.
            viewPage = setup.gotoPage(testReference);
            assertEquals("one **two** three", viewPage.getContent());

            informationPane = viewPage.openInformationDocExtraPane();
            assertEquals("Plain 1.0", informationPane.getSyntax());
        } finally {
            // Disable back the plain syntax.
            setup.deleteObject("Rendering", "RenderingConfig", "Rendering.RenderingConfigClass", 0);
        }
    }
}
