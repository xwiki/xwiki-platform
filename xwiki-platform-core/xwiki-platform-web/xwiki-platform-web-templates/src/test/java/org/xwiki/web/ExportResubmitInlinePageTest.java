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
package org.xwiki.web;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test of template {@code exportresubmitinline.vm}.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
class ExportResubmitInlinePageTest extends PageTest
{
    private static final String TEMPLATE_NAME = "exportresubmitinline.vm";

    private TemplateManager templateManager;

    @Mock
    private CSRFTokenScriptService csrfScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        this.componentManager.registerComponent(ScriptService.class, "csrf", this.csrfScriptService);
        when(this.csrfScriptService.getToken()).thenReturn("token42");

        XWikiDocument currentDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Some", "Page"), this.context);
        this.xwiki.saveDocument(currentDocument, this.context);
        this.context.setDoc(currentDocument);
    }

    @Test
    void resubmitTheRequestedExport() throws Exception
    {
        this.request.put("format", "xar");
        this.request.put("name", "My Export");
        this.request.put("pages", "xwiki:Some.%");

        Document document = Jsoup.parse(this.templateManager.render(TEMPLATE_NAME));

        Element form = document.getElementById("exportResubmit");
        assertEquals("post", form.attr("method"));
        assertEquals("/xwiki/bin/export/Some/Page", form.attr("action"));

        // The requested export is described to the user.
        assertEquals(List.of("format", "name", "pages"), form.select(".export-parameters dt").eachText());
        assertEquals(List.of("xar", "My Export", "xwiki:Some.%"), form.select(".export-parameters dd").eachText());

        // The same export is submitted again, with a token of the current user.
        assertEquals("token42", form.selectFirst("input[name='form_token']").attr("value"));
        assertEquals("xar", form.selectFirst("input[type='hidden'][name='format']").attr("value"));
        assertEquals("My Export", form.selectFirst("input[type='hidden'][name='name']").attr("value"));
        assertEquals("xwiki:Some.%", form.selectFirst("input[type='hidden'][name='pages']").attr("value"));
    }

    @Test
    void resubmitWithoutAnyParameter() throws Exception
    {
        Document document = Jsoup.parse(this.templateManager.render(TEMPLATE_NAME));

        Element form = document.getElementById("exportResubmit");
        // The user is warned that this would export the whole wiki.
        assertTrue(form.select(".export-parameters").isEmpty());
        assertEquals("core.export.resubmit.parameters.none", form.selectFirst(".xHint").text());
        // Only the token is submitted.
        assertEquals(1, form.select("input[type='hidden']").size());
        assertEquals("token42", form.selectFirst("input[name='form_token']").attr("value"));
    }

    @Test
    void doNotSubmitTheTokenOfTheRequest() throws Exception
    {
        // A token taken from the URL that led to this page must not be reused: the export is submitted by the user
        // reading the page, not by whoever built that URL.
        this.request.put("form_token", "forgedToken");

        Document document = Jsoup.parse(this.templateManager.render(TEMPLATE_NAME));

        Element form = document.getElementById("exportResubmit");
        assertTrue(form.select(".export-parameters").isEmpty());
        assertEquals(1, form.select("input[name='form_token']").size());
        assertEquals("token42", form.selectFirst("input[name='form_token']").attr("value"));
    }

    @Test
    void escapeParameters() throws Exception
    {
        String escapingTest = "\"'><script>console.log('escaping');</script>";
        this.request.put(escapingTest, escapingTest);

        Document document = Jsoup.parse(this.templateManager.render(TEMPLATE_NAME));

        assertNull(document.selectFirst("script"));
        Element form = document.getElementById("exportResubmit");
        assertEquals(escapingTest, form.selectFirst(".export-parameters dt").text());
        assertEquals(escapingTest, form.selectFirst(".export-parameters dd").text());
        Element parameterInput = form.select("input[type='hidden']").stream()
            .filter(input -> !"form_token".equals(input.attr("name")))
            .findFirst()
            .orElseThrow();
        assertEquals(escapingTest, parameterInput.attr("name"));
        assertEquals(escapingTest, parameterInput.attr("value"));
    }
}
