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

import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals(Arrays.asList("a", "b", "c"), ListClass.getListFromString("a|b|c"));
    }

    /**
     * Test that the separator can be escaped inside the list item.
     */
    @Test
    public void testGetListFromStringSeparatorInValues()
    {
        Assert.assertEquals(Arrays.asList("a", "|b", "c|", "|"), ListClass.getListFromString("a|\\|b|c\\||\\|"));
    }

    /**
     * Test that custom separators work.
     */
    @Test
    public void testGetListFromStringNonDefaultSeparator()
    {
        Assert.assertEquals(Arrays.asList("a", "b", "c"), ListClass.getListFromString("a*b*c", "*", false));
    }

    /**
     * Test that we can use more than one separator.
     */
    @Test
    public void testGetListFromStringMultipleSeparators()
    {
        Assert.assertEquals(Arrays.asList("a", "b", "c", "d", "e"),
            ListClass.getListFromString("a*b,c,d*e", "*,", false));
    }

    /**
     * Test that escaped separators in list values work with multipel separators as well.
     */
    @Test
    public void testGetListFromStringMultipleSeparatorsWithSeparatorsInValues()
    {
        Assert.assertEquals(Arrays.asList("a*b", "c,d", "e*f"),
            ListClass.getListFromString("a\\*b,c\\,d*e\\*f", "*,", false));
    }

    /**
     * Test that the default separator {@link ListClass#DEFAULT_SEPARATOR} is used when not specified.
     */
    @Test
    public void testGetStringFromListDefaultSeparator()
    {
        Assert.assertEquals("a|b|c", ListClass.getStringFromList(Arrays.asList("a", "b", "c")));
    }

    /**
     * Test that the separator can be escaped inside the list item.
     */
    @Test
    public void testGetStringFromListSeparatorInValues()
    {
        Assert.assertEquals("a|\\|b|c\\||\\|", ListClass.getStringFromList(Arrays.asList("a", "|b", "c|", "|")));
    }

    /**
     * Test that custom separators work.
     */
    @Test
    public void testGetStringFromListNonDefaultSeparator()
    {
        Assert.assertEquals("a*b*c", ListClass.getStringFromList(Arrays.asList("a", "b", "c"), "*"));
    }

    /**
     * Test that we can use more than one separator.
     */
    @Test
    public void testGetStringFromListMultipleSeparators()
    {
        Assert.assertEquals("a*b*c*d*e", ListClass.getStringFromList(Arrays.asList("a", "b", "c", "d", "e"), "*,"));
    }

    /**
     * Test that escaped separators in list values work with multipel separators as well.
     */
    @Test
    public void testGetStringFromListMultipleSeparatorsWithSeparatorsInValues()
    {
        Assert.assertEquals("a\\*b*c\\,d*e\\*f", ListClass.getStringFromList(Arrays.asList("a*b", "c,d", "e*f"), "*,"));
    }
}
