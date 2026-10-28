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

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Tests the {@code html_displayer/spacereference/edit.vm} template.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    TemplateScriptService.class,
    ModelScriptService.class
})
class SpaceReferenceEditDisplayerPageTest extends PageTest
{
    @Test
    void singleLocation() throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Test", "Page"), this.context);
        document.setSyntax(XWIKI_2_1);
        document.setContent("""
            {{velocity}}
            #set ($displayer = {'parameters': {'name': 'location'}, 'value': 'A.B'})
            {{html clean='false'}}#template('html_displayer/spacereference/edit.vm'){{/html}}
            {{/velocity}}""");
        Document html = renderHTMLPage(document);

        Element select = html.selectFirst("select.suggest-spaces");
        assertEquals("location", select.attr("name"));
        assertFalse(select.hasAttr("multiple"));
        assertEquals("A.B", select.selectFirst("option[selected]").val());
    }
}
