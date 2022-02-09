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

import java.sql.Timestamp;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link DateProperty}.
 *
 * @version $Id$
 * @since 5.0M1
 */
@ComponentTest
@ComponentList({LocalStringEntityReferenceSerializer.class})
class DatePropertyTest
{
    @Test
    void setValue()
    {
        Date date = new Date();
        DateProperty property = new DateProperty();
        property.setValue(date);
        assertSame(date, property.getValue());
    }

    /**
     * Verify that we use {@link Date} for storing the date.
     * 
     * @see "XWIKI-8648: DateProperty#equals pretty much never works between a document loaded from the database and a document loaded from the XAR for example"
     */
    @Test
    void setValueWithExtendedDate()
    {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        DateProperty property = new DateProperty();
        property.setValue(timestamp);
        assertSame(Date.class, property.getValue().getClass());
        assertEquals(timestamp.getTime(), ((Date) property.getValue()).getTime());
    }

    /**
     * Verify that we can set a date that is null (<a href="https://jira.xwiki.org/browse/XWIKI-8837">XWIKI-8837</a>).
     */
    @Test
    void setValueWithNullDate()
    {
        DateProperty property = new DateProperty();
        property.setValue(null);
        assertNull(property.getValue());
    }

    @Test
    void cloneDate()
    {
        DateProperty property = new DateProperty();
        property.setValue(new Date());

        DateProperty clonedProperty = property.clone();

        assertNotSame(property.getValue(), clonedProperty.getValue());
    }
}
