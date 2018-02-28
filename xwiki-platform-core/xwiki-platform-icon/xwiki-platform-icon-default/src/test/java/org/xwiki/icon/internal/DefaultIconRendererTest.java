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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private SkinExtension skinExtension;

    private SkinExtension linkExtension;

    private SkinExtension jsExtension;

    private VelocityRenderer velocityRenderer;

    @Before
    public void setUp() throws Exception
    {
        skinExtension = mocker.getInstance(SkinExtension.class, "ssx");
        linkExtension = mocker.getInstance(SkinExtension.class, "linkx");
        jsExtension = mocker.getInstance(SkinExtension.class, "jsx");
        velocityRenderer = mocker.getInstance(VelocityRenderer.class);
    }

    @Test
    public void render() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setRenderWiki("image:$icon.png");
        iconSet.addIcon("test", new Icon("blabla"));
        when(velocityRenderer.render("#set($icon = \"blabla\")\nimage:$icon.png")).thenReturn("image:blabla.png");

        // Test
        String result = mocker.getComponentUnderTest().render("test", iconSet);

        // Verify
        assertEquals("image:blabla.png", result);
        verify(linkExtension, never()).use(any());
        verify(skinExtension, never()).use(any());
        verify(jsExtension, never()).use(any());
    }

    @Test
    public void renderWithCSS() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setCss("css");
        iconSet.addIcon("test", new Icon("blabla"));
        when(velocityRenderer.render("css")).thenReturn("velocityParsedCSS");

        // Test
        mocker.getComponentUnderTest().render("test", iconSet);

        // Verify
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("rel", "stylesheet");
        verify(linkExtension).use(eq("velocityParsedCSS"), eq(parameters));
        verify(skinExtension, never()).use(any());
        verify(jsExtension, never()).use(any());
    }

    @Test
    public void renderWithSSX() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setSsx("ssx");
        iconSet.addIcon("test", new Icon("blabla"));

        // Test
        mocker.getComponentUnderTest().render("test", iconSet);

        // Verify
        verify(skinExtension).use("ssx");
        verify(linkExtension, never()).use(any());
        verify(jsExtension, never()).use(any());
    }

    @Test
    public void renderWithJSX() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setJsx("jsx");
        iconSet.addIcon("test", new Icon("blabla"));

        // Test
        mocker.getComponentUnderTest().render("test", iconSet);

        // Verify
        verify(jsExtension).use("jsx");
        verify(linkExtension, never()).use(any());
        verify(skinExtension, never()).use(any());
    }

    @Test
    public void renderHTML() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setRenderHTML("<img src=\"$icon.png\" />");
        iconSet.addIcon("test", new Icon("blabla"));

        when(velocityRenderer.render("#set($icon = \"blabla\")\n<img src=\"$icon.png\" />"))
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
        iconSet.addIcon("test", new Icon("blabla"));
        when(velocityRenderer.render("css")).thenReturn("velocityParsedCSS");

        // Test
        mocker.getComponentUnderTest().renderHTML("test", iconSet);

        // Verify
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("rel", "stylesheet");
        verify(linkExtension).use(eq("velocityParsedCSS"), eq(parameters));
        verify(skinExtension, never()).use(any());
        verify(jsExtension, never()).use(any());
    }

    @Test
    public void renderHTMLWithSSX() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setSsx("ssx");
        iconSet.addIcon("test", new Icon("blabla"));

        // Test
        mocker.getComponentUnderTest().renderHTML("test", iconSet);

        // Verify
        verify(skinExtension).use("ssx");
        verify(linkExtension, never()).use(any());
        verify(jsExtension, never()).use(any());
    }

    @Test
    public void renderHTMLWithJSX() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setJsx("jsx");
        iconSet.addIcon("test", new Icon("blabla"));

        // Test
        mocker.getComponentUnderTest().renderHTML("test", iconSet);

        // Verify
        verify(jsExtension).use("jsx");
        verify(linkExtension, never()).use(any());
        verify(skinExtension, never()).use(any());
    }

    @Test
    public void renderNonExistentIcon() throws Exception
    {
        IconSet iconSet = new IconSet("default");

        // Test
        String result = mocker.getComponentUnderTest().render("non-existent-icon", iconSet);

        // Verify
        assertEquals("", result);
    }

    @Test
    public void renderWithException() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setRenderWiki("image:$icon.png");
        iconSet.addIcon("test", new Icon("blabla"));
        IconException exception = new IconException("exception", null);
        when(velocityRenderer.render(any())).thenThrow(exception);

        // Test
        IconException caughtException = null;
        try {
            mocker.getComponentUnderTest().render("test", iconSet);
        } catch (IconException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals(exception, caughtException);
    }

}
