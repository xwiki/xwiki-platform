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
package org.xwiki.uiextension;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.uiextension.internal.filter.ExcludeFilter;
import org.xwiki.uiextension.internal.filter.SortByCustomOrderFilter;
import org.xwiki.uiextension.internal.filter.SortByIdFilter;
import org.xwiki.uiextension.internal.filter.SortByParameterFilter;
import org.xwiki.uiextension.internal.filter.SelectFilter;

public class UIExtensionPointFiltersTest
{
    private UIExtension testUix1valueZ = new UIExtensions.TestUix1valueZ();
    private UIExtension testUix2valueY = new UIExtensions.TestUix2valueY();
    private UIExtension testUix3valueX = new UIExtensions.TestUix3valueX();
    private UIExtension testUix4valueW = new UIExtensions.TestUix4valueW();
    private UIExtension testUix5value1 = new UIExtensions.TestUix5value1();
    private UIExtension testUix6value11 = new UIExtensions.TestUix6value11();
    private UIExtension testUix7value2 = new UIExtensions.TestUix7value2();
    private List<UIExtension> extensions = new ArrayList<UIExtension>();

    @Before
    public void configure()
    {
        extensions.add(testUix3valueX);
        extensions.add(testUix6value11);
        extensions.add(testUix1valueZ);
        extensions.add(testUix5value1);
        extensions.add(testUix4valueW);
        extensions.add(testUix7value2);
        extensions.add(testUix2valueY);
    }

    @Test
    public void excludeFilter()
    {
        String[] list = new String[] {"platform.testuix2", "platform.testuix3"};
        List<UIExtension> expected = new ArrayList<UIExtension>();
        expected.add(testUix6value11);
        expected.add(testUix1valueZ);
        expected.add(testUix5value1);
        expected.add(testUix4valueW);
        expected.add(testUix7value2);

        UIExtensionFilter filter = new ExcludeFilter();

        Assert.assertEquals(expected, filter.filter(extensions, list));
    }

    @Test
    public void selectFilter()
    {
        String[] list = new String[] {"platform.testuix2", "platform.testuix3", "platform.testuix4"};
        List<UIExtension> expected = new ArrayList<UIExtension>();
        // The extensions must be ordered as in the select clause above
        expected.add(testUix2valueY);
        expected.add(testUix3valueX);
        expected.add(testUix4valueW);

        UIExtensionFilter filter = new SelectFilter();

        Assert.assertEquals(expected, filter.filter(extensions, list));
    }

    @Test
    public void sortByListFilter()
    {
        String[] list = new String[] {"platform.testuix2", "platform.testuix3"};

        List<UIExtension> expected = new ArrayList<UIExtension>();
        // The first 2 are placed at the beginning, in the correct order
        expected.add(testUix2valueY);
        expected.add(testUix3valueX);
        // The order of the others is preserved
        expected.add(testUix6value11);
        expected.add(testUix1valueZ);
        expected.add(testUix5value1);
        expected.add(testUix4valueW);
        expected.add(testUix7value2);

        UIExtensionFilter filter = new SortByCustomOrderFilter();

        Assert.assertEquals(expected, filter.filter(extensions, list));
    }

    @Test
    public void sortByNameFilter()
    {
        List<UIExtension> expected = new ArrayList<UIExtension>();
        expected.add(testUix1valueZ);
        expected.add(testUix2valueY);
        expected.add(testUix3valueX);
        expected.add(testUix4valueW);
        expected.add(testUix5value1);
        expected.add(testUix6value11);
        expected.add(testUix7value2);


        UIExtensionFilter filter = new SortByIdFilter();

        Assert.assertEquals(expected, filter.filter(extensions));
    }

    @Test
    public void sortByParameterFilter()
    {
        List<UIExtension> expected = new ArrayList<UIExtension>();
        expected.add(testUix5value1);
        expected.add(testUix7value2);
        expected.add(testUix6value11);
        expected.add(testUix4valueW);
        expected.add(testUix3valueX);
        expected.add(testUix2valueY);
        expected.add(testUix1valueZ);

        UIExtensionFilter filter = new SortByParameterFilter();

        Assert.assertEquals(expected, filter.filter(extensions, "key"));
    }

}
