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
package com.xpn.xwiki.render;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import javax.servlet.ServletContext;

import org.jmock.Mock;
import org.junit.Assert;
import org.xwiki.localization.LocalizationContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequestStub;

/**
 * Unit tests for {@link DefaultXWikiRenderingEngine}.
 * 
 * @version $Id$
 */
public class DefaultXWikiRenderingEngineTest extends AbstractBridgedXWikiComponentTestCase
{
    private DefaultXWikiRenderingEngine engine;

    private XWiki xwiki;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        Mock mockLocalizationContext = registerMockComponent(LocalizationContext.class);
        mockLocalizationContext.stubs().method("getCurrentLocale").will(returnValue(Locale.ROOT));
        
        Mock mockServletContext = mock(ServletContext.class);
        ByteArrayInputStream bais = new ByteArrayInputStream("code=wiki:code:type:content".getBytes("UTF-8"));
        mockServletContext.stubs().method("getResourceAsStream").with(eq("/templates/macros.txt"))
            .will(returnValue(bais));
        mockServletContext.stubs().method("getResourceAsStream").with(eq("/WEB-INF/oscache.properties"))
            .will(returnValue(new ByteArrayInputStream("".getBytes("UTF-8"))));
        mockServletContext.stubs().method("getResourceAsStream").with(eq("/WEB-INF/oscache-local.properties"))
            .will(returnValue(new ByteArrayInputStream("".getBytes("UTF-8"))));
        XWikiServletContext engineContext = new XWikiServletContext((ServletContext) mockServletContext.proxy());

        getContext().setURL(new URL("http://host"));
        getContext().setRequest(new XWikiServletRequestStub());

        xwiki = new XWiki(new XWikiConfig(), getContext(), engineContext, false)
        {
            @Override
            public String getSkin(XWikiContext context)
            {
                return "skin";
            }

            @Override
            public String getXWikiPreference(String prefname, String defaultValue, XWikiContext context)
            {
                return defaultValue;
            }

            @Override
            public String getSpacePreference(String prefname, String defaultValue, XWikiContext context)
            {
                return defaultValue;
            }

            @Override
            public XWikiRightService getRightService()
            {
                return new XWikiRightServiceImpl()
                {
                    @Override
                    public boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context)
                    {
                        return true;
                    }
                };
            }
        };
        xwiki.setVersion("1.0");

        // Ensure that no Velocity Templates are going to be used when executing Velocity since otherwise
        // the Velocity init would fail (since by default the macros.vm templates wouldn't be found as we're
        // not providing it in our unit test resources).
        getConfigurationSource().setProperty("xwiki.render.velocity.macrolist", "");

        this.engine = (DefaultXWikiRenderingEngine) xwiki.getRenderingEngine();

        // Make sure the wiki in the context will say that we have programming permission.
        getContext().setWiki(this.xwiki);
    }

    public void testRenderTextWhenUsingCodeMacro() throws Exception
    {
        // We verify that the code macro doesn't render wiki markup, velocity, HTML, or other radeox
        // macros.
        // We also ensure that any Radeox macro coming after the code macro is rendered properly.
        // Last we also ensure that a second code macro works too.
        String text =
            "{code:none}\n" + "1 Title\n" + "c:\\dev\n" + "#info(\"test\")\n" + "<pre>hello</pre>\n"
                + "$xwiki.getVersion()\n" + "{style}style{style}\n" + "&#123;code}nested&#123;code}\n"
                + "<% print(\"hello\") %>\n" + "{code}\n" + "{table}\n" + "a | b\n" + "c | d\n" + "{table}\n"
                + "#set ($var = 'dummy')\n" + "{code:none}\n" + "1 Something\n" + "{code}";

        String expectedText =
            "<div class=\"code\"><pre>1 Title\n" + "c:&#92;dev\n" + "&#35;info(\"test\")\n"
                + "&#60;pre&#62;hello&#60;/pre&#62;\n" + "&#36;xwiki.getVersion()\n"
                + "&#123;style&#125;style&#123;style&#125;\n" + "&&#35;123;code&#125;nested&&#35;123;code&#125;\n"
                + "&#60;% print(\"hello\") %&#62;</pre></div>\n"
                + "<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><th>a</th>"
                + "<th>b</th></tr><tr class=\"table-odd\"><td>c</td><td>d</td></tr></table>\n"
                + "<div class=\"code\"><pre>1 Something</pre></div>";

        XWikiDocument document = new XWikiDocument();
        assertEquals(expectedText, engine.renderText(text, document, getContext()));
    }

    /**
     * Test that links are preserved after rendering. XWIKI-2672
     */
    public void testLinksAndCache() throws Exception
    {
        String link = "http://some:123/link";
        String text = "$xcontext.setCacheDuration(1800)\n" + link;
        XWikiDocument document = new XWikiDocument();

        Utils.enablePlaceholders(getContext());
        String out = engine.renderText(text, document, getContext());
        assertTrue(out.contains(link));
    }

    public void testRenderGroovy() throws Exception
    {
        assertEquals("hello world",
            engine.renderText("<% println(\"hello world\"); %>", new XWikiDocument(), getContext()));
    }

    public void testSwitchOrderOfRenderers() throws Exception
    {
        String text = "#set($x = '<' + '% println(\"hello world\"); %' + '>')\n$x";
        String velocityFirst = "hello world";
        String groovyFirst = "<% println(\"hello world\"); %>";

        XWikiDocument document = new XWikiDocument();

        // Prove that the renderers are in the right order by default.
        assertEquals(engine.getRendererNames(), new ArrayList<String>()
        {
            {
                add("mapping");
                add("groovy");
                add("velocity");
                add("plugin");
                add("wiki");
                add("xwiki");
            }
        });

        assertEquals(groovyFirst, engine.renderText(text, document, getContext()));

        getConfigurationSource().setProperty("xwiki.render.renderingorder",
            new String[] {"macromapping", "velocity", "groovy", "plugin", "wiki", "wikiwiki"});

        DefaultXWikiRenderingEngine myEngine = new DefaultXWikiRenderingEngine(xwiki, getContext());

        assertEquals(myEngine.getRendererNames(), new ArrayList<String>()
        {
            {
                add("mapping");
                add("velocity");
                add("groovy");
                add("plugin");
                add("wiki");
                add("xwiki");
            }
        });

        assertEquals(velocityFirst, myEngine.renderText(text, document, getContext()));
    }

    public void testRenderWithoutContextDoc()
    {
        String result = getContext().getWiki().getRenderingEngine().interpretText("toto", null, getContext());
        
        Assert.assertEquals("toto", result);
    }
}
