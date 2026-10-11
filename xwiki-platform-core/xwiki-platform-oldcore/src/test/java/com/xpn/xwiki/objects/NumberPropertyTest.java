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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link NumberProperty}.
 *
 * @version $Id$
 */
// The equals() tests below verify that contract itself, so their assertions deliberately call equals()
// explicitly: the boolean form is what makes visible which object is the receiver and which argument it
// gets, which matters here because the null-valued and non-null-valued sides are not interchangeable.
// Using assertEquals()/assertNotEquals() would move that into JUnit's internals and would invite a later
// SonarQube S3415 "swap these arguments" change that silently stops testing the contract. That is why
// those methods, and only those, carry @SuppressWarnings("java:S5785").
class NumberPropertyTest
{
    /**
     * Verify that we can compare a null valued number property with a non-null valued number property without having a
     * NPE (<a href="https://jira.xwiki.org/browse/XWIKI-9326">XWIKI-9326</a>).
     */
    @Test
    @SuppressWarnings("java:S5785")
    void nullValueEqualsWithOtherNumberProperty()
    {
        IntegerProperty nullValueProperty = new IntegerProperty();
        nullValueProperty.setValue(null);
        assertNull(nullValueProperty.getValue());

        IntegerProperty notNullValueProperty = new IntegerProperty();
        notNullValueProperty.setValue(1);
        assertNotNull(notNullValueProperty.getValue());

        // Should not throw a NPE.
        assertFalse(nullValueProperty.equals(notNullValueProperty));
    }

    /**
     * Verify that we can compare a not-null valued number property with a null valued number property without having a
     * NPE (<a href="https://jira.xwiki.org/browse/XWIKI-9326">XWIKI-9326</a>).
     */
    @Test
    @SuppressWarnings("java:S5785")
    void notNullValueEqualsWithOtherNullNumberProperty()
    {
        IntegerProperty nullValueProperty = new IntegerProperty();
        nullValueProperty.setValue(1);
        assertNotNull(nullValueProperty.getValue());

        IntegerProperty notNullValueProperty = new IntegerProperty();
        notNullValueProperty.setValue(null);
        assertNull(notNullValueProperty.getValue());

        // Should not throw a NPE.
        assertFalse(nullValueProperty.equals(notNullValueProperty));
    }

    /**
     * Verify that we can compare two null valued number properties without having a NPE
     * (<a href="https://jira.xwiki.org/browse/XWIKI-9326">XWIKI-9326</a>).
     */
    @Test
    @SuppressWarnings("java:S5785")
    void equalNullValueEquals()
    {
        IntegerProperty nullValueProperty1 = new IntegerProperty();
        nullValueProperty1.setValue(null);
        assertNull(nullValueProperty1.getValue());

        IntegerProperty nullValueProperty2 = new IntegerProperty();
        nullValueProperty2.setValue(null);
        assertNull(nullValueProperty2.getValue());

        // Should not throw a NPE.
        assertTrue(nullValueProperty1.equals(nullValueProperty2));
    }

    /**
     * Two equal non-null values.
     */
    @Test
    @SuppressWarnings("java:S5785")
    void equalNotNullValues()
    {
        IntegerProperty nullValueProperty = new IntegerProperty();
        nullValueProperty.setValue(1);

        IntegerProperty notNullValueProperty = new IntegerProperty();
        notNullValueProperty.setValue(1);

        assertTrue(nullValueProperty.equals(notNullValueProperty));
    }

    /**
     * Two not equal non-null values.
     */
    @Test
    @SuppressWarnings("java:S5785")
    void notEqualNonNullValues()
    {
        IntegerProperty nullValueProperty = new IntegerProperty();
        nullValueProperty.setValue(0);

        IntegerProperty notNullValueProperty = new IntegerProperty();
        notNullValueProperty.setValue(1);

        assertFalse(nullValueProperty.equals(notNullValueProperty));
    }

    @Test
    void testHashCode()
    {
        final Number value = 101;

        IntegerProperty n1 = new IntegerProperty();
        IntegerProperty n2 = new IntegerProperty();

        n1.setValue(value);
        n2.setValue(value);

        assertEquals(n1.hashCode(), n2.hashCode());
    }

    @Test
    void convert()
    {
        DoubleProperty doubleProperty = new DoubleProperty();
        doubleProperty.setValue(1);
        assertInstanceOf(Double.class, doubleProperty.getValue());
        assertEquals(1D, doubleProperty.getValue());
        doubleProperty.setValue("2");
        assertInstanceOf(Double.class, doubleProperty.getValue());
        assertEquals(2D, doubleProperty.getValue());

        FloatProperty floatProperty = new FloatProperty();
        floatProperty.setValue(1);
        assertInstanceOf(Float.class, floatProperty.getValue());
        assertEquals(1F, floatProperty.getValue());
        floatProperty.setValue("2");
        assertInstanceOf(Float.class, floatProperty.getValue());
        assertEquals(2F, floatProperty.getValue());

        IntegerProperty integerProperty = new IntegerProperty();
        integerProperty.setValue(1D);
        assertInstanceOf(Integer.class, integerProperty.getValue());
        assertEquals(1, integerProperty.getValue());
        integerProperty.setValue("2");
        assertInstanceOf(Integer.class, integerProperty.getValue());
        assertEquals(2, integerProperty.getValue());

        LongProperty longProperty = new LongProperty();
        longProperty.setValue(1D);
        assertInstanceOf(Long.class, longProperty.getValue());
        assertEquals(1L, longProperty.getValue());
        longProperty.setValue("2");
        assertInstanceOf(Long.class, longProperty.getValue());
        assertEquals(2L, longProperty.getValue());
    }
}
