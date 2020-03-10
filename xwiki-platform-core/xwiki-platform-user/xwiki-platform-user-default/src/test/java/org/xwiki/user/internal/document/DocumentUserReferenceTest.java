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
package org.xwiki.user.internal.document;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.WikiReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentUserReference}.
 *
 * @version $Id$
 */
public class DocumentUserReferenceTest
{
    @Test
    void identity()
    {
        DocumentUserReference reference1 =
            new DocumentUserReference(new DocumentReference("wiki1", "space1", "page1"), null);
        DocumentUserReference reference2 =
            new DocumentUserReference(new DocumentReference("wiki2", "space2", "page2"), null);
        assertEquals(reference1, reference1);
        assertNotEquals(reference2, reference1);
        assertNotEquals(reference1, null);
        assertNotEquals(reference1, "whatever");
        assertEquals(reference1.hashCode(), reference1.hashCode());
        assertNotEquals(reference2.hashCode(), reference1.hashCode());
    }

    @Test
    void getReference()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        DocumentUserReference userReference = new DocumentUserReference(documentReference, null);
        assertEquals(documentReference, userReference.getReference());
    }

    @Test
    void stringRepresentation()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        DocumentUserReference userReference = new DocumentUserReference(documentReference, null);
        assertEquals("reference = [wiki:space.page]", userReference.toString());
    }

    @Test
    void isGlobalWhenTrue()
    {
        DocumentReference documentReference = new DocumentReference("mainwiki", "space", "page");
        EntityReferenceProvider entityReferenceProvider = mock(EntityReferenceProvider.class);
        when(entityReferenceProvider.getDefaultReference(EntityType.WIKI)).thenReturn(new WikiReference("mainwiki"));
        DocumentUserReference userReference = new DocumentUserReference(documentReference, entityReferenceProvider);

        assertTrue(userReference.isGlobal());
    }

    @Test
    void isGlobalWhenFalse()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        EntityReferenceProvider entityReferenceProvider = mock(EntityReferenceProvider.class);
        when(entityReferenceProvider.getDefaultReference(EntityType.WIKI)).thenReturn(new WikiReference("mainwiki"));
        DocumentUserReference userReference = new DocumentUserReference(documentReference, entityReferenceProvider);

        assertFalse(userReference.isGlobal());
    }
}
