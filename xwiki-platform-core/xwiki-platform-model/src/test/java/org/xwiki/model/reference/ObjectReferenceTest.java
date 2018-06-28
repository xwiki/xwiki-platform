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

import org.junit.Test;
import org.xwiki.model.EntityType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit test for the Object reference ({@link ObjectReference}).
 * 
 * @version $Id$
 * @since 2.3M1
 */
public class ObjectReferenceTest
{
    /**
     * Ensures the equivalence of constructors.
     */
    @Test
    public void testConstructors()
    {
        ObjectReference reference =
            new ObjectReference(new EntityReference("Object", EntityType.OBJECT, new EntityReference("Page",
                EntityType.DOCUMENT, new EntityReference("Space", EntityType.SPACE, new EntityReference("wiki",
                    EntityType.WIKI)))));
        assertEquals(reference, new ObjectReference("Object", new DocumentReference("wiki", "Space", "Page")));
    }

    @Test
    public void testInvalidType()
    {
        try {
            new ObjectReference(new EntityReference("className", EntityType.DOCUMENT));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException expected) {
            assertEquals("Invalid type [DOCUMENT] for an object reference", expected.getMessage());
        }
    }

    @Test
    public void testInvalidNullParent()
    {
        try {
            new ObjectReference(new EntityReference("className", EntityType.OBJECT));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException expected) {
            assertEquals("Invalid parent reference [null] in an object reference", expected.getMessage());
        }
    }

    /**
     * Tests that an object reference throws exception if it doesn't have a document as a parent.
     */
    @Test
    public void testInvalidParentType()
    {
        try {
            new ObjectReference(new EntityReference("className", EntityType.OBJECT, new EntityReference("Space",
                EntityType.SPACE)));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException expected) {
            assertEquals("Invalid parent reference [Space Space] in an object reference", expected.getMessage());
        }
    }
}
