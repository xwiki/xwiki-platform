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

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.context.internal.concurrent.DefaultContextStoreManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.panels.internal.PanelClassDocumentInitializer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Page test for {@code Panels.ClassSwitcher}.
 *
 * @version $Id$
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@ComponentList({
    PanelClassDocumentInitializer.class,
    DefaultContextStoreManager.class
})
class ClassSwitcherPageTest extends PageTest
{
    private static final LocalDocumentReference PANEL_CLASS = new LocalDocumentReference("Panels", "PanelClass");

    @ParameterizedTest
    @ValueSource(strings = {
        "a\"><img class=\"special\" src=\"x\"/>",
        "b{{/html}}{{html}}<img class=\"special\" src=\"x\"/>{{/html}}"
    })
    void renderWithSpecialCharactersInTheClassReference(String name) throws Exception
    {
        this.xwiki.initializeMandatoryDocuments(this.context);
        XWikiDocument panel = loadPage(new DocumentReference("xwiki", "Panels", "ClassSwitcher"));

        // The panel lists the classes of the wiki, one of them with special characters in its space and name.
        doReturn(List.of(name + "." + name)).when(this.xwiki).getClassList(any());
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("edit"), any(), any(), any())).thenReturn(true);

        XWikiDocument currentDocument = new XWikiDocument(new DocumentReference("xwiki", "Sandbox", "WebHome"));
        currentDocument.setSyntax(Syntax.XWIKI_2_1);
        currentDocument.setContent(panel.getXObject(PANEL_CLASS).getLargeStringValue("content"));
        this.context.setDoc(currentDocument);

        Document result = renderHTMLPage(currentDocument);

        assertTrue(result.select(".special").isEmpty());
        assertEquals(name, result.selectFirst("select#classname optgroup").attr("label"));
        Element option = result.selectFirst("select#classname optgroup option");
        assertEquals(name, option.text());
    }
}
