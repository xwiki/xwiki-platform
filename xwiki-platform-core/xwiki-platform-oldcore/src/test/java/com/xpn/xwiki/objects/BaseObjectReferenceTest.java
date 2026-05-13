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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Validate {@link BaseObjectReference}.
 * 
 * @version $Id$
 */
@ReferenceComponentList
@OldcoreTest
class BaseObjectReferenceTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private DocumentReference document;

    @BeforeEach
    void before()
    {
        this.document = new DocumentReference("wiki", "space", "page");
    }

    @Test
    void serialize()
    {
        BaseObjectReference reference =
            new BaseObjectReference(new DocumentReference("wiki", "space", "class"), 42, this.document);

        assertEquals("space.class[42]", reference.getName());

        reference = new BaseObjectReference(new DocumentReference("wiki", "space", "class"), null, this.document);

        assertEquals("space.class", reference.getName());
    }

    @Test
    void serializeEscape()
    {
        BaseObjectReference reference =
            new BaseObjectReference(new DocumentReference("wiki", "space", "class[42]"), null, this.document);

        assertEquals("space.class\\[42]", reference.getName());

        reference =
            new BaseObjectReference(new DocumentReference("wiki", "space", "class\\\\[42]"), null, this.document);

        assertEquals("space.class\\\\\\\\\\[42]", reference.getName());
    }

    @Test
    void unserialize()
    {
        BaseObjectReference reference =
            new BaseObjectReference(new EntityReference("space.class[42]", EntityType.OBJECT, this.document));

        assertEquals(new DocumentReference("wiki", "space", "class"), reference.getXClassReference());
        assertEquals(42, (int) reference.getObjectNumber());

        reference = new BaseObjectReference(new EntityReference("space.class", EntityType.OBJECT, this.document));

        assertEquals(new DocumentReference("wiki", "space", "class"), reference.getXClassReference());
        assertNull(reference.getObjectNumber());
    }

    @Test
    void unserializeEscape()
    {
        BaseObjectReference reference =
            new BaseObjectReference(new EntityReference("space.class\\[42]", EntityType.OBJECT, this.document));

        assertEquals(new DocumentReference("wiki", "space", "class[42]"), reference.getXClassReference());
        assertNull(reference.getObjectNumber());

        reference =
            new BaseObjectReference(new EntityReference("space.class\\\\[42]", EntityType.OBJECT, this.document));

        assertEquals(new DocumentReference("wiki", "space", "class\\"), reference.getXClassReference());
        assertEquals(42, (int) reference.getObjectNumber());

        reference =
            new BaseObjectReference(new EntityReference("space.class\\\\\\[42]", EntityType.OBJECT, this.document));

        assertEquals(new DocumentReference("wiki", "space", "class\\[42]"), reference.getXClassReference());
        assertNull(reference.getObjectNumber());

        reference = new BaseObjectReference(new EntityReference("space.class[word]", EntityType.OBJECT, this.document));

        assertEquals(new DocumentReference("wiki", "space", "class[word]"), reference.getXClassReference());
        assertNull(reference.getObjectNumber());
    }
}
