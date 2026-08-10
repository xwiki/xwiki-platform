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
package org.xwiki.rest.internal.representations.objects;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.xwiki.rest.model.jaxb.Property;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for {@link TextPlainPropertyReader}.
 *
 * @version $Id$
 */
class TextPlainPropertyReaderTest
{
    private final TextPlainPropertyReader reader = new TextPlainPropertyReader();

    private InputStream utf8(String value)
    {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readsAsciiValue() throws Exception
    {
        Property property = this.reader.readFrom(Property.class, null, null, null, null, utf8("hello"));

        assertEquals("hello", property.getValue());
    }

    @Test
    void readsUtf8ValueWithoutCorruption() throws Exception
    {
        String value = "café — 日本語 — €";

        Property property = this.reader.readFrom(Property.class, null, null, null, null, utf8(value));

        assertEquals(value, property.getValue());
    }

    @Test
    void nullStreamYieldsEmptyValue() throws Exception
    {
        Property property = this.reader.readFrom(Property.class, null, null, null, null, null);

        assertEquals("", property.getValue());
    }
}
