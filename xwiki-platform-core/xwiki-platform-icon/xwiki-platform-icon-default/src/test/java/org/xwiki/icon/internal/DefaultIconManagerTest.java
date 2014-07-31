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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconRenderer;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.DefaultIconManager}.
 *
 * @since 6.2M1
 * @version $Id$
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
        iconSet.addIcon(new Icon("test", "hello"));
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);
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
        iconSet.addIcon(new Icon("test", "hello"));
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);
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
}
