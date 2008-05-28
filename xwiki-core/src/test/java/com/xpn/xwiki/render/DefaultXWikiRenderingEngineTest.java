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
package com.xpn.xwiki.render;

import java.io.ByteArrayInputStream;

import javax.servlet.ServletContext;

import org.jmock.Mock;
import org.xwiki.component.manager.ComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractXWikiComponentTestCase;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiServletContext;

/**
 * Unit tests for {@link DefaultXWikiRenderingEngine}.
 * 
 * @version $Id: $
 */
public class DefaultXWikiRenderingEngineTest extends AbstractXWikiComponentTestCase
{
    private DefaultXWikiRenderingEngine engine;

    protected void setUp() throws Exception
    {
        super.setUp();

        // Statically store the component manager in {@link Utils} to be able to access it without
        // the context.
        // @FIXME : move this initialization in AbstractXWikiComponentTestCase.setUp() when
        // shared-tests will depends on core 1.5 branch
        Utils.setComponentManager((ComponentManager) getContext().get(
            ComponentManager.class.getName()));

        XWikiConfig config = new XWikiConfig();

        Mock mockServletContext = mock(ServletContext.class);
        ByteArrayInputStream bais =
            new ByteArrayInputStream("code=wiki:code:type:content".getBytes());
        mockServletContext.stubs().method("getResourceAsStream")
            .with(eq("/templates/macros.txt")).will(returnValue(bais));
        mockServletContext.stubs().method("getResourceAsStream").with(
            eq("/WEB-INF/oscache.properties")).will(
            returnValue(new ByteArrayInputStream("".getBytes())));
        mockServletContext.stubs().method("getResourceAsStream").with(
            eq("/WEB-INF/oscache-local.properties")).will(
            returnValue(new ByteArrayInputStream("".getBytes())));
        mockServletContext.stubs().method("getResourceAsStream").with(
            eq("/skins/albatross/macros.vm")).will(
            returnValue(new ByteArrayInputStream("".getBytes())));
        mockServletContext.stubs().method("getResourceAsStream").with(eq("/templates/macros.vm"))
            .will(returnValue(new ByteArrayInputStream("".getBytes())));
        XWikiServletContext engineContext =
            new XWikiServletContext((ServletContext) mockServletContext.proxy());

        XWiki xwiki = new XWiki(config, getContext(), engineContext, false);
        xwiki.setVersion("1.0");

        this.engine = (DefaultXWikiRenderingEngine) xwiki.getRenderingEngine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.test.AbstractXWikiComponentTestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();

        // Makes sure tests are independents as Utils's ComponentManager is a static
        // @FIXME : move this initialization in AbstractXWikiComponentTestCase.setUp() when
        // shared-tests will depends on core 1.5 branch
        Utils.setComponentManager(null);
    }

    public void testRenderTextWhenUsingCodeMacro() throws Exception
    {
        // We verify that the code macro doesn't render wiki markup, velocity, HTML, or other radeox
        // macros.
        // We also ensure that any Radeox macro coming after the code macro is rendered properly.
        // Last we also ensure that a second code macro works too.
        String text =
            "{code:none}\n" + "1 Title\n" + "c:\\dev\n" + "#info(\"test\")\n"
                + "<pre>hello</pre>\n" + "$xwiki.getVersion()\n" + "{style}style{style}\n"
                + "&#123;code}nested&#123;code}\n" + "<% print(\"hello\") %>\n" + "{code}\n"
                + "{table}\n" + "a | b\n" + "c | d\n" + "{table}\n" + "#set ($var = 'dummy')\n"
                + "{code:none}\n" + "1 Something\n" + "{code}";

        String expectedText =
            "<div class=\"code\"><pre>1 Title\n"
                + "c:&#92;dev\n"
                + "&#35;info(\"test\")\n"
                + "&#60;pre&#62;hello&#60;/pre&#62;\n"
                + "&#36;xwiki.getVersion()\n"
                + "&#123;style&#125;style&#123;style&#125;\n"
                + "&&#35;123;code&#125;nested&&#35;123;code&#125;\n"
                + "&#60;% print(\"hello\") %&#62;</pre></div>\n"
                + "<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><th>a</th>"
                + "<th>b</th></tr><tr class=\"table-odd\"><td>c</td><td>d</td></tr></table>\n"
                + "<div class=\"code\"><pre>1 Something</pre></div>";

        XWikiDocument document = new XWikiDocument();
        assertEquals(expectedText, engine.renderText(text, document, getContext()));
    }
}
