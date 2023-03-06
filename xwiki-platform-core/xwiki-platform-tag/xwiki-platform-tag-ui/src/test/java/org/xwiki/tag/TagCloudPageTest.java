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

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.internal.HiddenDocumentFilter;
import org.xwiki.query.internal.UniqueDocumentFilter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.wikimacro.internal.WikiMacroFactoryComponentClass;
import org.xwiki.tag.internal.selector.DefaultTagsSelector;
import org.xwiki.tag.internal.selector.ExhaustiveCheckTagsSelector;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.user.DefaultUserComponentList;
import org.xwiki.user.internal.DefaultUserPropertiesResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.tag.TagPlugin;
import com.xpn.xwiki.plugin.tag.TagQueryUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    DefaultUserPropertiesResolver.class
})
@DefaultUserComponentList
@XWikiSyntax21ComponentList
@HTML50ComponentList
@WikiMacroFactoryComponentClass
class TagCloudPageTest extends PageTest
{
    private final Query query = mock(Query.class);

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
}
