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

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Page test of {@code XWiki.AdminSheet}.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    TestNoScriptMacro.class
})
class AdminSheetPageTest extends PageTest
{
    private static final DocumentReference ADMIN_SHEET = new DocumentReference("xwiki", "XWiki", "AdminSheet");

    @Test
    void sectionEscaping() throws Exception
    {
        this.request.put("section", "{{noscript /}}");
        this.request.put("viewer", "content");

        Document result = renderHTMLPage(ADMIN_SHEET);
        Element documentTitle = result.getElementById("document-title");
        assertNotNull(documentTitle);
        assertEquals("administration.sectionTitle [{{noscript /}}]", documentTitle.text());
    }
}
