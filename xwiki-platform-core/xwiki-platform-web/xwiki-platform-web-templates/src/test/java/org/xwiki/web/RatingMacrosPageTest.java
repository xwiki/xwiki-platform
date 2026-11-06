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
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Page test for the {@code rating_macros.vm} velocity template.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    TemplateMacro.class
})
class RatingMacrosPageTest extends PageTest
{
    @Test
    void ratingstarsEscapesIdAndCssClass() throws Exception
    {
        String id = "rating_\"'<em>{{/html}}{{html}}<strong>";
        String cssClass = "small\"'<em>";
        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Test", "Page"), this.context);
        document.setSyntax(XWIKI_2_1);
        document.setContent("""
            {{template name="rating_macros.vm" output="false"/}}

            {{velocity}}
            #set ($id = $request.id)
            #set ($cssClass = $request.cssClass)
            {{html clean="false"}}
            #ratingstars($id $cssClass "" 4 2 true)
            {{/html}}
            {{/velocity}}""");
        this.request.put("id", id);
        this.request.put("cssClass", cssClass);

        Document result = renderHTMLPage(document);

        Element container = result.selectFirst(".rating-container");
        assertEquals(id, container.id());
        assertEquals("rating-container " + cssClass, container.attr("class"));
        assertEquals("rating-header " + cssClass, result.selectFirst(".rating-header").attr("class"));
        assertEquals("star-rating " + cssClass + "-star  locked ", result.selectFirst(".star-rating").attr("class"));
        assertTrue(result.select("em, strong").isEmpty());
    }
}
