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
package org.xwiki.records.internal.macro;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.properties.converter.Converter;
import org.xwiki.records.macro.RecordsLocation;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RecordsLocationConverter}.
 *
 * @version $Id$
 */
@ComponentTest
class RecordsLocationConverterTest
{
    private static final SpaceReference SPACE = new SpaceReference("xwiki", "Clients", "Projects");

    @InjectMockComponents
    private RecordsLocationConverter converter;

    @MockComponent
    private Converter<SpaceReference> spaceReferenceConverter;

    @Test
    void convertFromString()
    {
        when(this.spaceReferenceConverter.<SpaceReference>convert(SpaceReference.class, "Clients.Projects"))
            .thenReturn(SPACE);

        Object location = this.converter.convert(RecordsLocation.class, "Clients.Projects");

        assertInstanceOf(RecordsLocation.class, location);
        assertEquals(SPACE, location);
    }

    @Test
    void convertNoValue()
    {
        assertNull(this.converter.convert(RecordsLocation.class, null));
    }

    @Test
    void convertToString()
    {
        RecordsLocation location = new RecordsLocation(SPACE);
        when(this.spaceReferenceConverter.<String>convert(String.class, location)).thenReturn("xwiki:Clients.Projects");

        assertEquals("xwiki:Clients.Projects", this.converter.convert(String.class, location));
    }
}
