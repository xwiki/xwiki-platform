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
package com.xpn.xwiki.objects;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test list property.
 * 
 * @version $Id$
 */
class ListPropertyTest
{
    @Test
    void dirtyFlagPropagation() throws Exception
    {
        ListProperty p = new ListProperty();

        p.setValueDirty(false);

        List<String> list = p.getList();

        list.add("foo");

        assertTrue(p.isValueDirty());

        p.setValueDirty(false);

        p.setList(null);

        assertTrue(p.isValueDirty());

        p.setValueDirty(false);

        assertEquals(Arrays.asList(), p.getList());
        assertFalse(p.isValueDirty());
    }

    @Test
    void cloneListProperty() throws Exception
    {
        ListProperty p = new ListProperty();

        List<String> pList = p.getList();

        p.setValueDirty(false);

        ListProperty clone = p.clone();

        List<String> cloneList = clone.getList();

        assertFalse(clone.isValueDirty());

        cloneList.add("foo");

        assertFalse(p.isValueDirty());
        assertTrue(clone.isValueDirty());
    }

    /**
     * Tests that the value that is saved in the database for a list property is not XML-encoded.
     */
    @Test
    void getTextValue()
    {
        ListProperty listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("a<b>c", "1\"2'3", "x{y&z"));
        assertEquals("a<b>c|1\"2'3|x{y&z", listProperty.getTextValue());
    }

    /**
     * Tests that {@link ListProperty#toText()} joins the values using the right separator, without XML-encoding the
     * values.
     */
    @Test
    void toText()
    {
        ListProperty listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("c<b>a", "3\"2'1", "z{y&x"));
        assertEquals("c<b>a|3\"2'1|z{y&x", listProperty.toText());
    }

    /**
     * Tests that {@link ListProperty#toFormString()} is XML-encoded.
     */
    @Test
    void toFormString()
    {
        ListProperty listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("o<n>e", "t\"w'o", "t{h&ree"));
        assertEquals("o&#60;n&#62;e|t&#34;w&#39;o|t&#123;h&#38;ree", listProperty.toFormString());
    }

    /**
     * Tests that {@link ListProperty#toText()} properly joins values containing the separator itself.
     */
    @Test
    void toTextValuesWithEscapedSeparators()
    {
        ListProperty listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("a|b", "c|d", "e\\|f"));
        assertEquals("a\\|b|c\\|d|e\\\\\\|f", listProperty.toText());
    }

    @Test
    void toListValuesWithBackslash()
    {
        ListProperty listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("a\\b", "c"));
        assertEquals("a\\b|c", listProperty.toText());

        listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("a", "\\", "c"));
        assertEquals("a|\\\\|c", listProperty.toText());

        listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("a", "\\b", "c"));
        assertEquals("a|\\b|c", listProperty.toText());

        listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("a", "b\\", "c"));
        assertEquals("a|b\\\\|c", listProperty.toText());

        listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("a", "b|c"));
        assertEquals("a|b\\|c", listProperty.toText());

        listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("a", "b\\|c"));
        assertEquals("a|b\\\\\\|c", listProperty.toText());

        listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("a", "|c"));
        assertEquals("a|\\|c", listProperty.toText());

        listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("a", "\\|c"));
        assertEquals("a|\\\\\\|c", listProperty.toText());

        listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("\\", "c"));
        assertEquals("\\\\|c", listProperty.toText());

        listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("a", "\\"));
        assertEquals("a|\\\\", listProperty.toText());
    }
}
