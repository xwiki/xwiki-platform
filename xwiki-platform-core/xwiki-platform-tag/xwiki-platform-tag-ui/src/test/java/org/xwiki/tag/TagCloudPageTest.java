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
package org.xwiki.tag;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Named;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.internal.HiddenDocumentFilter;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.internal.UniqueDocumentFilter;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.rendering.internal.macro.message.WarningMessageMacro;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.wikimacro.internal.WikiMacroFactoryComponentClass;
import org.xwiki.script.service.ScriptService;
import org.xwiki.tag.internal.selector.DefaultTagsSelector;
import org.xwiki.tag.internal.selector.ExhaustiveCheckTagsSelector;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.user.DefaultUserComponentList;
import org.xwiki.user.internal.DefaultUserPropertiesResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.tag.TagPlugin;
import com.xpn.xwiki.plugin.tag.TagQueryUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.test.page.WikiMacroSetup.loadWikiMacro;

/**
 * Test of {@code XWiki.TagCloud}.
 *
 * @version $Id$
 * @since 15.2RC1
 */
@ComponentList({
    DefaultTagsSelector.class,
    ExhaustiveCheckTagsSelector.class,
    HiddenDocumentFilter.class,
    UniqueDocumentFilter.class,
    DefaultUserPropertiesResolver.class,
    ErrorMessageMacro.class,
    WarningMessageMacro.class,
    TranslationMacro.class
})
@DefaultUserComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@XWikiSyntax21ComponentList
@HTML50ComponentList
@WikiMacroFactoryComponentClass
class TagCloudPageTest extends PageTest
{
    private final Query query = mock(Query.class);

    private final ScriptQuery spaceQuery = mock(ScriptQuery.class, RETURNS_SELF);

    @MockComponent(classToMock = QueryManagerScriptService.class)
    @Named("query")
    private ScriptService queryService;

    @BeforeEach
    void setUp() throws Exception
    {
        // For the TagQueryUtils.tagsSelector by reflection. Otherwise, an older version of the component is kept for 
        // all test methods. This is notably an issue because the wrong query manager is used, with invalid mocks.
        Field tagsSelector = TagQueryUtils.class.getDeclaredField("tagsSelector");
        tagsSelector.setAccessible(true);
        tagsSelector.set(null, null);

        when(this.oldcore.getQueryManager().createQuery(any(String.class), any(String.class))).thenReturn(this.query);
        when(this.query.addFilter(any())).thenReturn(this.query);

        // The locations exist, unless a test says otherwise.
        when(((QueryManagerScriptService) this.queryService).hql(anyString())).thenReturn(this.spaceQuery);
        when(this.spaceQuery.execute()).thenReturn(List.of("Space"));

        loadWikiMacro(this, this.componentManager, new DocumentReference("xwiki", "XWiki", "TagCloud"));

        this.oldcore.getSpyXWiki().getPluginManager().addPlugin("tag", TagPlugin.class.getName(), this.context);
    }

