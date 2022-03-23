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
package com.xpn.xwiki.objects.classes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ListClass}.
 * 
 * @version $Id$
 */
class ListClassTest
{
    /**
     * Test that the default separator {@link ListClass#DEFAULT_SEPARATOR} is used when not specified.
     */
    @Test
    void getListFromStringDefaultSeparator()
    {
        assertEquals(Arrays.asList("a", "b", "c"), ListClass.getListFromString("a|b|c"));
    }

    /**
     * Test that the separator can be escaped inside the list item.
     */
    @Test
    void getListFromStringSeparatorInValues()
    {
        assertEquals(Arrays.asList("a", "|b", "c|", "|"), ListClass.getListFromString("a|\\|b|c\\||\\|"));
    }

    /**
     * Test that custom separators work.
     */
    @Test
    void getListFromStringNonDefaultSeparator()
    {
        assertEquals(Arrays.asList("a", "b", "c"), ListClass.getListFromString("a*b*c", "*", false));
    }

    /**
     * Test that we can use more than one separator.
     */
    @Test
    void getListFromStringMultipleSeparators()
    {
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), ListClass.getListFromString("a*b,c,d*e", "*,", false));
    }

    /**
     * Test the behaviour when multiple separators are used in concatenation.
     */
    @Test
    void getListFromStringConcatenatedSeparators()
    {
        assertEquals(Arrays.asList("a", "b"), ListClass.getListFromString("a, b", ", ", false));
        assertEquals(Arrays.asList("a", "b"), ListClass.getListFromString("a ,b", ", ", false));
        assertEquals(Arrays.asList("a", "b"), ListClass.getListFromString("a , b", ", ", false));
        assertEquals(Arrays.asList("a", "b"), ListClass.getListFromString("a  b", ", ", false));
        assertEquals(Arrays.asList("a", "b"), ListClass.getListFromString("a    b", ", ", false));
        assertEquals(Arrays.asList("a", "", "b"), ListClass.getListFromString("a,,b", ", ", false));
        assertEquals(Arrays.asList("a", "b"), ListClass.getListFromString("a,|,b", "|, ", false));
        assertEquals(Arrays.asList("a", "", "b"), ListClass.getListFromString("a,||b", "|, ", false));
    }

    /**
     * Test that escaped separators in list values work with multipel separators as well.
     */
    @Test
    void getListFromStringMultipleSeparatorsWithSeparatorsInValues()
    {
        assertEquals(Arrays.asList("a*b", "c,d", "e*f"),
            ListClass.getListFromString("a\\*b,c\\,d*e\\*f", "*,", false));
    }

    /**
     * Test that the default separator {@link ListClass#DEFAULT_SEPARATOR} is used when not specified.
     */
    @Test
    void getStringFromListDefaultSeparator()
    {
        assertEquals("a|b|c", ListClass.getStringFromList(Arrays.asList("a", "b", "c")));
    }

    /**
     * Test that the separator can be escaped inside the list item.
     */
    @Test
    void getStringFromListSeparatorInValues()
    {
        assertEquals("a|\\|b|c\\||\\|", ListClass.getStringFromList(Arrays.asList("a", "|b", "c|", "|")));
    }

    /**
     * Test that custom separators work.
     */
    @Test
    void getStringFromListNonDefaultSeparator()
    {
        assertEquals("a*b*c", ListClass.getStringFromList(Arrays.asList("a", "b", "c"), "*"));
    }

    /**
     * Test that we can use more than one separator.
     */
    @Test
    void getStringFromListMultipleSeparators()
    {
        assertEquals("a*b*c*d*e", ListClass.getStringFromList(Arrays.asList("a", "b", "c", "d", "e"), "*,"));
    }

    /**
     * Test that escaped separators in list values work with multipel separators as well.
     */
    @Test
    void getStringFromListMultipleSeparatorsWithSeparatorsInValues()
    {
        assertEquals("a\\*b*c\\,d*e\\*f", ListClass.getStringFromList(Arrays.asList("a*b", "c,d", "e*f"), "*,"));
    }

    @Test
    void getStringFromListWithNullValue()
    {
        assertEquals("a.c", ListClass.getStringFromList(Arrays.asList("a", null, "c"), "."));
        assertEquals("a..c", ListClass.getStringFromList(Arrays.asList("a", "", "c"), "."));
    }

    @Test
    void getStringFromListFinalEmptyValue()
    {
        assertEquals("a|b|c|", ListClass.getStringFromList(Arrays.asList("a", "b", "c", ""), "|"));
    }

    @Test
    void getMapFromString()
    {
        Map<String, ListItem> map = ListClass.getMapFromString("a=1|b");
        assertEquals(2, map.size());
        assertEquals("a", map.get("a").getId());
        assertEquals("1", map.get("a").getValue());
        assertEquals("b", map.get("b").getId());
        assertEquals("b", map.get("b").getValue());
    }

    @Test
    void getMapFromStringWithEmptyValue()
    {
        Map<String, ListItem> map = ListClass.getMapFromString("|a");
        assertEquals(2, map.size());
        assertEquals("", map.get("").getId());
        assertEquals("", map.get("").getValue());
        assertEquals("a", map.get("a").getId());
        assertEquals("a", map.get("a").getValue());
    }

    @Test
    void getMapFromStringWithEmptyValueWithLabel()
    {
        Map<String, ListItem> map = ListClass.getMapFromString("=None|a");
        assertEquals(2, map.size());
        assertEquals("", map.get("").getId());
        assertEquals("None", map.get("").getValue());
        assertEquals("a", map.get("a").getId());
        assertEquals("a", map.get("a").getValue());
    }

    @Test
    void listWithBackslash()
    {
        assertEquals(Arrays.asList("a\\b", "c"), ListClass.getListFromString("a\\b|c"));
        assertEquals(Arrays.asList("a", "\\", "c"), ListClass.getListFromString("a|\\\\|c"));
        assertEquals(Arrays.asList("a", "\\b", "c"), ListClass.getListFromString("a|\\b|c"));
        assertEquals(Arrays.asList("a", "b\\", "c"), ListClass.getListFromString("a|b\\\\|c"));
        assertEquals(Arrays.asList("a", "b|c"), ListClass.getListFromString("a|b\\|c"));
        assertEquals(Arrays.asList("a", "b\\|c"), ListClass.getListFromString("a|b\\\\\\|c"));
        assertEquals(Arrays.asList("a", "\\|c"), ListClass.getListFromString("a|\\\\\\|c"));
        assertEquals(Arrays.asList("a", "|c"), ListClass.getListFromString("a|\\|c"));
        assertEquals(Arrays.asList("\\", "c"), ListClass.getListFromString("\\\\|c"));
        assertEquals(Arrays.asList("a", "\\"), ListClass.getListFromString("a|\\\\"));
        assertEquals(Arrays.asList("a", "\\", "c"), ListClass.getListFromString("a,\\\\,c", ",", false));
        assertEquals(Arrays.asList("a", ",c"), ListClass.getListFromString("a,\\,c", ",", false));
        assertEquals(Arrays.asList("a", "\\,c"), ListClass.getListFromString("a,\\\\\\,c", ",", false));
    }

    @Test
    void getListFromStringFilterEmptyValues()
    {
        assertEquals(Arrays.asList("a", "b", "", "c", ""), ListClass.getListFromString("a|b||c|", "|", false, false));
        assertEquals(Arrays.asList("a", "b", "c"), ListClass.getListFromString("a|b||c|", "|", false, true));
    }

    @Test
    void fromList()
    {
        BaseProperty property = mock(LargeStringProperty.class);
        ListClass listClass = new ListClass()
        {
            @Override
            public List<String> getList(XWikiContext context)
            {
                return null;
            }

            @Override
            public Map<String, ListItem> getMap(XWikiContext context)
            {
                return null;
            }
        };

        listClass.fromList(property, null, true);
        verify(property).setValue(null);

        ListProperty listProperty = mock(StringListProperty.class);
        listClass.fromList(listProperty, null, true);
        verify(listProperty).setList(null);

        listClass.fromList(listProperty, null, false);
        verify(listProperty, times(2)).setList(null);

        List<String> strings = Arrays.asList("XWiki.Foo", null, "XWiki.Bar", "");
        listClass.fromList(listProperty, strings, true);
        verify(listProperty).setList(Arrays.asList("XWiki.Foo", "XWiki.Bar"));

        listClass.fromList(listProperty, strings, false);
        verify(listProperty).setList(strings);

        listClass.fromList(property, strings, false);
        verify(property).setValue("XWiki.Foo");

        listClass.fromList(property, strings, true);
        verify(property, times(2)).setValue("XWiki.Foo");

        listClass.fromList(property, Collections.emptyList(), false);
        verify(property, times(2)).setValue(null);

        listClass.fromList(property, Collections.emptyList(), true);
        verify(property, times(3)).setValue(null);

        strings = Arrays.asList("", "XWiki.Foo", null, "XWiki.Bar", "");
        listClass.fromList(property, strings, false);
        verify(property).setValue("");

        listClass.fromList(property, strings, true);
        verify(property, times(3)).setValue("XWiki.Foo");

        listClass.fromList(property, Collections.singletonList(""), true);
        verify(property, times(4)).setValue(null);

        listClass.setMultiSelect(true);
        listClass.fromList(property, strings, false);
        verify(property).setValue("|XWiki.Foo|XWiki.Bar|");

        listClass.fromList(property, strings, true);
        verify(property).setValue("XWiki.Foo|XWiki.Bar");

        listClass.setSeparators("~@!");
        listClass.fromList(property, strings, false);
        verify(property).setValue("~XWiki.Foo~XWiki.Bar~");

        listClass.fromList(property, strings, true);
        verify(property).setValue("XWiki.Foo~XWiki.Bar");
    }

    @Test
    void fromStringArray()
    {
        String[] array = new String[] { "Foo||Bar", "Baz,Buz,", " ", "Other", "Thing" };
        ListClass listClass = new ListClass()
        {
            @Override
            public List<String> getList(XWikiContext context)
            {
                return null;
            }

            @Override
            public Map<String, ListItem> getMap(XWikiContext context)
            {
                return null;
            }
        };
        listClass.setMultiSelect(false);
        listClass.setSeparators("!");
        listClass.setName("myList");

        StringProperty expectedProperty = new StringProperty();
        expectedProperty.setValue("Foo||Bar");
        expectedProperty.setName("myList");

        assertEquals(expectedProperty, listClass.fromStringArray(array));

        listClass.setSeparators("|,");
        assertEquals(expectedProperty, listClass.fromStringArray(array));

        listClass.setMultiSelect(true);
        StringListProperty expectedList = new StringListProperty();
        expectedList.setName("myList");
        expectedList.setList(Arrays.asList("Foo||Bar", "Baz,Buz,", "Other", "Thing"));
        assertEquals(expectedList, listClass.fromStringArray(array));

        array = new String[] { "Foo||Bar" };
        expectedList.setList(Arrays.asList("Foo", "", "Bar"));
        assertEquals(expectedList, listClass.fromStringArray(array));
    }
}
