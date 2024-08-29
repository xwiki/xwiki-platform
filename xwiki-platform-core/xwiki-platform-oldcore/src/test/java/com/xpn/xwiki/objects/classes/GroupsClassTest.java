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
import java.util.List;

import org.junit.jupiter.api.Test;

import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link GroupsClass}.
 *
 * @version $Id$
 * @since 14.2RC1
 */
public class GroupsClassTest
{
    @Test
    void fromList()
    {
        BaseProperty baseProperty = mock(LargeStringProperty.class);
        List<String> list = Arrays.asList("XWiki.Foo", null, "XWiki.Bar", "");
        GroupsClass groupsClass = new GroupsClass();
        groupsClass.setMultiSelect(true);
        groupsClass.fromList(baseProperty, list);
        verify(baseProperty).setValue("XWiki.Foo,XWiki.Bar");
    }

    @Test
    void fromStringArray()
    {
        String[] array = new String[] {"XWiki.Foo", null, "XWiki.Bar", ""};
        GroupsClass groupsClass = new GroupsClass();
        groupsClass.setMultiSelect(true);
        LargeStringProperty expectedProperty = new LargeStringProperty();
        expectedProperty.setValue("XWiki.Foo,XWiki.Bar");
        expectedProperty.setName("groupslist");
        assertEquals(expectedProperty, groupsClass.fromStringArray(array));

        array = new String[] {"XWiki.Foo,XWiki.Bar,"};
        expectedProperty.setValue("XWiki.Foo,XWiki.Bar");
        assertEquals(expectedProperty, groupsClass.fromStringArray(array));
    }
}
