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

package org.xwiki.model.internal.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

/**
 * Unit tests for {@link DeprecatedLocalReferenceEntityReferenceSerializer}.
 * 
 * @version $Id$
 */
public class DeprecatedLocalReferenceEntityReferenceSerializerTest
{
    private EntityReferenceSerializer<EntityReference> serializer;

    @BeforeEach
    void setUp()
    {
        this.serializer = new DeprecatedLocalReferenceEntityReferenceSerializer();
    }

    @Test
    void testSerializeDocumentReference() throws Exception
    {
        EntityReference reference = this.serializer.serialize(new DocumentReference("wiki", "space", "page"));

        assertEquals(EntityType.DOCUMENT, reference.getType());
        assertEquals("page", reference.getName());
        assertEquals(EntityType.SPACE, reference.getParent().getType());
        assertEquals("space", reference.getParent().getName());
        assertNull(reference.getParent().getParent());
    }

    @Test
    void testSerializeSpaceReferenceWithChild()
    {
        EntityReference reference =
            this.serializer.serialize(new SpaceReference("space", new WikiReference("wiki")));

        assertEquals(EntityType.SPACE, reference.getType());
        assertEquals("space", reference.getName());
        assertNull(reference.getParent());
    }
}
