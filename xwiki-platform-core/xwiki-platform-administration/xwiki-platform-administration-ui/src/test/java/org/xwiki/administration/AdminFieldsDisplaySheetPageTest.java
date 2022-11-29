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

import java.util.Map;

import javax.script.ScriptContext;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.internal.configuration.DefaultExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.configuration.RenderingConfigClassDocumentConfigurationSource;
import org.xwiki.rendering.internal.macro.DefaultMacroCategoryManager;
import org.xwiki.rendering.internal.syntax.SyntaxConverter;
import org.xwiki.rendering.internal.transformation.macro.DefaultMacroTransformationConfiguration;
import org.xwiki.rendering.script.RenderingScriptService;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.velocity.tools.EscapeTool;
import org.xwiki.xml.internal.html.filter.ControlCharactersFilter;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static javax.script.ScriptContext.ENGINE_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Page test of {@code XWiki.AdminFieldsDisplaySheet}.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.1
 * @since 14.4.8
 * @since 13.10.11
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    TestNoScriptMacro.class,
    DefaultExtendedRenderingConfiguration.class,
    RenderingConfigClassDocumentConfigurationSource.class,
    // Start RenderingScriptService
    RenderingScriptService.class,
    DefaultExtendedRenderingConfiguration.class,
    RenderingConfigClassDocumentConfigurationSource.class,
    SyntaxConverter.class,
    DefaultMacroCategoryManager.class,
    DefaultMacroTransformationConfiguration.class,
    // End RenderingScriptService
    ControlCharactersFilter.class
})
class AdminFieldsDisplaySheetPageTest extends PageTest
{
    private ScriptContext scriptContext;

    @BeforeEach
    void setUp() throws Exception
    {
        this.scriptContext =
            this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class).getScriptContext();
        registerVelocityTool("escapetool", new EscapeTool());
    }

    @Test
    void escaping() throws Exception
    {
        String paramsInput = "\"/><script>console.log('params');</script>{{/html}}{{noscript/}}";
        String sectionInput = "\"/><strong>console.log('section');</script>{{/html}}{{noscript/}}";
        String paramClassInput = "\"/><script>console.log('paramClass');</script>{{/html}}{{noscript/}}";
        Map<Object, Object> params = singletonMap(paramsInput, emptyList());
        DocumentReference otherDocumentReference = new DocumentReference("xwiki", "Space", "Page");
        com.xpn.xwiki.api.Document otherDocument =
            new com.xpn.xwiki.api.Document(this.xwiki.getDocument(otherDocumentReference, this.context), this.context);

        this.scriptContext.setAttribute("section", sectionInput, ENGINE_SCOPE);
        this.scriptContext.setAttribute("paramDoc", otherDocument, ENGINE_SCOPE);
        this.scriptContext.setAttribute("params", params, ENGINE_SCOPE);
        this.scriptContext.setAttribute("paramClass", paramClassInput, ENGINE_SCOPE);

        Document document = renderHTMLPage(new DocumentReference("xwiki", "XWiki", "AdminFieldsDisplaySheet"));

        Element form = document.selectFirst("form");
        assertEquals(String.format("%s_%s", sectionInput, paramClassInput), form.attr("id"));
        assertEquals("/xwiki/bin/saveandcontinue/Space/Page", form.attr("action"));
        Element fieldset = form.selectFirst("fieldset");
        assertEquals(paramsInput, fieldset.attr("class"));
        assertEquals(paramClassInput, document.selectFirst(".hidden input[name='classname']").val());
    }
}
