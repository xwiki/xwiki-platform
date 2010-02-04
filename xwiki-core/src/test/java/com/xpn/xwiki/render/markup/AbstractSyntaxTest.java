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

import java.net.URL;
import java.util.ArrayList;

import org.jmock.Mock;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRadeoxRenderer;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.web.XWikiServletURLFactory;

public abstract class AbstractSyntaxTest extends AbstractBridgedXWikiComponentTestCase
{
    protected XWikiContext context;

    protected XWikiRadeoxRenderer renderer;

    private Mock mockXWiki;

    protected XWikiDocument document;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.renderer = new XWikiRadeoxRenderer();
        this.context = new XWikiContext();

        this.mockXWiki = mock(XWiki.class);
        // These are needed by the Link filter
        this.mockXWiki.stubs().method("exists").will(returnValue(true));
        this.mockXWiki.stubs().method("showViewAction").will(returnValue(true));
        this.mockXWiki.stubs().method("skipDefaultSpaceInURLs").will(returnValue(true));
        this.mockXWiki.stubs().method("useDefaultAction").will(returnValue(true));
        this.mockXWiki.stubs().method("getDefaultSpace").will(returnValue("Main"));
        this.mockXWiki.stubs().method("getEncoding").will(returnValue("UTF-8"));
        this.mockXWiki.stubs().method("getServletPath").will(returnValue("bin/"));

        this.context.setWiki((XWiki) this.mockXWiki.proxy());

        this.context.setURLFactory(new XWikiServletURLFactory(new URL("http://localhost/"), "xwiki/", "bin/"));

        this.document = new XWikiDocument("Main", "WebHome");

        this.context.setDoc(this.document);
    }

    protected void test(ArrayList<String> tests, ArrayList<String> expects)
    {
        for (int i = 0; i < tests.size(); ++i) {
            String result = this.renderer.render(tests.get(i).toString(), this.document, this.document, this.context);
            String expected = expects.get(i).toString();
            if (expected.startsWith("...")) {
                assertTrue(result.indexOf(expected.substring(3, expected.length() - 3)) > 0);
            } else {
                assertEquals(expected, result);
            }
        }
    }
}
