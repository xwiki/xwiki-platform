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
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ListClass}.
 * 
 * @version $Id$
 */
public class ListClassTest
{
    /**
     * Test that the default separator {@link ListClass#DEFAULT_SEPARATOR} is used when not specified.
     */
    @Test
    public void testGetListFromStringDefaultSeparator()
    {
        assertEquals(Arrays.asList("a", "b", "c"), ListClass.getListFromString("a|b|c"));
    }

    /**
     * Test that the separator can be escaped inside the list item.
     */
    @Test
    public void testGetListFromStringSeparatorInValues()
    {
        assertEquals(Arrays.asList("a", "|b", "c|", "|"), ListClass.getListFromString("a|\\|b|c\\||\\|"));
    }

    /**
     * Test that custom separators work.
     */
    @Test
    public void testGetListFromStringNonDefaultSeparator()
    {
        assertEquals(Arrays.asList("a", "b", "c"), ListClass.getListFromString("a*b*c", "*", false));
    }

    /**
     * Test that we can use more than one separator.
     */
    @Test
    public void testGetListFromStringMultipleSeparators()
    {
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"),
            ListClass.getListFromString("a*b,c,d*e", "*,", false));
    }

    /**
     * Test that escaped separators in list values work with multipel separators as well.
     */
    @Test
    public void testGetListFromStringMultipleSeparatorsWithSeparatorsInValues()
    {
        assertEquals(Arrays.asList("a*b", "c,d", "e*f"),
            ListClass.getListFromString("a\\*b,c\\,d*e\\*f", "*,", false));
    }

    /**
     * Test that the default separator {@link ListClass#DEFAULT_SEPARATOR} is used when not specified.
     */
    @Test
    public void testGetStringFromListDefaultSeparator()
    {
        assertEquals("a|b|c", ListClass.getStringFromList(Arrays.asList("a", "b", "c")));
    }

    /**
     * Test that the separator can be escaped inside the list item.
     */
    @Test
    public void testGetStringFromListSeparatorInValues()
    {
        assertEquals("a|\\|b|c\\||\\|", ListClass.getStringFromList(Arrays.asList("a", "|b", "c|", "|")));
    }

    /**
     * Test that custom separators work.
     */
    @Test
    public void testGetStringFromListNonDefaultSeparator()
    {
        assertEquals("a*b*c", ListClass.getStringFromList(Arrays.asList("a", "b", "c"), "*"));
    }

    /**
     * Test that we can use more than one separator.
     */
    @Test
    public void testGetStringFromListMultipleSeparators()
    {
        assertEquals("a*b*c*d*e", ListClass.getStringFromList(Arrays.asList("a", "b", "c", "d", "e"), "*,"));
    }

    /**
     * Test that escaped separators in list values work with multipel separators as well.
     */
    @Test
    public void testGetStringFromListMultipleSeparatorsWithSeparatorsInValues()
    {
        assertEquals("a\\*b*c\\,d*e\\*f", ListClass.getStringFromList(Arrays.asList("a*b", "c,d", "e*f"), "*,"));
    }

    @Test
    public void testGetStringFromListWithNullValue()
    {
        assertEquals("a.c", ListClass.getStringFromList(Arrays.asList("a", null, "c"), "."));
        assertEquals("a..c", ListClass.getStringFromList(Arrays.asList("a", "", "c"), "."));
    }

    @Test
    public void getMapFromString()
    {
        Map<String, ListItem> map = ListClass.getMapFromString("a=1|b");
        assertEquals(2, map.size());
        assertEquals("a", map.get("a").getId());
        assertEquals("1", map.get("a").getValue());
        assertEquals("b", map.get("b").getId());
        assertEquals("b", map.get("b").getValue());
    }

    @Test
    public void getMapFromStringWithEmptyValue()
    {
        Map<String, ListItem> map = ListClass.getMapFromString("|a");
        assertEquals(2, map.size());
        assertEquals("", map.get("").getId());
        assertEquals("", map.get("").getValue());
        assertEquals("a", map.get("a").getId());
        assertEquals("a", map.get("a").getValue());
    }

    @Test
    public void getMapFromStringWithEmptyValueWithLabel()
    {
        Map<String, ListItem> map = ListClass.getMapFromString("=None|a");
        assertEquals(2, map.size());
        assertEquals("", map.get("").getId());
        assertEquals("None", map.get("").getValue());
        assertEquals("a", map.get("a").getId());
        assertEquals("a", map.get("a").getValue());
    }

    @Test
    public void listWithBackslash()
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
}
