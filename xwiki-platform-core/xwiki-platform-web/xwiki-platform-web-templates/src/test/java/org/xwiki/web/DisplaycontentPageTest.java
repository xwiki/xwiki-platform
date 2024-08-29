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

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.job.script.ProgressScripService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiServletResponseStub;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the {@code displaycontent.vm} template.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    TemplateScriptService.class,
    ProgressScripService.class
})
class DisplaycontentPageTest extends PageTest
{
    private static final DocumentReference TEST_REFERENCE = new DocumentReference("xwiki", "space", "page");

    private static final String TEMPLATE_NAME = "displaycontent.vm";

    private static final String HTML_CONTENT_TYPE = "text/html; charset=utf-8";

    private XWikiDocument document;

    @Inject
    private TemplateManager templateManager;

    @BeforeEach
    void setUp() throws XWikiException
    {
        this.document = new XWikiDocument(TEST_REFERENCE);
        this.document.setSyntax(Syntax.XWIKI_2_1);
        this.xwiki.saveDocument(this.document, this.context);
        this.context.setDoc(this.document);

        // Allow setting the content type in the response, so we can assert it later. The default is consistent with
        // Utils#parseTemplate(String, boolean, XWikiContext).
        this.response = new XWikiServletResponseStub()
        {
            private String contentType = HTML_CONTENT_TYPE;

            @Override
            public String getContentType()
            {
                return this.contentType;
            }

            @Override
            public void setContentType(String type)
            {
                this.contentType = type;
            }
        };
        this.context.setResponse(this.response);
    }

    @Test
    void renderHTML() throws Exception
    {
        this.document.setContent("Hello World");
        this.xwiki.saveDocument(this.document, this.context);

        String result = this.templateManager.render(TEMPLATE_NAME).trim();
        assertEquals("<p>Hello World</p>", result);
        assertEquals(HTML_CONTENT_TYPE, this.response.getContentType());
    }

    @Test
    void renderJSON() throws Exception
    {
        String json = "{\"hello\": \"world\"}";
        this.document.setContent(json
            + " {{velocity}}#set ($discard = $response.setContentType('application/json')){{/velocity}}");
        this.xwiki.saveDocument(this.document, this.context);
        this.request.put("outputSyntax", "plain");

        String result = this.templateManager.render(TEMPLATE_NAME).trim();
        assertEquals(json, result);
        assertEquals("application/json", this.response.getContentType());
    }

    @Test
    void renderPlain() throws Exception
    {
        this.document.setContent("<strong>Some HTML</strong>");
        this.xwiki.saveDocument(this.document, this.context);
        this.request.put("outputSyntax", "plain");

        String result = this.templateManager.render(TEMPLATE_NAME).trim();
        assertEquals(this.document.getContent(), result);
        assertEquals("text/plain", this.response.getContentType());
    }
}