    @Test
    void noTags() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of());
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{tagcloud/}}");
        this.xwiki.saveDocument(xwikiDocument, this.context);

        Document document = renderHTMLPage(xwikiDocument);
        assertEquals("xe.tag.notags", document.selectFirst("p.noitems").text());
    }

    @Test
    void withTags() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of(
            new Object[] { "xwiki:Space.TaggedPage1", "tag1" },
            new Object[] { "xwiki:Space.TaggedPage1", "tag2" },
            new Object[] { "xwiki:Space.TaggedPage2", "tag2" }
        ));
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{tagcloud/}}");
        this.xwiki.saveDocument(xwikiDocument, this.context);

        Document document = renderHTMLPage(xwikiDocument);

        Elements lis = document.select("ol.tagCloud li");
        assertEquals(2, lis.size());
        Element li0 = lis.get(0);
        Element a0 = li0.selectFirst("a");
        assertEquals("notPopular", li0.className());
        assertEquals("/xwiki/bin/view/Main/Tags?do=viewTag&tag=tag1", a0.attr("href"));
        assertEquals("xe.tag.tooltip [1]", a0.attr("title"));
        assertEquals("tag1", a0.text());

        Element li1 = lis.get(1);
        Element a1 = li1.selectFirst("a");
        assertEquals("ultraPopular", li1.className());
        assertEquals("/xwiki/bin/view/Main/Tags?do=viewTag&tag=tag2", a1.attr("href"));
        assertEquals("xe.tag.tooltip [2]", a1.attr("title"));
        assertEquals("tag2", a1.text());
    }

    public static Stream<Arguments> withLimitSource()
    {
        return Stream.of(
            Arguments.of(-1, 0, null, null),
            Arguments.of(0, 0, null, null),
            Arguments.of(1, 1, null, "ultraPopular"),
            Arguments.of(2, 2, "notPopular", "ultraPopular"),
            Arguments.of(3, 2, "notPopular", "ultraPopular")
        );
    }

    @ParameterizedTest
    @MethodSource("withLimitSource")
    void withLimit(int limit, int expectedCount, String popularityTag1, String popularityTag2) throws Exception
    {
        when(this.query.execute()).thenReturn(List.of(
            new Object[] { "xwiki:Space.TaggedPage1", "tag1" },
            new Object[] { "xwiki:Space.TaggedPage1", "tag2" },
            new Object[] { "xwiki:Space.TaggedPage2", "tag2" }
        ));
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent(String.format("{{tagcloud limit=\"%d\"/}}", limit));
        this.xwiki.saveDocument(xwikiDocument, this.context);

        Document document = renderHTMLPage(xwikiDocument);

        Elements lis = document.select("ol.tagCloud li");
        assertEquals(expectedCount, lis.size());
        if (expectedCount > 0) {
            Element li1 = lis.get(Math.min(lis.size() - 1, 1));
            Element a1 = li1.selectFirst("a");
            assertEquals(popularityTag2, li1.className());
            assertEquals("/xwiki/bin/view/Main/Tags?do=viewTag&tag=tag2", a1.attr("href"));
            assertEquals("xe.tag.tooltip [2]", a1.attr("title"));
            assertEquals("tag2", a1.text());
        }
        if (expectedCount > 1) {
            Element li0 = lis.get(0);
            Element a0 = li0.selectFirst("a");
            assertEquals(popularityTag1, li0.className());
            assertEquals("/xwiki/bin/view/Main/Tags?do=viewTag&tag=tag1", a0.attr("href"));
            assertEquals("xe.tag.tooltip [1]", a0.attr("title"));
            assertEquals("tag1", a0.text());
        }
    }

    @Test
    void withSpaceAndNoTags() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of());
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{tagcloud space=\"My_Space\"/}}");
        this.xwiki.saveDocument(xwikiDocument, this.context);

        Document document = renderHTMLPage(xwikiDocument);

        assertEquals("xe.tag.notagsforspace", document.selectFirst("p.noitems").text());
        verify(this.query).bindValues(List.of("My_Space", "My\\_Space.%"));
    }

    @Test
    void withSpaces() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of(
            new Object[] { "xwiki:Space1.TaggedPage1", "tag1" },
            new Object[] { "xwiki:Space2.TaggedPage2", "tag1" }
        ));
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{tagcloud spaces=\"'Space1','Space2'\"/}}");
        this.xwiki.saveDocument(xwikiDocument, this.context);

        Document document = renderHTMLPage(xwikiDocument);

        Elements lis = document.select("ol.tagCloud li");
        assertEquals(1, lis.size());
        assertEquals("tag1", lis.get(0).selectFirst("a").text());
        verify(this.query).bindValues(List.of("Space1", "Space1.%", "Space2", "Space2.%"));
    }

    @Test
    void withSpaceAndSpaces() throws Exception
    {
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{tagcloud space=\"Space1\" spaces=\"'Space2'\"/}}");
        this.xwiki.saveDocument(xwikiDocument, this.context);

        Document document = renderHTMLPage(xwikiDocument);

        assertEquals("xe.tag.paramerror", document.selectFirst(".errormessage").text());
        verify(this.query, never()).execute();
    }

    @Test
    void withSpacesWithoutQuotes() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of(
            new Object[] { "xwiki:Sandbox.TaggedPage1", "tag1" },
            new Object[] { "xwiki:My Space.TaggedPage2", "tag1" }
        ));
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{tagcloud spaces=\"Sandbox,My Space\"/}}");
        this.xwiki.saveDocument(xwikiDocument, this.context);

        Document document = renderHTMLPage(xwikiDocument);

        Elements lis = document.select("ol.tagCloud li");
        assertEquals(1, lis.size());
        assertEquals("tag1", lis.get(0).selectFirst("a").text());
        verify(this.query).bindValues(List.of("Sandbox", "Sandbox.%", "My Space", "My Space.%"));
    }

    @Test
    void withSpacesWithDoubledQuote() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of());
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{tagcloud spaces=\"'Apo''stroph','My, Space'\"/}}");
        this.xwiki.saveDocument(xwikiDocument, this.context);

        renderHTMLPage(xwikiDocument);

        verify(this.query).bindValues(List.of("Apo'stroph", "Apo'stroph.%", "My, Space", "My, Space.%"));
    }

    @Test
    void withSpacesMixingQuotedAndPlainLocations() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of());
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{tagcloud spaces=\"'Space1','My, Space',Sandbox, O'Brien\"/}}");
        this.xwiki.saveDocument(xwikiDocument, this.context);

        renderHTMLPage(xwikiDocument);

        verify(this.query).bindValues(List.of("Space1", "Space1.%", "My, Space", "My, Space.%", "Sandbox",
            "Sandbox.%", "O'Brien", "O'Brien.%"));
    }

    public static Stream<String> withInvalidSpacesSource()
    {
        return Stream.of("'Sandbox", "'Sandbox' Space", "Sandbox,,Space", "Sandbox,", "''");
    }

    @ParameterizedTest
    @MethodSource("withInvalidSpacesSource")
    void withInvalidSpaces(String spaces) throws Exception
    {
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent(String.format("{{tagcloud spaces=\"%s\"/}}", spaces));
        this.xwiki.saveDocument(xwikiDocument, this.context);

        Document document = renderHTMLPage(xwikiDocument);

        assertEquals(String.format("tag.cloud.invalidLocations [%s]", spaces),
            document.selectFirst(".errormessage").text());
        verify(this.query, never()).execute();
    }

    @Test
    void withSpacesAndNoTags() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of());
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{tagcloud spaces=\"My_Space\"/}}");
        this.xwiki.saveDocument(xwikiDocument, this.context);

        Document document = renderHTMLPage(xwikiDocument);

        assertEquals("xe.tag.notagsforspace", document.selectFirst("p.noitems").text());
        assertNull(document.selectFirst(".warningmessage"));
        verify(this.query).bindValues(List.of("My_Space", "My\\_Space.%"));
    }

    @Test
    void withMissingLocation() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of());
        when(this.spaceQuery.execute()).thenReturn(List.of());
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{tagcloud space=\"No.Such\"/}}");
        this.xwiki.saveDocument(xwikiDocument, this.context);

        Document document = renderHTMLPage(xwikiDocument);

        assertEquals("tag.cloud.locationNotFound [No.Such]", document.selectFirst(".warningmessage").text());
        verify(this.spaceQuery).bindValue("space", "No.Such");
    }

    @Test
    void withSpacesAndOneMissingLocation() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of(
            new Object[] { "xwiki:Sandbox.TaggedPage1", "tag1" },
            new Object[] { "xwiki:Sandbox.TaggedPage2", "tag1" }
        ));
        ScriptQuery missingSpaceQuery = mock(ScriptQuery.class, RETURNS_SELF);
        when(missingSpaceQuery.execute()).thenReturn(List.of());
        when(this.spaceQuery.bindValue("space", "No**Such**")).thenReturn(missingSpaceQuery);
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{tagcloud spaces=\"Sandbox,No**Such**\"/}}");
        this.xwiki.saveDocument(xwikiDocument, this.context);

        Document document = renderHTMLPage(xwikiDocument);

        Elements warnings = document.select(".warningmessage");
        assertEquals(1, warnings.size());
        // The location is displayed as typed, without being parsed as wiki syntax.
        assertEquals("tag.cloud.locationNotFound [No**Such**]", warnings.get(0).text());
        assertNull(warnings.get(0).selectFirst("strong"));
        // The warning is a block of its own, otherwise the HTML of the tag cloud that follows would be inline too.
        assertEquals("div", warnings.get(0).tagName());
        assertEquals(List.of("tag1"), document.select("ol.tagCloud li a").eachText());
        verify(this.spaceQuery).bindValue("space", "Sandbox");
    }

    @Test
    void includedWithTagCloudSpace() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of());
        XWikiDocument tagCloudDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "TagCloud"), this.context);
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Page"), this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{velocity}}#set ($tagCloudSpace = \"Apo'stroph, Space\"){{/velocity}}\n\n"
            + tagCloudDocument.getContent());
        this.xwiki.saveDocument(xwikiDocument, this.context);

        renderHTMLPage(xwikiDocument);

        verify(this.query).bindValues(List.of("Apo'stroph, Space", "Apo'stroph, Space.%"));
    }
}
