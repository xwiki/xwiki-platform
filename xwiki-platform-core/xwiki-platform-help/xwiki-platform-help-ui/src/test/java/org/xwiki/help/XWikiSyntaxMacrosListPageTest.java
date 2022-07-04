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

import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.configuration.RenderingConfigClassDocumentConfigurationSource;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.wikimacro.internal.DefaultWikiMacro;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.xml.internal.html.filter.ControlCharactersFilter;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_ID_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_VISIBILITY_PROPERTY;

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
    ControlCharactersFilter.class
})
class XWikiSyntaxMacrosListPageTest extends PageTest
{
    public static final DocumentReference
        DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "XWikiSyntaxMacrosList");

    public static final DocumentReference XCLASS_REFERENCE = new DocumentReference("xwiki", "XWiki", "WikiMacroClass");

    @Test
    void renderTable() throws Exception
    {
        XWikiDocument mymacro = this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "MyMacro"),
            this.context);
        mymacro.setSyntax(Syntax.XWIKI_2_1);
        BaseObject macroObject = mymacro.newXObject(XCLASS_REFERENCE, this.context);
        macroObject.setStringValue(MACRO_VISIBILITY_PROPERTY, "WIKI");
        macroObject.setStringValue(MACRO_ID_PROPERTY, "mymacro");
        this.xwiki.saveDocument(mymacro, this.context);

        QueryManagerScriptService queryManagerScriptService =
            this.componentManager.registerMockComponent(ScriptService.class, "query", QueryManagerScriptService.class,
                false);
        DefaultWikiMacro mymacro1 =
            this.componentManager.registerMockComponent(Macro.class, "mymacro", DefaultWikiMacro.class, false);
        DefaultMacroDescriptor t =
            new DefaultMacroDescriptor(new MacroId("mymacro"), "My Macro", "My Macro Description");
        t.setDefaultCategories(Set.of("Category1", "Category2"));
        when(mymacro1.getDescriptor()).thenReturn(t);
        Query query = mock(Query.class);
        when(queryManagerScriptService.xwql(any())).thenReturn(query);
        when(query.execute()).thenReturn(List.of("xwiki:XWiki.MyMacro"));
        Document document = renderHTMLPage(DOCUMENT_REFERENCE);
        System.out.println(document);
        assertNotNull(document);
    }
}
