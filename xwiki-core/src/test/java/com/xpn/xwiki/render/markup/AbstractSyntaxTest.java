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
 *
 */
package com.xpn.xwiki.render.markup;

import java.util.ArrayList;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRadeoxRenderer;

public abstract class AbstractSyntaxTest extends MockObjectTestCase
{
    private XWikiContext context;

    private XWikiRadeoxRenderer renderer;

    private Mock mockXWiki;

    private Mock mockDocument;

    private XWikiDocument document;

    protected void setUp()
    {
        this.renderer = new XWikiRadeoxRenderer();
        this.context = new XWikiContext();

        this.mockXWiki =
            mock(XWiki.class, new Class[] {XWikiConfig.class, XWikiContext.class}, new Object[] {
            new XWikiConfig(), context});
        this.context.setWiki((XWiki) this.mockXWiki.proxy());

        this.mockDocument = mock(XWikiDocument.class);
        this.document = (XWikiDocument) this.mockDocument.proxy();

        this.context.setDoc(document);
    }

    protected void test(ArrayList<String> tests, ArrayList<String> expects)
    {
        for (int i = 0; i < tests.size(); ++i) {
            String result = renderer.render(tests.get(i).toString(), document, document, context);
            String expected = expects.get(i).toString();
            if (expected.startsWith("...")) {
                assertTrue(result.indexOf(expected.substring(3, expected.length() - 3)) > 0);
            } else {
                assertEquals(expects.get(i).toString(), result);
            }
        }
    }
}
