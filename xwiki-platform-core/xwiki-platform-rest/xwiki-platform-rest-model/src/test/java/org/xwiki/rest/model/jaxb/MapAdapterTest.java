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

import java.util.LinkedHashMap;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MapAdapter}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
public class MapAdapterTest
{
    @Test
    public void marshal() throws Exception
    {
        java.util.Map<String, java.lang.Object> input = new LinkedHashMap<>();
        input.put("label", "News");
        input.put("count", 7);

        Map output = new MapAdapter().marshal(input);
        assertEquals(2, output.getEntries().size());
        assertEquals("label", output.getEntries().get(0).getKey());
        assertEquals("News", output.getEntries().get(0).getValue());
        assertEquals("count", output.getEntries().get(1).getKey());
        assertEquals(7, output.getEntries().get(1).getValue());
    }

    @Test
    public void marshalNull() throws Exception
    {
        assertNull(new MapAdapter().marshal(null));
    }

    @Test
    public void unmarshal() throws Exception
    {
        Map input = new Map();
        input.getEntries().add(new MapEntry().withKey("label").withValue("Release"));
        input.getEntries().add(new MapEntry().withKey("count").withValue(13));

        java.util.Map<String, java.lang.Object> output = new MapAdapter().unmarshal(input);
        assertEquals(2, output.size());
        assertEquals("Release", output.get("label"));
        assertEquals(13, output.get("count"));
    }

    @Test
    public void unmarshalNull() throws Exception
    {
        assertNull(new MapAdapter().unmarshal(null));
    }
}
