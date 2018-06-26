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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconRenderer;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.DefaultIconManager}.
 *
 * @version $Id$
 * @since 6.2M1
 */
public class DefaultIconManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultIconManager> mocker =
        new MockitoComponentMockingRule<>(DefaultIconManager.class);

    private IconSetManager iconSetManager;

    private IconRenderer iconRenderer;

    @Before
    public void setUp() throws Exception
    {
        iconSetManager = mocker.getInstance(IconSetManager.class);
        iconRenderer = mocker.getInstance(IconRenderer.class);
    }

    @Test
    public void render() throws Exception
    {
        IconSet iconSet = new IconSet("silk");
        iconSet.addIcon("test", new Icon("hello"));
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);
        when(iconSetManager.getIconSet("silk")).thenReturn(iconSet);
        when(iconRenderer.render("test", iconSet)).thenReturn("rendered icon");

        // Test
        String result = mocker.getComponentUnderTest().render("test");
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
        String result = mocker.getComponentUnderTest().render("test");
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
        String result = mocker.getComponentUnderTest().renderHTML("test");
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
        String result = mocker.getComponentUnderTest().renderHTML("test");
        assertEquals("rendered icon", result);
    }

    @Test
    public void renderCustom() throws Exception
    {
        IconSet iconSet = new IconSet("silk");
        iconSet.addIcon("test", new Icon("hello"));
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);
        when(iconSetManager.getIconSet("silk")).thenReturn(iconSet);
        when(iconRenderer.renderCustom("test", iconSet)).thenReturn("rendered icon");

        // Test
        String result = mocker.getComponentUnderTest().renderCustom("test");
        assertEquals("rendered icon", result);
    }

    @Test
    public void renderCustomWithFallBack() throws Exception
    {
        IconSet iconSet = new IconSet("silk");
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);

        IconSet defaultIconSet = new IconSet("default");
        when(iconSetManager.getDefaultIconSet()).thenReturn(defaultIconSet);
        when(iconRenderer.renderCustom("test", defaultIconSet)).thenReturn("rendered icon");

        // Test
        String result = mocker.getComponentUnderTest().renderCustom("test");
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
        when(iconRenderer.renderCustom("icon1", iconSet1)).thenReturn("Custom rendered icon 1");
        when(iconRenderer.renderCustom("icon2", iconSet1)).thenReturn("");

        IconSet defaultIconSet = new IconSet("default");
        defaultIconSet.addIcon("icon2", new Icon("icon"));
        when(iconSetManager.getDefaultIconSet()).thenReturn(defaultIconSet);
        when(iconRenderer.render("icon1", defaultIconSet)).thenReturn("default rendered icon 1");
        when(iconRenderer.render("icon2", defaultIconSet)).thenReturn("default rendered icon 2");
        when(iconRenderer.renderHTML("icon1", defaultIconSet)).thenReturn("HTML default rendered icon 1");
        when(iconRenderer.renderHTML("icon2", defaultIconSet)).thenReturn("HTML default rendered icon 2");
        when(iconRenderer.renderCustom("icon1", defaultIconSet)).thenReturn("Custom default rendered icon 1");
        when(iconRenderer.renderCustom("icon2", defaultIconSet)).thenReturn("Custom default rendered icon 2");

        // Tests
        assertEquals("rendered icon 1", mocker.getComponentUnderTest().render("icon1", "iconSet1"));
        assertEquals("default rendered icon 2", mocker.getComponentUnderTest().render("icon2", "iconSet1"));
        assertEquals("default rendered icon 1", mocker.getComponentUnderTest().render("icon1", "iconSet2"));
        assertEquals("rendered icon 1", mocker.getComponentUnderTest().render("icon1", "iconSet1", true));
        assertEquals("default rendered icon 2", mocker.getComponentUnderTest().render("icon2", "iconSet1", true));
        assertEquals("default rendered icon 1", mocker.getComponentUnderTest().render("icon1", "iconSet2", true));
        assertEquals("rendered icon 1", mocker.getComponentUnderTest().render("icon1", "iconSet1", false));
        assertEquals("", mocker.getComponentUnderTest().render("icon2", "iconSet1", false));
        assertEquals("", mocker.getComponentUnderTest().render("icon1", "iconSet2", false));

        assertEquals("HTML rendered icon 1", mocker.getComponentUnderTest().renderHTML("icon1", "iconSet1"));
        assertEquals("HTML default rendered icon 2", mocker.getComponentUnderTest().renderHTML("icon2", "iconSet1"));
        assertEquals("HTML default rendered icon 1", mocker.getComponentUnderTest().renderHTML("icon1", "iconSet2"));
        assertEquals("HTML rendered icon 1", mocker.getComponentUnderTest().renderHTML("icon1", "iconSet1", true));
        assertEquals("HTML default rendered icon 2",
            mocker.getComponentUnderTest().renderHTML("icon2", "iconSet1", true));
        assertEquals("HTML default rendered icon 1",
            mocker.getComponentUnderTest().renderHTML("icon1", "iconSet2", true));
        assertEquals("HTML rendered icon 1", mocker.getComponentUnderTest().renderHTML("icon1", "iconSet1", false));
        assertEquals("", mocker.getComponentUnderTest().renderHTML("icon2", "iconSet1", false));
        assertEquals("", mocker.getComponentUnderTest().renderHTML("icon1", "iconSet2", false));

        assertEquals("Custom rendered icon 1", mocker.getComponentUnderTest().renderCustom("icon1", "iconSet1"));
        assertEquals("Custom default rendered icon 2",
            mocker.getComponentUnderTest().renderCustom("icon2", "iconSet1"));
        assertEquals("Custom default rendered icon 1",
            mocker.getComponentUnderTest().renderCustom("icon1", "iconSet2"));
        assertEquals("Custom rendered icon 1", mocker.getComponentUnderTest().renderCustom("icon1", "iconSet1", true));
        assertEquals("Custom default rendered icon 2",
            mocker.getComponentUnderTest().renderCustom("icon2", "iconSet1", true));
        assertEquals("Custom default rendered icon 1",
            mocker.getComponentUnderTest().renderCustom("icon1", "iconSet2", true));
        assertEquals("Custom rendered icon 1", mocker.getComponentUnderTest().renderCustom("icon1", "iconSet1", false));
        assertEquals("", mocker.getComponentUnderTest().renderCustom("icon2", "iconSet1", false));
        assertEquals("", mocker.getComponentUnderTest().renderCustom("icon1", "iconSet2", false));
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
        List<String> results = mocker.getComponentUnderTest().getIconNames();
        List<String> results2 = mocker.getComponentUnderTest().getIconNames("iconSet1");

        // Verify
        assertEquals(2, results.size());
        assertTrue(results.contains("icon1"));
        assertTrue(results.contains("icon2"));
        assertEquals(2, results2.size());
        assertTrue(results2.contains("icon1"));
        assertTrue(results2.contains("icon2"));
    }

    @Test
    public void getIconSet() throws Exception
    {
        IconSet iconSet = new IconSet("silk");
        iconSet.addIcon("test", new Icon("hello"));
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);
        when(iconSetManager.getIconSet("silk")).thenReturn(iconSet);

        // Test
        assertSame(iconSet, mocker.getComponentUnderTest().getIconSet("test"));
    }

    @Test
    public void getIconSetWithIconSetName() throws Exception
    {
        IconSet iconSet = new IconSet("silk");
        iconSet.addIcon("test1", new Icon("hello1"));
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);
        when(iconSetManager.getIconSet("silk")).thenReturn(iconSet);

        IconSet defaultIconSet = new IconSet("default");
        defaultIconSet.addIcon("test2", new Icon("hello2"));
        when(iconSetManager.getDefaultIconSet()).thenReturn(defaultIconSet);

        // Test
        assertSame(iconSet, mocker.getComponentUnderTest().getIconSet("test1", "silk"));
        assertSame(defaultIconSet, mocker.getComponentUnderTest().getIconSet("test2", "silk"));
        assertSame(defaultIconSet, mocker.getComponentUnderTest().getIconSet("test1", "font-awesome"));
    }

    @Test
    public void getIconSetWithFallBack() throws Exception
    {
        IconSet iconSet = new IconSet("silk");
        iconSet.addIcon("test1", new Icon("hello1"));
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);
        when(iconSetManager.getIconSet("silk")).thenReturn(iconSet);

        IconSet defaultIconSet = new IconSet("default");
        defaultIconSet.addIcon("test2", new Icon("hello2"));
        when(iconSetManager.getDefaultIconSet()).thenReturn(defaultIconSet);

        // Test
        assertSame(iconSet, mocker.getComponentUnderTest().getIconSet("test1", "silk", true));
        assertSame(iconSet, mocker.getComponentUnderTest().getIconSet("test1", "silk", false));
        assertSame(defaultIconSet, mocker.getComponentUnderTest().getIconSet("test2", "silk", true));
        assertNull(mocker.getComponentUnderTest().getIconSet("test2", "silk", false));
        assertSame(defaultIconSet, mocker.getComponentUnderTest().getIconSet("test1", "font-awesome", true));
        assertNull(mocker.getComponentUnderTest().getIconSet("test1", "font-awesome", false));
    }
}
