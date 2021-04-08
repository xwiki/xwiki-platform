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
import org.xwiki.properties.ConverterManager;

import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link BaseStringProperty} class.
 *
 * @version $Id$
 */
@OldcoreTest
public class BaseStringPropertyTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Test
    public void testHashCode()
    {
        String value = "test value";

        BaseStringProperty p1 = new BaseStringProperty();
        BaseStringProperty p2 = new BaseStringProperty();

        p1.setValue(value);
        p2.setValue(value);

        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void setValueWhenTypeIsNotString() throws Exception
    {
        BaseStringProperty p = new BaseStringProperty();
        Integer value = new Integer(42);
        ConverterManager converterManager = this.oldcore.getMocker().registerMockComponent(ConverterManager.class);
        when(converterManager.convert(String.class, value)).thenReturn("42");
        // Note: we set the dirty flag to false since it's true by default, to verify that it's set.
        p.setValueDirty(false);

        p.setValue(value);

        assertTrue(p.isValueDirty());
        assertEquals("42", p.getValue());
    }

    @Test
    public void setValueWhenTypeIsNotStringAndSameValue() throws Exception
    {
        BaseStringProperty p = new BaseStringProperty();
        Integer value = new Integer(42);
        ConverterManager converterManager = this.oldcore.getMocker().registerMockComponent(ConverterManager.class);
        when(converterManager.convert(String.class, value)).thenReturn("42");
        // Set an existing value with the same String representation to verify that the dirty flag is not set in this
        // case. We also need to set the dirty flag to false since by default its true (and set to false by the DB
        // operations only).
        p.setValue("42");
        p.setValueDirty(false);

        p.setValue(value);

        assertFalse(p.isValueDirty());
    }
}
