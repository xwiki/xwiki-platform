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

import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;

import java.util.List;
import java.util.ArrayList;

import com.xpn.xwiki.web.Utils;

import org.xwiki.test.jmock.AbstractComponentTestCase;

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
}
