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
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for the Object reference ({@link PageObjectReference}).
 * 
 * @version $Id$
 */
public class PageObjectReferenceTest
{
    /**
     * Ensures the equivalence of constructors.
     */
    @Test
    public void testConstructors()
    {
        PageObjectReference reference = new PageObjectReference(new EntityReference("Object", EntityType.PAGE_OBJECT,
            new EntityReference("Page", EntityType.PAGE, new EntityReference("wiki", EntityType.WIKI))));
        assertEquals(reference, new PageObjectReference("Object", new PageReference("wiki", "Page")));
    }

    @Test
    public void testInvalidType()
    {
        try {
            new PageObjectReference(new EntityReference("className", EntityType.PAGE));

            fail("Should have thrown exception");
        } catch (IllegalArgumentException expected) {
            assertEquals("Invalid type [PAGE] for an object reference", expected.getMessage());
        }
    }

    @Test
    public void testInvalidNullParent()
    {
        try {
            new PageObjectReference(new EntityReference("className", EntityType.PAGE_OBJECT));

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
            new PageObjectReference(
                new EntityReference("className", EntityType.PAGE_OBJECT, new EntityReference("wiki", EntityType.WIKI)));

            fail("Should have thrown exception");
        } catch (IllegalArgumentException expected) {
            assertEquals("Invalid parent reference [Wiki wiki] in an object reference", expected.getMessage());
        }
    }

    @Test
    public void testReplaceParent()
    {
        PageObjectReference reference =
            new PageObjectReference("object", new PageReference("wiki", "space", "page"))
                .replaceParent(new PageReference("wiki2", "space2", "page2"));

        assertEquals(new PageObjectReference("object", new PageReference("wiki2", "space2", "page2")), reference);

        assertSame(reference, reference.replaceParent(reference.getParent()));
    }
}
