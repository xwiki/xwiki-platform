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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StringClass;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PropertyConverter}.
 * 
 * @version $Id$
 */
@ComponentTest
public class PropertyConverterTest
{
    @InjectMockComponents
    private PropertyConverter converter;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    /**
     * XWIKI-8649: Error when changing the number type of a field from an application
     */
    @Test
    void doubleToInteger() throws Exception
    {
        // The number property whose type has changed from Double to Integer.
        NumberClass numberClass = mock(NumberClass.class);
        IntegerProperty integerProperty = mock(IntegerProperty.class);
        when(numberClass.newProperty()).thenReturn(integerProperty);
        when(numberClass.getNumberType()).thenReturn("integer");

        DoubleProperty doubleProperty = mock(DoubleProperty.class);
        when(doubleProperty.getValue()).thenReturn(3.5);

        assertEquals(integerProperty, this.converter.convertProperty(doubleProperty, numberClass));
        verify(integerProperty).setValue(3);
    }

    @Test
    void unsetDoubleToInteger() throws Exception
    {
        NumberClass numberClass = mock(NumberClass.class);
        DoubleProperty unsetDoubleProperty = mock(DoubleProperty.class, "unset");
        assertNull(this.converter.convertProperty(unsetDoubleProperty, numberClass));
    }

    @Test
    void multipleToSingleSelectOnDBList() throws Exception
    {
        // The Database List property that was switched from multiple select to single select.
        DBListClass dbListClass = mock(DBListClass.class);
        when(dbListClass.isMultiSelect()).thenReturn(false);
        StringProperty stringProperty = mock(StringProperty.class);
        when(dbListClass.newProperty()).thenReturn(stringProperty);

        StringListProperty stringListProperty = mock(StringListProperty.class);
        when(stringListProperty.getValue()).thenReturn(Arrays.asList("one", "two"));

        assertEquals(stringProperty, this.converter.convertProperty(stringListProperty, dbListClass));
        verify(stringProperty).setValue("one");
    }

    @Test
    void multipleToSingleSelectOnEmptyDBList() throws Exception
    {
        // The Database List property that was switched from multiple select to single select.
        DBListClass dbListClass = mock(DBListClass.class);
        when(dbListClass.isMultiSelect()).thenReturn(false);

        StringListProperty emptyListProperty = mock(StringListProperty.class);
        when(emptyListProperty.getValue()).thenReturn(Arrays.asList());

        assertNull(this.converter.convertProperty(emptyListProperty, dbListClass));
    }

    @Test
    void singleToMultipleSelectOnDBList() throws Exception
    {
        // The Database List property that was switched from single select to multiple select.
        DBListClass dbListClass = mock(DBListClass.class);
        when(dbListClass.isMultiSelect()).thenReturn(true);
        StringListProperty stringListProperty = mock(StringListProperty.class);
        when(dbListClass.newProperty()).thenReturn(stringListProperty);

        StringProperty stringProperty = mock(StringProperty.class);
        when(stringProperty.getValue()).thenReturn("one");

        assertEquals(stringListProperty, this.converter.convertProperty(stringProperty, dbListClass));
        verify(stringListProperty).setValue(Arrays.asList("one"));
    }

    @Test
    void singleToMultipleSelectOnUnsetDBList() throws Exception
    {
        // The Database List property that was switched from single select to multiple select.
        DBListClass dbListClass = mock(DBListClass.class);
        when(dbListClass.isMultiSelect()).thenReturn(true);

        StringProperty stringProperty = mock(StringProperty.class);
        when(stringProperty.getValue()).thenReturn(null);

        assertNull(this.converter.convertProperty(stringProperty, dbListClass));
    }

    @Test
    void longToString() throws Exception
    {
        LongProperty longProperty = new LongProperty();
        longProperty.setName("property");
        longProperty.setValue(Long.MAX_VALUE);

        StringProperty stringProperty = new StringProperty();
        stringProperty.setName(longProperty.getName());

        StringClass stringClass = mock(StringClass.class);
        when(stringClass.newProperty()).thenReturn(new StringProperty());
        when(stringClass.fromString(longProperty.toText())).thenReturn(stringProperty);

        assertEquals(stringProperty, this.converter.convertProperty(longProperty, stringClass));
    }

    @Test
    void stringToNumber() throws Exception
    {
        StringProperty stringProperty = new StringProperty();
        stringProperty.setValue("one");

        NumberClass numberClass = mock(NumberClass.class);
        when(numberClass.newProperty()).thenReturn(new IntegerProperty());
        when(numberClass.fromString(stringProperty.toText())).thenReturn(null);
        when(numberClass.getName()).thenReturn("age");
        when(numberClass.getClassName()).thenReturn("Some.Class");

        assertNull(this.converter.convertProperty(stringProperty, numberClass));
        ILoggingEvent logEvent = this.logCapture.getLogEvent(0);
        assertSame(Level.WARN, logEvent.getLevel());
        assertEquals("Incompatible data migration when changing field [age] of class [Some.Class]",
            logEvent.getFormattedMessage());
    }
}
