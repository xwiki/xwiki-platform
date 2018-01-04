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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.test.ComponentManagerRule;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for {@link NumberProperty}.
 *
 * @version $Id$
 * @since 5.2M1
 */
@ComponentList({LocalStringEntityReferenceSerializer.class})
public class NumberPropertyTest
{
    @Rule
    public ComponentManagerRule componentManager = new ComponentManagerRule();

    @Before
    public void setup()
    {
        Utils.setComponentManager(this.componentManager);
    }

    /**
     * Verify that we can compare a null valued number property with a non-null valued number property without having a
     * NPE (<a href="https://jira.xwiki.org/browse/XWIKI-9326">XWIKI-9326</a>).
     */
    @Test
    public void nullValueEqualsWithOtherNumberProperty()
    {
        NumberProperty nullValueProperty = new IntegerProperty();
        nullValueProperty.setValue(null);
        Assert.assertNull(nullValueProperty.getValue());

        NumberProperty notNullValueProperty = new IntegerProperty();
        notNullValueProperty.setValue(1);
        Assert.assertNotNull(notNullValueProperty.getValue());

        // Should not throw a NPE.
        Assert.assertFalse(nullValueProperty.equals(notNullValueProperty));
    }

    /**
     * Verify that we can compare a not-null valued number property with a null valued number property without having a
     * NPE (<a href="https://jira.xwiki.org/browse/XWIKI-9326">XWIKI-9326</a>).
     */
    @Test
    public void notNullValueEqualsWithOtherNullNumberProperty()
    {
        NumberProperty nullValueProperty = new IntegerProperty();
        nullValueProperty.setValue(1);
        Assert.assertNotNull(nullValueProperty.getValue());

        NumberProperty notNullValueProperty = new IntegerProperty();
        notNullValueProperty.setValue(null);
        Assert.assertNull(notNullValueProperty.getValue());

        // Should not throw a NPE.
        Assert.assertFalse(nullValueProperty.equals(notNullValueProperty));
    }

    /**
     * Verify that we can compare two null valued number properties without having a NPE (<a
     * href="https://jira.xwiki.org/browse/XWIKI-9326">XWIKI-9326</a>).
     */
    @Test
    public void equalNullValueEquals()
    {
        NumberProperty nullValueProperty1 = new IntegerProperty();
        nullValueProperty1.setValue(null);
        Assert.assertNull(nullValueProperty1.getValue());

        NumberProperty nullValueProperty2 = new IntegerProperty();
        nullValueProperty2.setValue(null);
        Assert.assertNull(nullValueProperty2.getValue());

        // Should not throw a NPE.
        Assert.assertTrue(nullValueProperty1.equals(nullValueProperty2));
    }

    /**
     * Two equal non-null values.
     */
    @Test
    public void equalNotNullValues()
    {
        NumberProperty nullValueProperty = new IntegerProperty();
        nullValueProperty.setValue(1);

        NumberProperty notNullValueProperty = new IntegerProperty();
        notNullValueProperty.setValue(1);

        Assert.assertTrue(nullValueProperty.equals(notNullValueProperty));
    }

    /**
     * Two not equal non-null values.
     */
    @Test
    public void notEqualNonNullValues()
    {
        NumberProperty nullValueProperty = new IntegerProperty();
        nullValueProperty.setValue(0);

        NumberProperty notNullValueProperty = new IntegerProperty();
        notNullValueProperty.setValue(1);

        Assert.assertFalse(nullValueProperty.equals(notNullValueProperty));
    }

    @Test
    public void testHashCode()
    {
        final Number value = 101;

        NumberProperty n1 = new NumberProperty();
        NumberProperty n2 = new NumberProperty();

        n1.setValue(value);
        n2.setValue(value);

        Assert.assertEquals(n1.hashCode(), n2.hashCode());
    }
}
