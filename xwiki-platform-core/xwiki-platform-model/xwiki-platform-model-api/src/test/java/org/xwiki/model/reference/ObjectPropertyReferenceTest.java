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
package org.xwiki.model.reference;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for the Property reference ({@link ObjectPropertyReference}).
 * 
 * @version $Id$
 * @since 2.3M1
 */
public class ObjectPropertyReferenceTest
{
    /**
     * Test equivalence of constructors.
     */
    @Test
    public void testConstructors()
    {
        ObjectPropertyReference reference =
            new ObjectPropertyReference(new EntityReference("property", EntityType.OBJECT_PROPERTY,
                new EntityReference("Object", EntityType.OBJECT, new EntityReference("Page", EntityType.DOCUMENT,
                    new EntityReference("Space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI))))));
        assertEquals(reference, new ObjectPropertyReference("property",
            new ObjectReference("Object", new DocumentReference("wiki", "Space", "Page"))));
    }

    @Test
    public void testInvalidType()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new ObjectPropertyReference(new EntityReference("space.page", EntityType.DOCUMENT)));

        assertEquals("Invalid type [DOCUMENT] for an object property reference", e.getMessage());
    }

    @Test
    public void testInvalidNullParent()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new ObjectPropertyReference(new EntityReference("property", EntityType.OBJECT_PROPERTY)));

        assertEquals("Invalid parent reference [null] in an object property reference", e.getMessage());
    }

    /**
     * Tests that an object reference throws exception if it doesn't have a document as a parent.
     */
    @Test
    public void testInvalidParentType()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new ObjectPropertyReference(new EntityReference("property", EntityType.OBJECT_PROPERTY,
                new EntityReference("Space", EntityType.SPACE))));

        assertEquals("Invalid parent reference [Space Space] in an object property reference", e.getMessage());
    }

    @Test
    public void testReplaceParent()
    {
        ObjectPropertyReference reference = new ObjectPropertyReference("prop",
            new ObjectReference("object", new DocumentReference("wiki", "space", "page")))
                .replaceParent(new ObjectReference("object2", new DocumentReference("wiki2", "space2", "page2")));

        assertEquals(new ObjectPropertyReference("prop",
            new ObjectReference("object2", new DocumentReference("wiki2", "space2", "page2"))), reference);

        assertSame(reference, reference.replaceParent(reference.getParent()));
    }
}
