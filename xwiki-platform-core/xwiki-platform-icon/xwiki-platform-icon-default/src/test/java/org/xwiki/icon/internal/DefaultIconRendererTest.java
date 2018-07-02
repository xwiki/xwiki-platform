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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.DefaultIconRenderer}.
 *
 * @version $Id$
 * @since 6.2M1
 */
@ComponentTest
public class DefaultIconRendererTest
{
    @InjectMockComponents
    private DefaultIconRenderer iconRenderer;

    @MockComponent
    @Named("ssx")
    private SkinExtension skinExtension;

    @MockComponent
    @Named("linkx")
    private SkinExtension linkExtension;

    @MockComponent
    @Named("jsx")
    private SkinExtension jsExtension;

    @MockComponent
    private VelocityRenderer velocityRenderer;

    @Test
    public void render() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setRenderWiki("image:$icon.png");
        iconSet.addIcon("test", new Icon("blabla"));
        when(velocityRenderer.render("#set($icon = \"blabla\")\nimage:$icon.png")).thenReturn("image:blabla.png");

        // Test
        String result = iconRenderer.render("test", iconSet);

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
        iconSet.setRenderWiki("image:$icon.png");
        iconSet.setCss("css");
        iconSet.addIcon("test", new Icon("blabla"));
        when(velocityRenderer.render("css")).thenReturn("velocityParsedCSS");

        // Test
        iconRenderer.render("test", iconSet);

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
        iconSet.setRenderWiki("image:$icon.png");
        iconSet.setSsx("ssx");
        iconSet.addIcon("test", new Icon("blabla"));

        // Test
        iconRenderer.render("test", iconSet);

        // Verify
        verify(skinExtension).use("ssx");
        verify(linkExtension, never()).use(any());
        verify(jsExtension, never()).use(any());
    }

    @Test
    public void renderWithJSX() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setRenderWiki("image:$icon.png");
        iconSet.setJsx("jsx");
        iconSet.addIcon("test", new Icon("blabla"));

        // Test
        iconRenderer.render("test", iconSet);

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
        String result = iconRenderer.renderHTML("test", iconSet);

        // Verify
        assertEquals("<img src=\"blabla.png\" />", result);
    }

    @Test
    public void renderHTMLWithCSS() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setRenderHTML("<img src=\"$icon.png\" />");
        iconSet.setCss("css");
        iconSet.addIcon("test", new Icon("blabla"));
        when(velocityRenderer.render("css")).thenReturn("velocityParsedCSS");

        // Test
        iconRenderer.renderHTML("test", iconSet);

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
        iconSet.setRenderHTML("<img src=\"$icon.png\" />");
        iconSet.setSsx("ssx");
        iconSet.addIcon("test", new Icon("blabla"));

        // Test
        iconRenderer.renderHTML("test", iconSet);

        // Verify
        verify(skinExtension).use("ssx");
        verify(linkExtension, never()).use(any());
        verify(jsExtension, never()).use(any());
    }

    @Test
    public void renderHTMLWithJSX() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setRenderHTML("<img src=\"$icon.png\" />");
        iconSet.setJsx("jsx");
        iconSet.addIcon("test", new Icon("blabla"));

        // Test
        iconRenderer.renderHTML("test", iconSet);

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
        String result = iconRenderer.render("non-existent-icon", iconSet);

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
            iconRenderer.render("test", iconSet);
        } catch (IconException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals(exception, caughtException);
    }

    @Test
    public void renderIcon() throws Exception
    {
        IconSet iconSet = new IconSet("iconSet");
        iconSet.addIcon("test", new Icon("hello"));
        when(velocityRenderer.render("#set($icon = \"hello\")\nfa fa-$icon")).thenReturn("fa fa-hello");

        // Test
        String renderedIcon1 = iconRenderer.render("test", iconSet, "fa fa-$icon");
        String renderedIcon2 = iconRenderer.render("none", iconSet, "fa fa-$icon");
        String renderedIcon3 = iconRenderer.render("none", null, "fa fa-$icon");
        String renderedIcon4 = iconRenderer.render("none", iconSet, null);

        // Verify
        assertEquals("fa fa-hello", renderedIcon1);
        assertEquals("", renderedIcon2);
        assertEquals("", renderedIcon3);
        assertEquals("", renderedIcon4);
    }

    @Test
    public void useWithIconSetNull() throws Exception
    {
        assertThrows(IconException.class, () -> iconRenderer.use(null));
    }
}
