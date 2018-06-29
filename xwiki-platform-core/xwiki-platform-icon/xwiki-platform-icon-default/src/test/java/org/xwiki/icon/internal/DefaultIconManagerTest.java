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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconManager;
import org.xwiki.icon.IconRenderer;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.icon.IconType;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.DefaultIconManager}.
 *
 * @version $Id$
 * @since 6.2M1
 */
@ComponentTest
public class DefaultIconManagerTest
{
    @InjectMockComponents
    private DefaultIconManager iconManager;

    @MockComponent
    private IconSetManager iconSetManager;

    @MockComponent
    private IconRenderer iconRenderer;

    @Test
    public void render() throws Exception
    {
        IconSet iconSet = new IconSet("silk");
        iconSet.addIcon("test", new Icon("hello"));
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);
        when(iconSetManager.getIconSet("silk")).thenReturn(iconSet);
        when(iconRenderer.render("test", iconSet)).thenReturn("rendered icon");

        // Test
        String result = iconManager.render("test");
        assertEquals("rendered icon", result);
    }

    @Test
    public void renderWithFallBack() throws Exception
    {
        IconSet iconSet = new IconSet("silk");
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);

        IconSet defaultIconSet = new IconSet("default");
        when(iconSetManager.getDefaultIconSet()).thenReturn(defaultIconSet);
        when(iconRenderer.render("test", defaultIconSet)).thenReturn("rendered icon");

        // Test
        String result = iconManager.render("test");
        assertEquals("rendered icon", result);
    }

    @Test
    public void renderHTML() throws Exception
    {
        IconSet iconSet = new IconSet("silk");
        iconSet.addIcon("test", new Icon("hello"));
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);
        when(iconSetManager.getIconSet("silk")).thenReturn(iconSet);
        when(iconRenderer.renderHTML("test", iconSet)).thenReturn("rendered icon");

        // Test
        String result = iconManager.renderHTML("test");
        assertEquals("rendered icon", result);
    }

    @Test
    public void renderHTMLWithFallBack() throws Exception
    {
        IconSet iconSet = new IconSet("silk");
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);

        IconSet defaultIconSet = new IconSet("default");
        when(iconSetManager.getDefaultIconSet()).thenReturn(defaultIconSet);
        when(iconRenderer.renderHTML("test", defaultIconSet)).thenReturn("rendered icon");

        // Test
        String result = iconManager.renderHTML("test");
        assertEquals("rendered icon", result);
    }

    @Test
    public void renderWithIconSetName() throws Exception
    {
        // Mocks
        IconSet iconSet1 = new IconSet("iconSet1");
        iconSet1.addIcon("icon1", new Icon("icon"));
        when(iconSetManager.getIconSet("iconSet1")).thenReturn(iconSet1);
        when(iconRenderer.render("icon1", iconSet1)).thenReturn("rendered icon 1");
        when(iconRenderer.render("icon2", iconSet1)).thenReturn("");
        when(iconRenderer.renderHTML("icon1", iconSet1)).thenReturn("HTML rendered icon 1");
        when(iconRenderer.renderHTML("icon2", iconSet1)).thenReturn("");

        IconSet defaultIconSet = new IconSet("default");
        defaultIconSet.addIcon("icon2", new Icon("icon"));
        when(iconSetManager.getDefaultIconSet()).thenReturn(defaultIconSet);
        when(iconRenderer.render("icon1", defaultIconSet)).thenReturn("default rendered icon 1");
        when(iconRenderer.render("icon2", defaultIconSet)).thenReturn("default rendered icon 2");
        when(iconRenderer.renderHTML("icon1", defaultIconSet)).thenReturn("HTML default rendered icon 1");
        when(iconRenderer.renderHTML("icon2", defaultIconSet)).thenReturn("HTML default rendered icon 2");

        // Tests
        assertEquals("rendered icon 1", iconManager.render("icon1", "iconSet1"));
        assertEquals("default rendered icon 2", iconManager.render("icon2", "iconSet1"));
        assertEquals("default rendered icon 1", iconManager.render("icon1", "iconSet2"));
        assertEquals("rendered icon 1", iconManager.render("icon1", "iconSet1", true));
        assertEquals("default rendered icon 2", iconManager.render("icon2", "iconSet1", true));
        assertEquals("default rendered icon 1", iconManager.render("icon1", "iconSet2", true));
        assertEquals("rendered icon 1", iconManager.render("icon1", "iconSet1", false));
        assertEquals("", iconManager.render("icon2", "iconSet1", false));
        assertEquals("", iconManager.render("icon1", "iconSet2", false));

        assertEquals("HTML rendered icon 1", iconManager.renderHTML("icon1", "iconSet1"));
        assertEquals("HTML default rendered icon 2", iconManager.renderHTML("icon2", "iconSet1"));
        assertEquals("HTML default rendered icon 1", iconManager.renderHTML("icon1", "iconSet2"));
        assertEquals("HTML rendered icon 1", iconManager.renderHTML("icon1", "iconSet1", true));
        assertEquals("HTML default rendered icon 2",
            iconManager.renderHTML("icon2", "iconSet1", true));
        assertEquals("HTML default rendered icon 1",
            iconManager.renderHTML("icon1", "iconSet2", true));
        assertEquals("HTML rendered icon 1", iconManager.renderHTML("icon1", "iconSet1", false));
        assertEquals("", iconManager.renderHTML("icon2", "iconSet1", false));
        assertEquals("", iconManager.renderHTML("icon1", "iconSet2", false));
    }

    @Test
    public void getIconNames() throws Exception
    {
        IconSet iconSet = new IconSet("iconSet1");
        iconSet.addIcon("icon1", new Icon("icon1 value"));
        iconSet.addIcon("icon2", new Icon("icon2 value"));

        // Mocks
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);
        when(iconSetManager.getIconSet("iconSet1")).thenReturn(iconSet);

        // Test
        List<String> results = iconManager.getIconNames();
        List<String> results2 = iconManager.getIconNames("iconSet1");

        // Verify
        assertEquals(2, results.size());
        assertTrue(results.contains("icon1"));
        assertTrue(results.contains("icon2"));
        assertEquals(2, results2.size());
        assertTrue(results2.contains("icon1"));
        assertTrue(results2.contains("icon2"));
    }

    @Test
    public void getMetaData() throws Exception
    {
        IconSet iconSet = new IconSet("iconSet");
        iconSet.setType(IconType.FONT);
        iconSet.setUrl("http://url_to_image/$icon.png");
        iconSet.setCssClass("fa fa-$icon");
        iconSet.addIcon("test", new Icon("hello"));

        // Mocks
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);
        when(iconSetManager.getIconSet("iconSet")).thenReturn(iconSet);
        when(iconRenderer.renderIcon("test", iconSet, "fa fa-$icon")).thenReturn("fa fa-hello");
        when(iconRenderer.renderIcon("test", iconSet, "http://url_to_image/$icon.png"))
            .thenReturn("http://url_to_image/hello.png");

        // Test
        Map<String, Object> metadata = iconManager.getMetaData("test");

        // Verify
        assertEquals("iconSet", metadata.get(IconManager.META_DATA_ICON_SET_NAME));
        assertEquals("FONT", metadata.get(IconManager.META_DATA_ICON_SET_TYPE));
        assertEquals("http://url_to_image/hello.png", metadata.get(IconManager.META_DATA_URL));
        assertEquals("fa fa-hello", metadata.get(IconManager.META_DATA_CSS_CLASS));
        assertEquals(metadata, iconManager.getMetaData("test", "iconSet"));
    }

    @Test
    public void getMetaDataWithFallback() throws Exception
    {
        IconSet iconSet = new IconSet("iconSet");

        IconSet defaultIconSet = new IconSet("default");
        defaultIconSet.setType(IconType.IMAGE);
        defaultIconSet.setCssClass("fa fa-$icon");
        defaultIconSet.addIcon("test", new Icon("hello"));

        // Mocks
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);
        when(iconSetManager.getDefaultIconSet()).thenReturn(defaultIconSet);
        when(iconSetManager.getIconSet("iconSet")).thenReturn(iconSet);
        when(iconRenderer.renderIcon("test", defaultIconSet, "fa fa-$icon")).thenReturn("fa fa-hello");

        // Test
        Map<String, Object> metadata = iconManager.getMetaData("test", "iconSet", true);

        // Verify
        assertEquals("default", metadata.get(IconManager.META_DATA_ICON_SET_NAME));
        assertEquals("IMAGE", metadata.get(IconManager.META_DATA_ICON_SET_TYPE));
        assertNull(metadata.get(IconManager.META_DATA_URL));
        assertEquals("fa fa-hello", metadata.get(IconManager.META_DATA_CSS_CLASS));
    }

    @Test
    public void getMetaDataWithoutFallback() throws Exception
    {
        // Test
        Map<String, Object> metadata = iconManager.getMetaData("test", "iconSet", false);

        // Verify
        assertTrue(metadata.isEmpty());
    }
}
