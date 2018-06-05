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
package org.xwiki.help;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XHTML10ComponentList;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for testing the {@code XWiki.XWikiSyntax} wiki page.
 *
 * @version $Id$
 * @since 8.3M2
 */
@XWikiSyntax21ComponentList
@XHTML10ComponentList
public class HelpTest extends PageTest
{
    /**
     * The bug we're trying to prevent happening again was that there was that "$subHeading" was rendered when going to
     * the Links section (for example).
     * Note: It was working fine when displaying all sections though.
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-13650">XWIKI-13650</a>
     */
    @Test
    public void verifySubHeadingVelocityVariableCorrectlyEvaluatedWhenUsedInSection() throws Exception
    {
        // URL that we're simulating:
        //   http://localhost:8080/xwiki/bin/view/XWiki/XWikiSyntax?syntax=2.1&section=Links&xpage=print
        setOutputSyntax(Syntax.XHTML_1_0);
        request.put("section", "Links");
        request.put("xpage", "print");

        // Register the XWikiSyntaxLinks page with an XWikiSyntaxClass xobject in it so that it can be found later on
        // by XWiki.XWikiSyntax
        loadPage(new DocumentReference("xwiki", "XWiki", "XWikiSyntaxLinks"));

        // Register query script service since it's not registered by default and it's used in XWiki.XWikiSyntax to
        // find all syntax pages.
        QueryManagerScriptService qss = mock(QueryManagerScriptService.class);
        componentManager.registerComponent(ScriptService.class, "query", qss);
        ScriptQuery sq = mock(ScriptQuery.class);
        when(qss.xwql(contains("from doc.object(XWiki.XWikiSyntaxClass)"))).thenReturn(sq);
        when(sq.addFilter((String)any())).thenReturn(sq);
        when(sq.execute()).thenReturn(Arrays.asList("XWiki.XWikiSyntaxLinks"));

        String result = renderPage(new DocumentReference("xwiki", "XWiki", "XWikiSyntax"));
        assertTrue(result.contains("<h3 id=\"HXWikiSyntax2.1LinkSpecification\""),
            "$subHeading should have been evaluated and replaced by '==='");
        assertTrue(!result.contains("$subHeading"),
            "$subHeading should have been evaluated and replaced by '==='");
    }
}
