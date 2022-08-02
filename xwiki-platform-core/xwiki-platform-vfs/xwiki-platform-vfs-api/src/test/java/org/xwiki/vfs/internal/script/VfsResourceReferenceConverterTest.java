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
package org.xwiki.vfs.internal.script;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.vfs.VfsResourceReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for {@link VfsResourceReferenceConverter}.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@ComponentTest
class VfsResourceReferenceConverterTest
{
    @InjectMockComponents
    private VfsResourceReferenceConverter vfsResourceReferenceConverter;

    @Test
    void convertWithNull()
    {
        assertNull(this.vfsResourceReferenceConverter.convertToType(null, null));
    }

    @Test
    void convertWithString() throws Exception
    {
        String value = "attach:Toto.WebHome@testvfs.zip///test.doc";
        VfsResourceReference expectedReference = new VfsResourceReference(URI.create(value));
        assertEquals(expectedReference, this.vfsResourceReferenceConverter.convertToType(null, value));
    }

    @Test
    void convertWithSpecialString() throws Exception
    {
        String value = "attach:Toto.WebHome@testvfs.zip///logo xwiki.png";
        VfsResourceReference expectedReference =
            new VfsResourceReference("attach:Toto.WebHome@testvfs.zip///logo xwiki.png");
        assertEquals(expectedReference, this.vfsResourceReferenceConverter.convertToType(null, value));
    }
}
