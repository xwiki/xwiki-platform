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
 *
 */
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit tests for {@link StaticListClass}.
 * 
 * @version $Id$
 */
public class StaticListClassTest extends AbstractBridgedXWikiComponentTestCase
{
    /** Tests that {@link StaticListClass#getList} returns values sorted according to the property's sort option. */
    public void testGetListIsSorted()
    {
        StaticListClass listClass = new StaticListClass();
        listClass.setValues("a=A|c=D|d=C|b");

        assertEquals("Default order was not preserved.", "[a, c, d, b]", listClass.getList(getContext()).toString());
        listClass.setSort("none");
        assertEquals("Default order was not preserved.", "[a, c, d, b]", listClass.getList(getContext()).toString());
        listClass.setSort("id");
        assertEquals("Items were not ordered by ID.", "[a, b, c, d]", listClass.getList(getContext()).toString());
        listClass.setSort("value");
        assertEquals("Items were not ordered by value.", "[a, b, d, c]", listClass.getList(getContext()).toString());
    }
}
