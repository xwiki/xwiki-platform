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
package org.xwiki.panels;

import org.jsoup.nodes.Document;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.context.internal.concurrent.DefaultContextStoreManager;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.panels.internal.PanelClassDocumentInitializer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Page test for {@code Panels.NewPage}.
 *
 * @version $Id$
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@ComponentList({
    PanelClassDocumentInitializer.class,
    DefaultContextStoreManager.class,
    ModelScriptService.class,
    LocalStringEntityReferenceSerializer.class
})
class NewPagePageTest extends PageTest
{
    private static final LocalDocumentReference PANEL_CLASS = new LocalDocumentReference("Panels", "PanelClass");

    @ParameterizedTest
    @ValueSource(strings = {
        "a\"><img class=\"special\" src=\"x\"/>",
        "b{{/html}}{{html}}<img class=\"special\" src=\"x\"/>{{/html}}"
    })
    void renderWithSpecialCharactersInTheCurrentDocumentReference(String name) throws Exception
    {
        this.xwiki.initializeMandatoryDocuments(this.context);
        XWikiDocument panel = loadPage(new DocumentReference("xwiki", "Panels", "NewPage"));

        // The panel is rendered in the context of the current document, so we render its content as the content of a
        // document whose space and name contain special characters.
        XWikiDocument currentDocument = new XWikiDocument(new DocumentReference("xwiki", name, name));
        currentDocument.setSyntax(Syntax.XWIKI_2_1);
        currentDocument.setContent("{{velocity}}#set ($hasEdit = true){{/velocity}}"
            + panel.getXObject(PANEL_CLASS).getLargeStringValue("content"));
        this.context.setDoc(currentDocument);

        Document result = renderHTMLPage(currentDocument);

        assertTrue(result.select(".special").isEmpty());
        assertEquals(name + "." + name, result.selectFirst("input[name=parent]").attr("value"));
        assertEquals(name, result.selectFirst("input[name=spaceReference]").attr("value"));
    }
}
