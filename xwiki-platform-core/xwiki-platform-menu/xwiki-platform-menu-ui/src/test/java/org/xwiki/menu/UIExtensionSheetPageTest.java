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
package org.xwiki.menu;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.uiextension.internal.UIExtensionClassDocumentInitializer;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Page test for the document {@code Menu.UIExtensionSheet}.
 *
 * @version $Id$
 */
@ComponentList({
    UIExtensionClassDocumentInitializer.class
})
@HTML50ComponentList
@XWikiSyntax21ComponentList
class UIExtensionSheetPageTest extends PageTest
{
    private static final DocumentReference SHEET_REFERENCE = new DocumentReference("xwiki", "Menu", "UIExtensionSheet");

    @Test
    void escaping() throws Exception
    {
        DocumentReference testDocumentReference = new DocumentReference("xwiki", "space", "test");
        XWikiDocument testDocument = new XWikiDocument(testDocumentReference);
        BaseObject uiExtension =
            testDocument.newXObject(UIExtensionClassDocumentInitializer.UI_EXTENSION_CLASS, this.context);
        String extensionID = "\"{{/html}}</option>";
        String contentValue = "content +" + extensionID;
        uiExtension.setLargeStringValue(UIExtensionClassDocumentInitializer.CONTENT_PROPERTY, contentValue);
        uiExtension.setStringValue(UIExtensionClassDocumentInitializer.EXTENSION_POINT_ID_PROPERTY, extensionID);
        this.xwiki.saveDocument(testDocument, this.context);

        this.context.setDoc(testDocument);
        this.context.setAction("edit");

        XWikiDocument sheet = loadPage(SHEET_REFERENCE);
        String htmlContent = sheet.getRenderedContent(this.context);
        Document renderedDocument = Jsoup.parse(htmlContent);

        Element selectElement = renderedDocument.getElementById("XWiki.UIExtensionClass_0_extensionPointId");
        assertNotNull(selectElement);
        Elements optionElement = selectElement.getElementsByAttributeValue("value", extensionID);
        assertEquals(1, optionElement.size());
        String extensionIdTitle = "menu.uix.extensionPoint.value." + extensionID;
        assertEquals(extensionIdTitle, optionElement.get(0).text());

        selectElement = renderedDocument.getElementById("XWiki.UIExtensionClass_0_content");
        assertNotNull(selectElement);
        optionElement = selectElement.getElementsByAttributeValue("value", contentValue);
        assertEquals(1, optionElement.size());
        assertEquals(extensionIdTitle, optionElement.get(0).text());
    }
}
