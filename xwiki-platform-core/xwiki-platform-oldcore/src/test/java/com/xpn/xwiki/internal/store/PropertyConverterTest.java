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
package com.xpn.xwiki.internal.store;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StringClass;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PropertyConverter}.
 * 
 * @version $Id$
 */
public class PropertyConverterTest
{
    @Rule
    public MockitoComponentMockingRule<PropertyConverter> mocker = new MockitoComponentMockingRule<PropertyConverter>(
        PropertyConverter.class);

    /**
     * @see XWIKI-8649: Error when changing the number type of a field from an application
     */
    @Test
    public void doubleToInteger() throws Exception
    {
        // The number property whose type has changed from Double to Integer.
        NumberClass numberClass = mock(NumberClass.class);
        IntegerProperty integerProperty = mock(IntegerProperty.class);
        when(numberClass.newProperty()).thenReturn(integerProperty);
        when(numberClass.getNumberType()).thenReturn("integer");

        DoubleProperty doubleProperty = mock(DoubleProperty.class);
        when(doubleProperty.getValue()).thenReturn(3.5);

        assertEquals(integerProperty, this.mocker.getComponentUnderTest().convertProperty(doubleProperty, numberClass));
        verify(integerProperty).setValue(3);
    }

    @Test
    public void unsetDoubleToInteger() throws Exception
    {
        NumberClass numberClass = mock(NumberClass.class);
        DoubleProperty unsetDoubleProperty = mock(DoubleProperty.class, "unset");
        assertNull(this.mocker.getComponentUnderTest().convertProperty(unsetDoubleProperty, numberClass));
    }

    @Test
    public void multipleToSingleSelectOnDBList() throws Exception
    {
        // The Database List property that was switched from multiple select to single select.
        DBListClass dbListClass = mock(DBListClass.class);
        when(dbListClass.isMultiSelect()).thenReturn(false);
        StringProperty stringProperty = mock(StringProperty.class);
        when(dbListClass.newProperty()).thenReturn(stringProperty);

        StringListProperty stringListProperty = mock(StringListProperty.class);
        when(stringListProperty.getValue()).thenReturn(Arrays.asList("one", "two"));

        assertEquals(stringProperty,
            this.mocker.getComponentUnderTest().convertProperty(stringListProperty, dbListClass));
        verify(stringProperty).setValue("one");
    }

    @Test
    public void multipleToSingleSelectOnEmptyDBList() throws Exception
    {
        // The Database List property that was switched from multiple select to single select.
        DBListClass dbListClass = mock(DBListClass.class);
        when(dbListClass.isMultiSelect()).thenReturn(false);

        StringListProperty emptyListProperty = mock(StringListProperty.class);
        when(emptyListProperty.getValue()).thenReturn(Arrays.asList());

        assertNull(this.mocker.getComponentUnderTest().convertProperty(emptyListProperty, dbListClass));
    }

    @Test
    public void singleToMultipleSelectOnDBList() throws Exception
    {
        // The Database List property that was switched from single select to multiple select.
        DBListClass dbListClass = mock(DBListClass.class);
        when(dbListClass.isMultiSelect()).thenReturn(true);
        StringListProperty stringListProperty = mock(StringListProperty.class);
        when(dbListClass.newProperty()).thenReturn(stringListProperty);

        StringProperty stringProperty = mock(StringProperty.class);
        when(stringProperty.getValue()).thenReturn("one");

        assertEquals(stringListProperty,
            this.mocker.getComponentUnderTest().convertProperty(stringProperty, dbListClass));
        verify(stringListProperty).setValue(Arrays.asList("one"));
    }

    @Test
    public void singleToMultipleSelectOnUnsetDBList() throws Exception
    {
        // The Database List property that was switched from single select to multiple select.
        DBListClass dbListClass = mock(DBListClass.class);
        when(dbListClass.isMultiSelect()).thenReturn(true);

        StringProperty stringProperty = mock(StringProperty.class);
        when(stringProperty.getValue()).thenReturn(null);

        assertNull(this.mocker.getComponentUnderTest().convertProperty(stringProperty, dbListClass));
    }

    @Test
    public void longToString() throws Exception
    {
        LongProperty longProperty = new LongProperty();
        longProperty.setValue(Long.MAX_VALUE);

        StringClass stringClass = mock(StringClass.class);
        when(stringClass.newProperty()).thenReturn(new StringProperty());
        StringProperty stringProperty = new StringProperty();
        when(stringClass.fromString(longProperty.toText())).thenReturn(stringProperty);

        assertEquals(stringProperty, this.mocker.getComponentUnderTest().convertProperty(longProperty, stringClass));
    }

    @Test
    public void stringToNumber() throws Exception
    {
        StringProperty stringProperty = new StringProperty();
        stringProperty.setValue("one");

        NumberClass numberClass = mock(NumberClass.class);
        when(numberClass.newProperty()).thenReturn(new IntegerProperty());
        when(numberClass.fromString(stringProperty.toText())).thenReturn(null);
        when(numberClass.getName()).thenReturn("age");
        when(numberClass.getClassName()).thenReturn("Some.Class");

        assertNull(this.mocker.getComponentUnderTest().convertProperty(stringProperty, numberClass));
        verify(this.mocker.getMockedLogger()).warn(
            "Incompatible data migration when changing field [{}] of class [{}]", "age", "Some.Class");
    }
}
