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

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.test.ComponentManagerRule;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for {@link DateProperty}.
 *
 * @version $Id$
 * @since 5.0M1
 */
@ComponentList({
    LocalStringEntityReferenceSerializer.class
})
public class DatePropertyTest
{
    @Rule
    public ComponentManagerRule componentManager = new ComponentManagerRule();

    @Before
    public void setUp()
    {
        Utils.setComponentManager(this.componentManager);
    }

    @Test
    public void setValue()
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
    public void setValueWithExtendedDate()
    {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        DateProperty property = new DateProperty();
        property.setValue(timestamp);
        assertSame(Date.class, property.getValue().getClass());
        assertEquals(timestamp.getTime(), ((Date) property.getValue()).getTime());
    }

    /**
     * Verify that we can set a date that is null (<a href="http://jira.xwiki.org/browse/XWIKI-8837">XWIKI-8837</a>).
     */
    @Test
    public void setValueWithNullDate()
    {
        DateProperty property = new DateProperty();
        property.setValue(null);
        assertNull(property.getValue());
    }
}
