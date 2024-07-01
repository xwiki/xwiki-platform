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

import java.util.concurrent.Callable;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.evaluation.internal.DefaultObjectEvaluator;
import org.xwiki.evaluation.internal.VelocityObjectPropertyEvaluator;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.search.internal.SearchSuggestSourceObjectEvaluator;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    DefaultObjectEvaluator.class,
    VelocityObjectPropertyEvaluator.class,
    SearchSuggestSourceObjectEvaluator.class,
    TestNoScriptMacro.class,
})
public class SearchSuggestConfigSheetPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final DocumentReference SEARCH_SUGGEST_CONFIG_SHEET =
        new DocumentReference(WIKI_NAME, "XWiki", "SearchSuggestConfigSheet");

    private static final DocumentReference SEARCH_SUGGEST_SOURCE_CLASS =
        new DocumentReference(WIKI_NAME, "XWiki", "SearchSuggestSourceClass");

    private static final DocumentReference TEST_PAGE =
        new DocumentReference(WIKI_NAME, "Test", "TestDocument");

    private static final DocumentReference AUTHOR_REFERENCE =
        new DocumentReference(WIKI_NAME, "XWiki", "TestUser");

    @MockComponent
    private AuthorizationManager authorizationManager;

    private AuthorExecutor authorExecutor;

    private VelocityEngine velocityEngine;

    private XWikiDocument testPageDocument;

    private XWikiDocument searchSuggestConfigSheetDocument;

    private String testString;

    private String evaluatedTestString;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.initializeMandatoryDocuments(this.context);
        loadPage(SEARCH_SUGGEST_SOURCE_CLASS);
        loadPage(SEARCH_SUGGEST_CONFIG_SHEET);

        this.testString = "$doc.getDocumentReference().getName(){{/html}}{{noscript /}}";
        this.evaluatedTestString = TEST_PAGE.getName() + "{{/html}}{{noscript /}}";

        this.searchSuggestConfigSheetDocument = this.xwiki.getDocument(SEARCH_SUGGEST_CONFIG_SHEET, this.context);

        this.testPageDocument = this.xwiki.getDocument(TEST_PAGE, this.context);
        this.testPageDocument.setTitle("Test Search Suggestions");
        this.testPageDocument.setAuthorReference(AUTHOR_REFERENCE);
        BaseObject searchSuggestSourceObject =
            this.testPageDocument.newXObject(SEARCH_SUGGEST_SOURCE_CLASS, this.context);
        searchSuggestSourceObject.setStringValue("name", this.testString);
        searchSuggestSourceObject.setStringValue("icon", this.testString);
        searchSuggestSourceObject.setStringValue("resultsNumber", this.testString);
        searchSuggestSourceObject.setStringValue("engine", this.testString);
        this.xwiki.saveDocument(this.testPageDocument, this.context);

        this.authorExecutor = this.componentManager.registerMockComponent(AuthorExecutor.class, true);

        // Spy Velocity Engine.
        VelocityManager velocityManager = this.componentManager.getInstance(VelocityManager.class);
        this.velocityEngine = velocityManager.getVelocityEngine();
        this.velocityEngine = spy(this.velocityEngine);
        velocityManager = spy(velocityManager);
        this.componentManager.registerComponent(VelocityManager.class, velocityManager);
        when(velocityManager.getVelocityEngine()).thenReturn(this.velocityEngine);

        when(this.authorExecutor.call(any(), any(), any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            return callable.call();
        });
    }

    @Test
    void displaySourceWithoutScriptRights() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.SCRIPT, AUTHOR_REFERENCE, TEST_PAGE)).thenReturn(false);

        this.context.setDoc(this.testPageDocument);
        Document result = renderHTMLPage(this.searchSuggestConfigSheetDocument);

        verify(this.authorizationManager).hasAccess(Right.SCRIPT, AUTHOR_REFERENCE, TEST_PAGE);
        verify(this.velocityEngine, never()).evaluate(any(), any(), any(), eq(this.testString));

        Element presentationLink =
            result.getElementsByAttributeValue("role", "presentation").get(0).getElementsByTag("a").get(0);
        // Escaping tests only.
        assertEquals("#" + this.testString + "SearchSuggestSources", presentationLink.attr("href"));
        assertEquals(this.testString, presentationLink.text());
        assertEquals(this.testString + "SearchSuggestSources", presentationLink.attr("aria-controls"));
        assertEquals(this.testString + "SearchSuggestSources", result.getElementsByClass("tab-pane").get(0).attr("id"));
        assertEquals(this.testString, result.getElementsByClass("limit").text());

        // These should not be evaluated.
        assertEquals(this.testString, result.getElementsByClass("icon").get(0).attr("src"));
        assertEquals(this.testString, result.getElementsByClass("name").text());
    }

    @Test
    void displaySourceWithScriptRights() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.SCRIPT, AUTHOR_REFERENCE, TEST_PAGE)).thenReturn(true);

        this.context.setDoc(this.testPageDocument);
        Document result = renderHTMLPage(this.searchSuggestConfigSheetDocument);

        verify(this.authorizationManager).hasAccess(Right.SCRIPT, AUTHOR_REFERENCE, TEST_PAGE);
        verify(this.authorExecutor).call(any(), eq(AUTHOR_REFERENCE), eq(TEST_PAGE));
        verify(this.velocityEngine, times(2)).evaluate(any(), any(), any(), eq(this.testString));

        Element presentationLink =
            result.getElementsByAttributeValue("role", "presentation").get(0).getElementsByTag("a").get(0);
        // Escaping tests only.
        assertEquals("#" + this.testString + "SearchSuggestSources", presentationLink.attr("href"));
        assertEquals(this.testString, presentationLink.text());
        assertEquals(this.testString + "SearchSuggestSources", presentationLink.attr("aria-controls"));
        assertEquals(this.testString + "SearchSuggestSources", result.getElementsByClass("tab-pane").get(0).attr("id"));
        assertEquals(this.testString, result.getElementsByClass("limit").text());

        // These should be evaluated.
        assertEquals(this.evaluatedTestString, result.getElementsByClass("icon").get(0).attr("src"));
        assertEquals(this.evaluatedTestString, result.getElementsByClass("name").text());
    }
}
