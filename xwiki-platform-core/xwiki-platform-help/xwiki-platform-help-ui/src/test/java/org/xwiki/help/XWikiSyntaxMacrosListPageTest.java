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
package org.xwiki.help;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.context.internal.concurrent.DefaultContextStoreManager;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.renderer.xhtml.link.DocumentXHTMLLinkTypeRenderer;
import org.xwiki.rendering.internal.resolver.DefaultResourceReferenceEntityReferenceResolver;
import org.xwiki.rendering.internal.resolver.DocumentResourceReferenceEntityReferenceResolver;
import org.xwiki.rendering.internal.wiki.XWikiWikiModel;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.wikimacro.internal.DefaultWikiMacro;
import org.xwiki.rendering.wikimacro.internal.WikiMacroClassDocumentInitializer;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.DefaultSkinAccessBridge;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_ID_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_VISIBILITY_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.WIKI_MACRO_CLASS_REFERENCE;

/**
 * Page Test of the {@code XWiki.XWikiSyntaxMacrosList} document.
 *
 * @version $Id$
 * @since 14.6RC1
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@ComponentList({
    // Start document initializer
    WikiMacroClassDocumentInitializer.class,
    DefaultContextStoreManager.class,
    // End document initializer
    // XWikiWikiModel is required to make 
    // org.xwiki.rendering.internal.parser.reference.AbstractUntypedReferenceParser.parse work in "wiki mode". Otherwise
    // untyped references are not resolved.
    // Start of XWikiWikiModel
    XWikiWikiModel.class,
    DefaultSkinAccessBridge.class,
    DefaultResourceReferenceEntityReferenceResolver.class,
    // End of XWikiWikiModel
    DocumentXHTMLLinkTypeRenderer.class,
    DocumentResourceReferenceEntityReferenceResolver.class,
    TestNoScriptMacro.class,
    TranslationMacro.class
})
class XWikiSyntaxMacrosListPageTest extends PageTest
{
    public static final DocumentReference DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "XWikiSyntaxMacrosList");

    private DefaultWikiMacro myMacro;

    @BeforeEach
    void setUp() throws Exception
    {
        // Initialize "WikiMacroClass"
        this.xwiki.initializeMandatoryDocuments(this.context);

        // Mock the database.
        Query query = mock(Query.class);
        QueryManagerScriptService queryManagerScriptService =
            this.componentManager.registerMockComponent(ScriptService.class, "query", QueryManagerScriptService.class,
                false);
        when(queryManagerScriptService.xwql(any())).thenReturn(query);
        when(query.execute()).thenReturn(List.of("xwiki:XWiki.MyMacro"));

        // Create a wiki macro.
        XWikiDocument myMacroDocument = this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "MyMacro"),
            this.context);
        myMacroDocument.setSyntax(Syntax.XWIKI_2_1);
        BaseObject macroObject = myMacroDocument.newXObject(WIKI_MACRO_CLASS_REFERENCE, this.context);
        macroObject.setStringValue(MACRO_VISIBILITY_PROPERTY, "WIKI");
        macroObject.setStringValue(MACRO_ID_PROPERTY, "mymacro");
        this.xwiki.saveDocument(myMacroDocument, this.context);

        // Register the wiki macro component.
        this.myMacro =
            this.componentManager.registerMockComponent(Macro.class, "mymacro", DefaultWikiMacro.class, false);
    }

    @Test
    void renderTable() throws Exception
    {
        DefaultMacroDescriptor macroDescriptor = new DefaultMacroDescriptor(new MacroId("mymacro"), "My Macro",
            "My Macro Description");
        macroDescriptor.setDefaultCategories(Set.of("Category1", "Category2"));
        when(this.myMacro.getDescriptor()).thenReturn(macroDescriptor);

        // Render the page.
        Document document = renderHTMLPage(DOCUMENT_REFERENCE);

        // Assert the values of the cells.
        Elements trs = document.select("tr");
        Elements includeMacroRowTds = trs.get(0).select("th");
        assertEquals("help.macroList.id", includeMacroRowTds.get(0).text());
        assertEquals("help.macroList.name", includeMacroRowTds.get(1).text());
        assertEquals("help.macroList.categories", includeMacroRowTds.get(2).text());
        assertEquals("help.macroList.description", includeMacroRowTds.get(3).text());
        assertEquals("help.macroList.visibility", includeMacroRowTds.get(4).text());
        assertJavaMacro(trs.get(1), "html", "HTML", "Development", "Inserts HTML or XHTML code into the page.",
            "XWiki.WikiMacroClass_visibility_Global");
        assertJavaMacro(trs.get(2), "include", "Include", "Content",
            "Include other pages into the current page.",
            "XWiki.WikiMacroClass_visibility_Global");
        assertWikiMacro(trs.get(3), "mymacro", "/xwiki/bin/view/XWiki/MyMacro", "My Macro",
            Set.of("Category1", "Category2"), "My Macro Description", "XWiki.WikiMacroClass_visibility_WIKI");
        assertJavaMacro(trs.get(4), "noscript", "NoScript", "", "No Script!", "XWiki.WikiMacroClass_visibility_Global");
        assertJavaMacro(trs.get(5), "translation", "Translation", "Content",
            "Display a translation message.", "XWiki.WikiMacroClass_visibility_Global");
        assertJavaMacro(trs.get(6), "velocity", "Velocity", "Development", "Executes a Velocity script.",
            "XWiki.WikiMacroClass_visibility_Global");
    }

    @Test
    void checkTableEscaping() throws Exception
    {
        String unescapedString = "{{noscript /}}";

        DefaultMacroDescriptor macroDescriptor = new DefaultMacroDescriptor(new MacroId("mymacro"), unescapedString,
            unescapedString);
        macroDescriptor.setDefaultCategories(Set.of(unescapedString));
        when(this.myMacro.getDescriptor()).thenReturn(macroDescriptor);

        Document document = renderHTMLPage(DOCUMENT_REFERENCE);

        Elements trs = document.select("tr");
        Element myMacroTr = null;
        for (Element tr : trs) {
            Element th = tr.selectFirst("td");
            if (th != null && th.text().equals("mymacro")) {
                myMacroTr = tr;
            }
        }

        assertNotNull(myMacroTr);
        assertWikiMacro(myMacroTr, "mymacro", "/xwiki/bin/view/XWiki/MyMacro", unescapedString, Set.of(unescapedString),
            unescapedString, "XWiki.WikiMacroClass_visibility_WIKI");
    }

    private void assertWikiMacro(Element rowElement, String id, String link, String name, Set<String> categories,
        String description, String visibility)
    {
        Elements myMacroRowTds = rowElement.select("td");
        assertEquals(id, myMacroRowTds.get(0).text());
        assertEquals(link, myMacroRowTds.get(0).select("a").attr("href"));
        assertEquals(name, myMacroRowTds.get(1).text());
        assertEquals(categories, Arrays.stream(myMacroRowTds.get(2).text().split("\\s*,\\s*"))
            .collect(Collectors.toSet()));
        assertEquals(description, myMacroRowTds.get(3).text());
        assertEquals(visibility, myMacroRowTds.get(4).text());
    }

    private void assertJavaMacro(Element rowElement, String id, String name, String categories, String description,
        String visibility)
    {
        Elements includeMacroRowTds = rowElement.select("td");
        assertEquals(id, includeMacroRowTds.get(0).text());
        assertEquals(name, includeMacroRowTds.get(1).text());
        assertEquals(categories, includeMacroRowTds.get(2).text());
        assertEquals(description, includeMacroRowTds.get(3).text());
        assertEquals(visibility, includeMacroRowTds.get(4).text());
    }
}
