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
package org.xwiki.rest.model.jaxb;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Test for {@link PropertyValue}.
 */
public class PropertyValueTest
{
    @Test
    public void equals()
    {
        assertEquals(new PropertyValue(), new PropertyValue());

        PropertyValue p1, p2;
        p1 = new PropertyValue();
        p2 = new PropertyValue();

        p1.setValue("a string value");
        assertNotEquals(p1, p2);
        p2.setValue("a string value");
        assertEquals(p1, p2);

        java.util.Map<String, java.lang.Object> metadata1, metadata2;
        metadata1 = new HashMap<>();
        p1.setMetaData(metadata1);
        metadata2 = new HashMap<>();
        p2.setMetaData(metadata2);

        assertEquals(p1, p2);
        metadata1.put("somekey", true);
        assertNotEquals(p1, p2);
        metadata2.put("anotherkey", false);
        assertNotEquals(p1, p2);

        metadata1.put("anotherkey", false);
        metadata2.put("somekey", true);
        assertEquals(p1, p2);
    }
}
