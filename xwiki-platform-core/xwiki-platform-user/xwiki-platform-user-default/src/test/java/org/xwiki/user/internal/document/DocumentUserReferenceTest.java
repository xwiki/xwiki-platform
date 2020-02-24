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
import org.xwiki.model.reference.DocumentReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
        DocumentUserReference reference1 = new DocumentUserReference(new DocumentReference("wiki1", "space1", "page1"));
        DocumentUserReference reference2 = new DocumentUserReference(new DocumentReference("wiki2", "space2", "page2"));
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
        DocumentUserReference userReference = new DocumentUserReference(documentReference);
        assertEquals(documentReference, userReference.getReference());
    }

    @Test
    void stringRepresentation()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        DocumentUserReference userReference = new DocumentUserReference(documentReference);
        assertEquals("reference = [wiki:space.page]", userReference.toString());
    }
}
