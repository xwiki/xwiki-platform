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
package org.xwiki.administration;

import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Page test of {@code XWiki.ConfigurableClass}.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@ComponentList({
    TestNoScriptMacro.class,
    TranslationMacro.class,
    ErrorMessageMacro.class,
    TemplateScriptService.class
})
class ConfigurableClassPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final String SPACE_NAME = "XWiki";

    private static final DocumentReference CONFIGURABLE_CLASS =
        new DocumentReference(WIKI_NAME, SPACE_NAME, "ConfigurableClass");

    private static final DocumentReference CONFIGURABLE_CLASS_MACROS =
        new DocumentReference(WIKI_NAME, SPACE_NAME, "ConfigurableClassMacros");

    private static final DocumentReference MY_SECTION =
        new DocumentReference(WIKI_NAME, SPACE_NAME, "]]{{noscript /}}");

    private static final String MY_SECTION_SERIALIZED = "XWiki.]]{{noscript /}}";

    @Mock
    private QueryManagerScriptService queryService;

    @Mock
    private ScriptQuery query;

    @BeforeEach
    void setUp() throws Exception
    {
        // Load the macros page so it can be included.
        loadPage(CONFIGURABLE_CLASS_MACROS);

        // Mock the query.
        this.oldcore.getMocker().registerComponent(ScriptService.class, "query", this.queryService);
        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.addFilter(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.bindValues(any(List.class))).thenReturn(this.query);
    }

    @Test
    void escapeHeadingForError() throws Exception
    {
        this.request.put("section", "other");
        when(this.query.execute()).thenReturn(List.of(MY_SECTION_SERIALIZED)).thenReturn(List.of());

        XWikiDocument mySectionDoc = new XWikiDocument(MY_SECTION);
        this.xwiki.saveDocument(mySectionDoc, this.context);

        Document htmlPage = renderHTMLPage(CONFIGURABLE_CLASS);
        assertEquals(String.format("admin.customize %s:", MY_SECTION_SERIALIZED),
            htmlPage.selectFirst("h1").text());
    }
}
