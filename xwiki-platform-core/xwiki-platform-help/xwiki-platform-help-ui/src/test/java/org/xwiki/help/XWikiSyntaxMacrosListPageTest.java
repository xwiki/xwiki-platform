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
import org.junit.jupiter.api.Test;
import org.xwiki.context.internal.concurrent.DefaultContextStoreManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.configuration.RenderingConfigClassDocumentConfigurationSource;
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
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.xml.internal.html.filter.ControlCharactersFilter;

import com.xpn.xwiki.DefaultSkinAccessBridge;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * @since 16.4RC1
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@RenderingScriptServiceComponentList
@ComponentList({
    // Start - Required in addition of RenderingScriptServiceComponentList
    DefaultExtendedRenderingConfiguration.class,
    RenderingConfigClassDocumentConfigurationSource.class,
    // End - Required in additional of RenderingScriptServiceComponentList
    ControlCharactersFilter.class,
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
    DocumentResourceReferenceEntityReferenceResolver.class
})
class XWikiSyntaxMacrosListPageTest extends PageTest
{
    public static final DocumentReference
        DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "XWikiSyntaxMacrosList");

    @Test
    void renderTable() throws Exception
    {
        // Initialize "WikiMacroClass"
        this.xwiki.initializeMandatoryDocuments(this.context);
        
        // Create a wiki macro.
        XWikiDocument myMacroDocument = this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "MyMacro"),
            this.context);
        myMacroDocument.setSyntax(Syntax.XWIKI_2_1);
        BaseObject macroObject = myMacroDocument.newXObject(WIKI_MACRO_CLASS_REFERENCE, this.context);
        macroObject.setStringValue(MACRO_VISIBILITY_PROPERTY, "WIKI");
        macroObject.setStringValue(MACRO_ID_PROPERTY, "mymacro");
        this.xwiki.saveDocument(myMacroDocument, this.context);

        // Register the wiki macro component.
        DefaultWikiMacro myMacro =
            this.componentManager.registerMockComponent(Macro.class, "mymacro", DefaultWikiMacro.class, false);
        DefaultMacroDescriptor macroDescriptor =
            new DefaultMacroDescriptor(new MacroId("mymacro"), "My Macro", "My Macro Description");
        macroDescriptor.setDefaultCategories(Set.of("Category1", "Category2"));
        when(myMacro.getDescriptor()).thenReturn(macroDescriptor);
        
        // Mock the database.
        Query query = mock(Query.class);
        QueryManagerScriptService queryManagerScriptService =
            this.componentManager.registerMockComponent(ScriptService.class, "query", QueryManagerScriptService.class,
                false);
        when(queryManagerScriptService.xwql(any())).thenReturn(query);
        when(query.execute()).thenReturn(List.of("xwiki:XWiki.MyMacro"));
        
        // Render the page.
        Document document = renderHTMLPage(DOCUMENT_REFERENCE);
        
        
        // Assert the values of the cells.
        Elements trs = document.select("tr");
        Element headersRow = trs.get(0);
        Elements headerThs = headersRow.select("th");
        assertEquals("help.macroList.id", headerThs.get(0).text());
        assertEquals("help.macroList.name", headerThs.get(1).text());
        assertEquals("help.macroList.categories", headerThs.get(2).text());
        assertEquals("help.macroList.description", headerThs.get(3).text());
        assertEquals("help.macroList.visibility", headerThs.get(4).text());
        Element myMacroRow = trs.get(1);
        Elements myMacroRowTds = myMacroRow.select("td");
        assertEquals("mymacro", myMacroRowTds.get(0).text());
        assertEquals("/xwiki/bin/view/XWiki/MyMacro", myMacroRowTds.get(0).select("a").attr("href"));
        assertEquals("My Macro", myMacroRowTds.get(1).text());
        assertEquals(Set.of("Category1", "Category2"), Arrays.stream(myMacroRowTds.get(2).text().split("\\s*,\\s*"))
            .collect(Collectors.toSet()));
        assertEquals("My Macro Description", myMacroRowTds.get(3).text());
        assertEquals("WIKI", myMacroRowTds.get(4).text());
        Element velocityMacroRow = trs.get(2);
        Elements velocityMacroRowTds = velocityMacroRow.select("td");
        assertEquals("velocity", velocityMacroRowTds.get(0).text());
        assertEquals("Velocity", velocityMacroRowTds.get(1).text());
        assertEquals("Development", velocityMacroRowTds.get(2).text());
        assertEquals("Executes a Velocity script.", velocityMacroRowTds.get(3).text());
        assertEquals("Global", velocityMacroRowTds.get(4).text());
        Element htmlMacroRow = trs.get(3);
        Elements htmlMacroRowTds = htmlMacroRow.select("td");
        assertEquals("html", htmlMacroRowTds.get(0).text());
        assertEquals("HTML", htmlMacroRowTds.get(1).text());
        assertEquals("Development", htmlMacroRowTds.get(2).text());
        assertEquals("Inserts HTML or XHTML code into the page.", htmlMacroRowTds.get(3).text());
        assertEquals("Global", htmlMacroRowTds.get(4).text());
        Element includeMacroRow = trs.get(4);
        Elements includeMacroRowTds = includeMacroRow.select("td");
        assertEquals("include", includeMacroRowTds.get(0).text());
        assertEquals("Include", includeMacroRowTds.get(1).text());
        assertEquals("Content", includeMacroRowTds.get(2).text());
        assertEquals("Include other pages into the current page.", includeMacroRowTds.get(3).text());
        assertEquals("Global", includeMacroRowTds.get(4).text());
    }
}
