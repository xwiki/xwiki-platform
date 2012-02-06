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
package org.xwiki.annotation.reference;

import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests the {@link IndexedObjectReference} implementation of object names.
 * 
 * @version $Id$
 * @since 2.3M1
 */
public class IndexedObjectReferenceTest
{
    /**
     * Ensures the equivalence of constructors.
     */
    @Test
    public void testConstructors()
    {
        IndexedObjectReference reference1 =
            new IndexedObjectReference(new EntityReference("XWiki.Class[2]", EntityType.OBJECT, new EntityReference(
                "Page", EntityType.DOCUMENT, new EntityReference("Space", EntityType.SPACE, new EntityReference("wiki",
                    EntityType.WIKI)))));
        IndexedObjectReference reference2 =
            new IndexedObjectReference("XWiki.Class", 2, new EntityReference("Page", EntityType.DOCUMENT,
                new EntityReference("Space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI))));
        assertEquals(reference1, reference2);
        assertEquals(reference1.getClassName(), reference2.getClassName());
        assertEquals(reference1.getObjectNumber(), reference2.getObjectNumber());
    }

    /**
     * Ensures the equivalence of constructors building a reference to the default object.
     */
    public void testConstructorsWhenNonIndexedReference()
    {
        IndexedObjectReference reference1 =
            new IndexedObjectReference(new EntityReference("XWiki.Class", EntityType.OBJECT, new EntityReference(
                "Page", EntityType.DOCUMENT, new EntityReference("Space", EntityType.SPACE, new EntityReference("wiki",
                    EntityType.WIKI)))));
        IndexedObjectReference reference2 =
            new IndexedObjectReference("XWiki.Class", null, new EntityReference("Page", EntityType.DOCUMENT,
                new EntityReference("Space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI))));
        assertEquals(reference1, reference2);
        assertEquals(reference1.getClassName(), reference2.getClassName());
        assertEquals(reference1.getObjectNumber(), reference2.getObjectNumber());
    }

    @Test
    public void testObjectNumber()
    {
        IndexedObjectReference reference =
            new IndexedObjectReference(new EntityReference("XWiki.Class[2]", EntityType.OBJECT, new EntityReference(
                "Page", EntityType.DOCUMENT, new EntityReference("Space", EntityType.SPACE, new EntityReference("wiki",
                    EntityType.WIKI)))));
        assertEquals("XWiki.Class", reference.getClassName());
        assertEquals(new Integer(2), reference.getObjectNumber());
    }

    @Test
    public void testObjectNumberWhenDefaultObject()
    {
        IndexedObjectReference reference =
            new IndexedObjectReference(new EntityReference("XWiki.Class", EntityType.OBJECT, new EntityReference(
                "Page", EntityType.DOCUMENT, new EntityReference("Space", EntityType.SPACE, new EntityReference("wiki",
                    EntityType.WIKI)))));
        assertEquals("XWiki.Class", reference.getClassName());
        assertNull(reference.getObjectNumber());
    }

    @Test
    public void testClassNameWhenRelativeClass()
    {
        IndexedObjectReference reference =
            new IndexedObjectReference(new EntityReference("Class", EntityType.OBJECT, new EntityReference("Page",
                EntityType.DOCUMENT, new EntityReference("Space", EntityType.SPACE, new EntityReference("wiki",
                    EntityType.WIKI)))));
        assertEquals("Class", reference.getClassName());
        assertNull(reference.getObjectNumber());
    }

    @Test
    public void testObjectNumberWhenNonPairedSeparators()
    {
        IndexedObjectReference reference =
            new IndexedObjectReference(new EntityReference("XWiki.Class0]", EntityType.OBJECT, new EntityReference(
                "Page", EntityType.DOCUMENT, new EntityReference("Space", EntityType.SPACE, new EntityReference("wiki",
                    EntityType.WIKI)))));
        assertEquals("XWiki.Class0]", reference.getClassName());
        assertNull(reference.getObjectNumber());
    }

    @Test
    public void testObjectNumberWhenNameContainsSeparators()
    {
        IndexedObjectReference reference =
            new IndexedObjectReference(new EntityReference("XW[iki.C]lass[0]", EntityType.OBJECT, new EntityReference(
                "Page", EntityType.DOCUMENT, new EntityReference("Space", EntityType.SPACE, new EntityReference("wiki",
                    EntityType.WIKI)))));
        assertEquals("XW[iki.C]lass", reference.getClassName());
        assertEquals(new Integer(0), reference.getObjectNumber());
    }

    @Test
    public void testObjectNumberWhenIndexNotNumber()
    {
        IndexedObjectReference reference =
            new IndexedObjectReference(new EntityReference("XWiki.Class[number]", EntityType.OBJECT,
                new EntityReference("Page", EntityType.DOCUMENT, new EntityReference("Space", EntityType.SPACE,
                    new EntityReference("wiki", EntityType.WIKI)))));
        assertEquals("XWiki.Class[number]", reference.getClassName());
        assertNull(reference.getObjectNumber());
    }
}
