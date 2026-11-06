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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Page Test for the {@code rating} custom displayer of {@code ExtensionCode.ExtensionClass}.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    TemplateMacro.class,
    TestNoScriptMacro.class
})
class ExtensionRatingDisplayerPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final DocumentReference EXTENSION_CLASS =
        new DocumentReference(WIKI_NAME, "ExtensionCode", "ExtensionClass");

    private static final DocumentReference AVERAGE_RATINGS_CLASS =
        new DocumentReference(WIKI_NAME, "XWiki", "AverageRatingsClass");

    // A page name with characters that have a meaning in HTML and in wiki syntax.
    private static final String PAGE_NAME = "Ext\"'<em>{{/html}}{{noscript /}}{{html}}<strong>";

    private XWikiDocument extensionDocument;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.initializeMandatoryDocuments(this.context);
        loadPage(EXTENSION_CLASS);
        this.extensionDocument =
            this.xwiki.getDocument(new DocumentReference(WIKI_NAME, "Test", PAGE_NAME), this.context);
        this.extensionDocument.newXObject(EXTENSION_CLASS, this.context);
        BaseObject averageRating = this.extensionDocument.newXObject(AVERAGE_RATINGS_CLASS, this.context);
        averageRating.setFloatValue("averagevote", 4);
        averageRating.setIntValue("nbvotes", 2);
        this.xwiki.saveDocument(this.extensionDocument, this.context);
        this.context.setDoc(this.extensionDocument);
    }

    @Test
    void displayEscapesTheDocumentName() throws Exception
    {
        // The noscript macro logs an error, failing the test, if it is executed.
        Document result = Jsoup.parse(this.extensionDocument.display("rating", "view", this.context));

        Element container = result.selectFirst(".rating-container");
        assertEquals("rating_Test." + PAGE_NAME, container.id());
        assertTrue(result.select("em, strong").isEmpty());
    }
}
