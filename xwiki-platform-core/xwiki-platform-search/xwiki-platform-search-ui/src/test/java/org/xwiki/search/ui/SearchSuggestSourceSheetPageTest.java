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
package org.xwiki.search.ui;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.uiextension.script.UIExtensionScriptServiceComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Page test for {@code XWiki.SearchSuggestSourceSheet}.
 *
 * @version $Id$
 */
@ComponentList({
    TestNoScriptMacro.class
})
@UIExtensionScriptServiceComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
class SearchSuggestSourceSheetPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final String XWIKI_SPACE = "XWiki";

    private static final DocumentReference SEARCH_SUGGEST_SOURCE_SHEET =
        new DocumentReference(WIKI_NAME, XWIKI_SPACE, "SearchSuggestSourceSheet");

    private static final DocumentReference SEARCH_SUGGEST_SOURCE_CLASS =
        new DocumentReference(WIKI_NAME, XWIKI_SPACE, "SearchSuggestSourceClass");

    private XWikiDocument searchSuggestSourceSheetDocument;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.initializeMandatoryDocuments(this.context);

        this.loadPage(SEARCH_SUGGEST_SOURCE_CLASS);
        this.searchSuggestSourceSheetDocument = this.loadPage(SEARCH_SUGGEST_SOURCE_SHEET);
    }

    @Test
    void checkPropertiesEscaping() throws Exception
    {
        // Create an instance of XWiki.SearchSuggestSourceClass with properties that require escaping.
        String[] properties = new String[]{"name", "engine", "url", "query", "resultsNumber", "icon"};
        String unescapedProperty = "{{/html}}}}}{{noscript}}";
        BaseObject searchSuggestSource =
            this.searchSuggestSourceSheetDocument.newXObject(SEARCH_SUGGEST_SOURCE_CLASS, this.context);
        for (String property : properties) {
            searchSuggestSource.set(property, unescapedProperty, this.context);
        }
        this.xwiki.saveDocument(this.searchSuggestSourceSheetDocument, this.context);

        this.context.setDoc(this.searchSuggestSourceSheetDocument);
        Document document = renderHTMLPage(this.searchSuggestSourceSheetDocument);
        Elements labels = document.getElementsByTag("label");
        Elements values = document.getElementsByTag("dd");

        // Check that the value of the property has not been evaluated for each label that we know of.
        for (String property : properties) {
            int iLabel = -1;
            for (int i = 0; i < labels.size(); i++) {
                if (labels.get(i).text().replaceAll("^.*_", "").equals(property)) {
                    iLabel = i;
                    break;
                }
            }
            assertTrue(iLabel >= 0, "Could not find property " + property + " in rendered document.");
            assertEquals(unescapedProperty, values.get(iLabel).text());
        }
    }
}
