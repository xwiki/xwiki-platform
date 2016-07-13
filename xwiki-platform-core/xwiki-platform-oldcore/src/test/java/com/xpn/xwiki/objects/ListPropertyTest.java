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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.test.jmock.AbstractComponentTestCase;

import com.xpn.xwiki.web.Utils;

/**
 * Test list property.
 * 
 * @version $Id$
 */
public class ListPropertyTest extends AbstractComponentTestCase
{

    @Before
    public void configure() throws Exception
    {
        Utils.setComponentManager(getComponentManager());
    }

    @Test
    public void dirtyFlagPropagation() throws Exception
    {
        ListProperty p = new ListProperty();

        p.setValueDirty(false);
        
        List<String> list = p.getList();

        list.add("foo");

        Assert.assertTrue(p.isValueDirty());

        p.setValueDirty(false);

        p.setList(null);

        Assert.assertTrue(p.isValueDirty());
    }

    @Test
    public void cloneListProperty() throws Exception
    {
        ListProperty p = new ListProperty();

        List<String> pList = p.getList();

        p.setValueDirty(false);

        ListProperty clone = p.clone();

        List<String> cloneList = clone.getList();

        Assert.assertFalse(clone.isValueDirty());

        cloneList.add("foo");

        Assert.assertFalse(p.isValueDirty());
        Assert.assertTrue(clone.isValueDirty());
    }

    /**
     * Tests that the value that is saved in the database for a list property is not XML-encoded.
     */
    @Test
    public void getTextValue()
    {
        ListProperty listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("a<b>c", "1\"2'3", "x{y&z"));
        Assert.assertEquals("a<b>c|1\"2'3|x{y&z", listProperty.getTextValue());
    }

    /**
     * Tests that {@link ListProperty#toText()} joins the values using the right separator, without XML-encoding the
     * values.
     */
    @Test
    public void toText()
    {
        ListProperty listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("c<b>a", "3\"2'1", "z{y&x"));
        Assert.assertEquals("c<b>a|3\"2'1|z{y&x", listProperty.toText());
    }

    /**
     * Tests that {@link ListProperty#toFormString()} is XML-encoded.
     */
    @Test
    public void toFormString()
    {
        ListProperty listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("o<n>e", "t\"w'o", "t{h&ree"));
        Assert.assertEquals("o&#60;n&#62;e|t&#34;w&#39;o|t&#123;h&#38;ree", listProperty.toFormString());
    }

    /**
     * Tests that {@link ListProperty#toText()} properly joins values containing the separator itself.
     */
    @Test
    public void toTextValuesWithEscapedSeparators()
    {
        ListProperty listProperty = new ListProperty();
        listProperty.setValue(Arrays.asList("a|b", "c|d", "e\\|f"));
        Assert.assertEquals("a\\|b|c\\|d|e\\\\|f", listProperty.toText());
    }
}
