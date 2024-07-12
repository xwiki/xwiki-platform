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
package org.xwiki.search.ui;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.feed.FeedPlugin;
import com.xpn.xwiki.web.XWikiServletResponseStub;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Page test for {@code Main.DatabaseSearch}.
 *
 * @version $Id$
 */
@ComponentList({
    TestNoScriptMacro.class
})
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
class DatabaseSearchPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final String MAIN_SPACE = "Main";

    private static final DocumentReference DATABASE_SEARCH_REFERENCE =
        new DocumentReference(WIKI_NAME, MAIN_SPACE, "DatabaseSearch");

    @BeforeEach
    void setUp()
    {
        this.xwiki.initializeMandatoryDocuments(this.context);

        this.xwiki.getPluginManager().addPlugin("feed", FeedPlugin.class.getName(), this.context);
    }

    @Test
    void checkRSSFeedContent() throws Exception
    {
        String unescapedText = "<b>}}}{{noscript}}</b>";
        String escapedText = "&lt;b&gt;}}}{{noscript}}&lt;/b&gt;";

        this.request.put("text", unescapedText);
        this.context.setAction("get");

        XWikiDocument databaseSearchDocument = loadPage(DATABASE_SEARCH_REFERENCE);
        this.context.setDoc(databaseSearchDocument);

        // Get directly the writer to check the RSS feed.
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        this.response = new XWikiServletResponseStub() {
            @Override
            public PrintWriter getWriter()
            {
                return writer;
            }
        };
        this.context.setResponse(this.response);

        String rssFeed = databaseSearchDocument.displayDocument(Syntax.PLAIN_1_0, this.context);
        assertTrue(StringUtils.isAllBlank(rssFeed));

        rssFeed = out.toString();
        assertTrue(rssFeed.contains("<title>search.rss [" + escapedText + "]</title>"));
        assertTrue(rssFeed.contains("<description>search.rss [" + escapedText + "]</description>"));
    }
}
