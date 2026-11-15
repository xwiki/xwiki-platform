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
package org.xwiki.repository.server.ui;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Page Test for {@code ExtensionCode.ExtensionAuthorsDisplayer}.
 *
 * @version $Id$
 */
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    ModelScriptService.class,
    TestNoScriptMacro.class
})
class ExtensionAuthorsDisplayerPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final DocumentReference EXTENSION_AUTHORS_DISPLAYER =
        new DocumentReference(WIKI_NAME, "ExtensionCode", "ExtensionAuthorsDisplayer");

    private static final DocumentReference EXTENSION_CLASS =
        new DocumentReference(WIKI_NAME, "ExtensionCode", "ExtensionClass");

    private static final DocumentReference USERS_CLASS = new DocumentReference(WIKI_NAME, "XWiki", "XWikiUsers");

    private static final DocumentReference TEST_PAGE = new DocumentReference(WIKI_NAME, "Test", "TestDocument");

    private static final String INJECTION = "]] {{noscript /}} [[";

    private XWikiDocument displayerDocument;

    private XWikiDocument testPageDocument;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.initializeMandatoryDocuments(this.context);
        loadPage(EXTENSION_CLASS);
        this.displayerDocument = loadPage(EXTENSION_AUTHORS_DISPLAYER);
        this.testPageDocument = this.xwiki.getDocument(TEST_PAGE, this.context);
    }

    @Test
    void displayEscapesAuthors() throws Exception
    {
        // A user whose display name contains wiki syntax.
        XWikiDocument userDocument =
            this.xwiki.getDocument(new DocumentReference(WIKI_NAME, "XWiki", "Alice"), this.context);
        BaseObject userObject = userDocument.newXObject(USERS_CLASS, this.context);
        userObject.setStringValue("first_name", "Alice" + INJECTION);
        this.xwiki.saveDocument(userDocument, this.context);

        // An existing page whose name contains wiki syntax.
        String injectedPageName = "Bob" + INJECTION + "z";
        XWikiDocument injectedPageDocument =
            this.xwiki.getDocument(new DocumentReference(WIKI_NAME, "XWiki", injectedPageName), this.context);
        this.xwiki.saveDocument(injectedPageDocument, this.context);

        // A page that doesn't exist, displayed as text.
        String missingAuthor = "XWiki.Missing" + INJECTION;

        BaseObject extensionObject = this.testPageDocument.newXObject(EXTENSION_CLASS, this.context);
        extensionObject.setStringListValue("authors",
            List.of("xwiki:XWiki.Alice", "XWiki." + injectedPageName, missingAuthor));
        this.context.setDoc(this.testPageDocument);

        // The noscript macro logs an error, failing the test, if any author breaks out of its link or text.
        Document document = renderHTMLPage(this.displayerDocument);

        Elements links = document.select("a");
        assertEquals(2, links.size());
        assertEquals("Alice" + INJECTION, links.get(0).text());
        assertEquals("doc:xwiki:XWiki.Alice", links.get(0).attr("href"));
        assertEquals(injectedPageName, links.get(1).text());
        assertEquals("doc:xwiki:XWiki." + injectedPageName, links.get(1).attr("href"));
        assertEquals("Alice%s, %s, %s".formatted(INJECTION, injectedPageName, missingAuthor), document.text());
    }
}
