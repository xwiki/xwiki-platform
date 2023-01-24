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
package org.xwikiplatform.appwithinminutes;

import java.util.List;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.groovy.internal.DefaultGroovyConfiguration;
import org.xwiki.groovy.internal.GroovyScriptEngineFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.internal.macro.groovy.GroovyMacro;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Page Test of {@code AppWithinMinutes.ClassEditSheet}.
 *
 * @version $Id$
 * @since 14.4.8
 * @since 14.10.4
 * @since 15.0
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    // Start GroovyMacro 
    GroovyMacro.class,
    GroovyScriptEngineFactory.class,
    DefaultGroovyConfiguration.class
    // End GroovyMacro
})
class ClassEditSheetPageTest extends PageTest
{
    private QueryManagerScriptService queryManagerScriptService;

    @Mock
    private Query query;

    @BeforeEach
    void setUp() throws Exception
    {
        this.queryManagerScriptService =
            this.componentManager.registerMockComponent(ScriptService.class, "query", QueryManagerScriptService.class,
                false);
    }

    @Test
    void displayFieldPalette() throws Exception
    {
        loadPage(new DocumentReference("xwiki", "AppWithinMinutes", "VelocityMacros"));
        loadPage(new DocumentReference("xwiki", "AppWithinMinutes", "ClassEditSheet"));

        when(this.queryManagerScriptService.xwql("from doc.object(AppWithinMinutes.FormFieldCategoryClass) as category "
            + "order by category.priority")).thenReturn(this.query);
        when(this.query.execute()).thenReturn(List.of("xwiki:XWiki.Category"));

        XWikiDocument xWikiDocumentCategory =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Category"), this.context);
        xWikiDocumentCategory.setTitle("<strong>TITLE</strong>");
        this.xwiki.saveDocument(xWikiDocumentCategory, this.context);

        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Page"), this.context);

        xwikiDocument.setContent("{{include reference=\"AppWithinMinutes.ClassEditSheet\" /}}\n"
            + "\n"
            + "{{velocity}}\n"
            + "#displayFieldPalette()\n"
            + "{{/velocity}}\n");
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        this.xwiki.saveDocument(xwikiDocument, this.context);

        Document document = renderHTMLPage(xwikiDocument);

        assertEquals("<strong>TITLE</strong>", document.selectFirst(".category").text());
    }
}
