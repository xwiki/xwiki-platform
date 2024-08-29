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

import java.util.Collections;
import java.util.Map;

import javax.script.ScriptContext;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.internal.RestrictedConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiPreferencesDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.DBListClass;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static javax.script.ScriptContext.ENGINE_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
    XWikiPreferencesDocumentInitializer.class,
    ModelScriptService.class,
    RestrictedConfigurationSource.class
})
class AdminFieldsDisplaySheetPageTest extends PageTest
{
    private ScriptContext scriptContext;

    @Mock
    private Query query;

    @BeforeEach
    void setUp() throws Exception
    {
        this.scriptContext =
            this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class).getScriptContext();
        // Mock the database access for now as we don't need it.
        QueryBuilder<DBListClass> queryBuilder = this.componentManager.registerMockComponent(
            new DefaultParameterizedType(null, QueryBuilder.class, DBListClass.class));
        when(queryBuilder.build(any())).thenReturn(this.query);
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

    @Test
    void displayLinkInColorThemeHint() throws Exception
    {
        DocumentReference flamingoThemeDocumentReference = new DocumentReference("xwiki", "FlamingoThemes", "WebHome");
        DocumentReference xwikiPreferencesDocumentReference =
            new DocumentReference("xwiki", "XWiki", "XWikiPreferences");
        DocumentReference otherDocumentReference = new DocumentReference("xwiki", "Space", "Page");

        // Initialize XWiki.XWikiPreferences
        this.xwiki.initializeMandatoryDocuments(this.context);

        // Initialize the flamingo theme.
        this.xwiki.saveDocument(this.xwiki.getDocument(flamingoThemeDocumentReference, this.context), this.context);

        // Initialize the document to be displayed with the sheet.
        XWikiDocument otherDoc = this.xwiki.getDocument(otherDocumentReference, this.context);
        BaseObject xObject =
            otherDoc.newXObject(xwikiPreferencesDocumentReference, this.context);
        xObject.set("colorTheme", "themeName", this.context);
        this.xwiki.saveDocument(otherDoc, this.context);

        // Initialize the sheet parameters.
        this.scriptContext.setAttribute("section", "test-section", ENGINE_SCOPE);
        this.scriptContext.setAttribute("paramDoc", new com.xpn.xwiki.api.Document(otherDoc, this.context),
            ENGINE_SCOPE);
        this.scriptContext.setAttribute("params",
            Collections.<Object, Object>singletonMap("param-input", singletonList("colorTheme")), ENGINE_SCOPE);

        Document document = renderHTMLPage(new DocumentReference("xwiki", "XWiki", "AdminFieldsDisplaySheet"));

        Element xHint = document.selectFirst(".xHint");
        assertEquals("admin.colortheme.manage", xHint.text());
        assertEquals("/xwiki/bin/view/FlamingoThemes/", xHint.selectFirst("a").attr("href"));
    }
}
