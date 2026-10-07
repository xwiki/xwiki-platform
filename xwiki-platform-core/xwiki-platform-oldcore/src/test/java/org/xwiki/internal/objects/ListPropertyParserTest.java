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
package org.xwiki.internal.objects;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.StringListProperty;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ListPropertyParser}.
 *
 * @version $Id$
 */
@ComponentTest
class ListPropertyParserTest
{
    @InjectMockComponents
    private ListPropertyParser listPropertyParser;

    @MockComponent
    private ComponentDescriptor<ObjectPropertyParser> descriptor;

    @Test
    void fromString() throws XWikiException
    {
        String value = "1|2|3";
        StringListProperty expectedProperty = new StringListProperty();
        expectedProperty.setValue(List.of("1", "2", "3"));

        when(descriptor.getRoleHint()).thenReturn(StringListProperty.PROPERTY_TYPE);
        BaseProperty<?> obtainedProperty = listPropertyParser.fromString(value);
        assertEquals(expectedProperty, obtainedProperty);

        assertEquals(expectedProperty, listPropertyParser.fromValue(value));
    }

    @Test
    void fromValue() throws XWikiException
    {
        List<String> value = List.of("foo", "bar", "buz");
        DBStringListProperty expectedProperty = new DBStringListProperty();
        expectedProperty.setValue(value);

        when(descriptor.getRoleHint()).thenReturn(DBStringListProperty.PROPERTY_TYPE);
        assertEquals(expectedProperty, listPropertyParser.fromValue(value));
    }
}