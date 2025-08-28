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
import static org.mockito.Mockito.mock;
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
class DefaultIconRendererTest
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
    private IconTemplateRendererManager rendererManager;

    @Test
    void render() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        String template = "image:$icon.png";
        iconSet.setRenderWiki(template);
        iconSet.addIcon("test", new Icon("blabla"));

        setupSimpleTemplate(template);

        // Test
        String result = this.iconRenderer.render("test", iconSet);

        // Verify
        assertEquals("image:blabla.png", result);
        verify(this.linkExtension, never()).use(any());
        verify(this.skinExtension, never()).use(any());
        verify(this.jsExtension, never()).use(any());
    }

    private void setupSimpleTemplate(String template) throws IconException
    {
        when(this.rendererManager.getRenderer(template)).thenReturn(
            (icon, documentReference) -> template.replace("$icon", icon));
    }

    @Test
    void renderWithCSS() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        String template = "image:$icon.png";
        iconSet.setRenderWiki(template);
        iconSet.setCss("css");
        iconSet.addIcon("test", new Icon("blabla"));

        IconTemplateRenderer mockRenderer = mock();
        when(this.rendererManager.getRenderer("css")).thenReturn(mockRenderer);
        String parsedCSS = "velocityParsedCSS";
        when(mockRenderer.render(null, null)).thenReturn(parsedCSS);

        setupSimpleTemplate(template);

        // Test
        this.iconRenderer.render("test", iconSet);

        // Verify
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("rel", "stylesheet");
        verify(this.linkExtension).use(parsedCSS, parameters);
        verify(this.skinExtension, never()).use(any());
        verify(this.jsExtension, never()).use(any());
    }

    @Test
    void renderWithSSX() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        String template = "image:$icon.png";
        iconSet.setRenderWiki(template);
        iconSet.setSsx("ssx");
        iconSet.addIcon("test", new Icon("blabla"));

        setupSimpleTemplate(template);

        // Test
        this.iconRenderer.render("test", iconSet);

        // Verify
        verify(this.skinExtension).use("ssx");
        verify(this.linkExtension, never()).use(any());
        verify(this.jsExtension, never()).use(any());
    }

    @Test
    void renderWithJSX() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        String template = "image:$icon.png";
        iconSet.setRenderWiki(template);
        iconSet.setJsx("jsx");
        iconSet.addIcon("test", new Icon("blabla"));
        setupSimpleTemplate(template);

        // Test
        this.iconRenderer.render("test", iconSet);

        // Verify
        verify(this.jsExtension).use("jsx");
        verify(this.linkExtension, never()).use(any());
        verify(this.skinExtension, never()).use(any());
    }

    @Test
    void renderHTML() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        String template = "<img src=\"$icon.png\" />";
        iconSet.setRenderHTML(template);
        iconSet.addIcon("test", new Icon("blabla"));

        setupSimpleTemplate(template);

        // Test
        String result = this.iconRenderer.renderHTML("test", iconSet);

        // Verify
        assertEquals("<img src=\"blabla.png\" />", result);
    }

    @Test
    void renderHTMLWithCSS() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        String template = "<img src=\"$icon.png\" />";
        iconSet.setRenderHTML(template);
        iconSet.setCss("css");
        iconSet.addIcon("test", new Icon("blabla"));

        IconTemplateRenderer mockRenderer = mock();
        when(this.rendererManager.getRenderer("css")).thenReturn(mockRenderer);
        String parsedCSS = "velocityParsedCSS";
        when(mockRenderer.render(null, null)).thenReturn(parsedCSS);

        setupSimpleTemplate(template);

        // Test
        this.iconRenderer.renderHTML("test", iconSet);

        // Verify
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("rel", "stylesheet");
        verify(this.linkExtension).use(parsedCSS, parameters);
        verify(this.skinExtension, never()).use(any());
        verify(this.jsExtension, never()).use(any());
    }

    @Test
    void renderHTMLWithSSX() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        String template = "<img src=\"$icon.png\" />";
        iconSet.setRenderHTML(template);
        iconSet.setSsx("ssx");
        iconSet.addIcon("test", new Icon("blabla"));

        setupSimpleTemplate(template);

        // Test
        this.iconRenderer.renderHTML("test", iconSet);

        // Verify
        verify(this.skinExtension).use("ssx");
        verify(this.linkExtension, never()).use(any());
        verify(this.jsExtension, never()).use(any());
    }

    @Test
    void renderHTMLWithJSX() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        String template = "<img src=\"$icon.png\" />";
        iconSet.setRenderHTML(template);
        iconSet.setJsx("jsx");
        iconSet.addIcon("test", new Icon("blabla"));

        setupSimpleTemplate(template);

        // Test
        this.iconRenderer.renderHTML("test", iconSet);

        // Verify
        verify(this.jsExtension).use("jsx");
        verify(this.linkExtension, never()).use(any());
        verify(this.skinExtension, never()).use(any());
    }

    @Test
    void renderNonExistentIcon() throws Exception
    {
        IconSet iconSet = new IconSet("default");

        // Test
        String result = this.iconRenderer.render("non-existent-icon", iconSet);

        // Verify
        assertEquals("", result);
    }

    @Test
    void renderWithException() throws Exception
    {
        IconSet iconSet = new IconSet("default");
        iconSet.setRenderWiki("image:$icon.png");
        iconSet.addIcon("test", new Icon("blabla"));
        IconException exception = new IconException("exception");
        IconTemplateRenderer mockRenderer = mock();
        when(this.rendererManager.getRenderer(any())).thenReturn(mockRenderer);
        when(mockRenderer.render(any(), any())).thenThrow(exception);

        // Test
        IconException caughtException = null;
        try {
            this.iconRenderer.render("test", iconSet);
        } catch (IconException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals(exception, caughtException);
    }

    @Test
    void renderIcon() throws Exception
    {
        IconSet iconSet = new IconSet("iconSet");
        iconSet.addIcon("test", new Icon("hello"));
        String template = "fa fa-$icon";
        setupSimpleTemplate(template);

        // Test
        String renderedIcon1 = this.iconRenderer.render("test", iconSet, template);
        String renderedIcon2 = this.iconRenderer.render("none", iconSet, template);
        String renderedIcon3 = this.iconRenderer.render("none", null, template);
        String renderedIcon4 = this.iconRenderer.render("none", iconSet, null);

        // Verify
        assertEquals("fa fa-hello", renderedIcon1);
        assertEquals("", renderedIcon2);
        assertEquals("", renderedIcon3);
        assertEquals("", renderedIcon4);
    }

    @Test
    void useWithIconSetNull()
    {
        assertThrows(IconException.class, () -> this.iconRenderer.use(null));
    }
}
