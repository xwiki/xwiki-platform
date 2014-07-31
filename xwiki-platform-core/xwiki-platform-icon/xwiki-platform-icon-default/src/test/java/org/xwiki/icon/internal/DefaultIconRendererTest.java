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
package org.xwiki.icon.internal;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconSet;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.DefaultIconRenderer}.
 *
 * @since 6.2M1
 * @version $Id$
 */
public class DefaultIconRendererTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultIconRenderer> mocker =
            new MockitoComponentMockingRule<>(DefaultIconRenderer.class);

    private Provider<XWikiContext> xcontextProvider;

    private SkinExtension skinExtension;

    private SkinExtension linkExtension;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        skinExtension = mocker.getInstance(SkinExtension.class, "ssx");
        linkExtension = mocker.getInstance(SkinExtension.class, "linkx");
    }

    @Test
    public void render() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setRenderWiki("image:$icon.png");
        iconSet.addIcon(new Icon("test", "blabla"));
        when(xwiki.parseContent("#set($icon = \"blabla\")\nimage:$icon.png", xcontext)).thenReturn("image:blabla.png");

        // Test
        String result = mocker.getComponentUnderTest().render("test", iconSet);

        // Verify
        assertEquals("image:blabla.png", result);
        verify(linkExtension, never()).use(anyString());
        verify(skinExtension, never()).use(anyString());
    }

    @Test
    public void renderWithCSS() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setCss("css");
        iconSet.addIcon(new Icon("test", "blabla"));
        when(xwiki.parseContent("css", xcontext)).thenReturn("velocityParsedCSS");

        // Test
        mocker.getComponentUnderTest().render("test", iconSet);

        // Verify
        Map<String, Object> parameters = new HashMap();
        parameters.put("rel", "stylesheet");
        verify(linkExtension).use(eq("velocityParsedCSS"), eq(parameters));
        verify(skinExtension, never()).use(anyString());
    }

    @Test
    public void renderWithSSX() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setSsx("ssx");
        iconSet.addIcon(new Icon("test", "blabla"));

        // Test
        mocker.getComponentUnderTest().render("test", iconSet);

        // Verify
        verify(skinExtension).use("ssx");
        verify(linkExtension, never()).use(anyString());
    }

    @Test
    public void renderHTML() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setRenderHTML("<img src=\"$icon.png\" />");
        iconSet.addIcon(new Icon("test", "blabla"));

        when(xwiki.parseContent("#set($icon = \"blabla\")\n<img src=\"$icon.png\" />", xcontext))
                .thenReturn("<img src=\"blabla.png\" />");

        // Test
        String result = mocker.getComponentUnderTest().renderHTML("test", iconSet);

        // Verify
        assertEquals("<img src=\"blabla.png\" />", result);
    }

    @Test
    public void renderHTMLWithCSS() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setCss("css");
        iconSet.addIcon(new Icon("test", "blabla"));
        when(xwiki.parseContent("css", xcontext)).thenReturn("velocityParsedCSS");

        // Test
        mocker.getComponentUnderTest().renderHTML("test", iconSet);

        // Verify
        Map<String, Object> parameters = new HashMap();
        parameters.put("rel", "stylesheet");
        verify(linkExtension).use(eq("velocityParsedCSS"), eq(parameters));
        verify(skinExtension, never()).use(anyString());
    }

    @Test
    public void renderHTMLWithSSX() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setSsx("ssx");
        iconSet.addIcon(new Icon("test", "blabla"));

        // Test
        mocker.getComponentUnderTest().renderHTML("test", iconSet);

        // Verify
        verify(skinExtension).use("ssx");
        verify(linkExtension, never()).use(anyString());
    }

}
