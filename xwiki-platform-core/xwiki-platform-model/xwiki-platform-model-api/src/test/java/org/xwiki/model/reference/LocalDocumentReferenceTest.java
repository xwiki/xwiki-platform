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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link LocalDocumentReference}.
 *
 * @version $Id$
 * @since 5.0M1
 */
public class LocalDocumentReferenceTest
{
    @Test
    public void verifyLocalDocumentReferenceProperties()
    {
        LocalDocumentReference reference = new LocalDocumentReference("space", "page");
        assertEquals("space", reference.getParent().getName());
        assertNull(reference.getParent().getParent());
        assertTrue(reference instanceof EntityReference);
    }

    @Test
    public void testReplaceParent()
    {
        LocalDocumentReference reference =
            new LocalDocumentReference("space", "page").replaceParent(new EntityReference("space2", EntityType.SPACE));

        assertEquals(new LocalDocumentReference("space2", "page"), reference);

        assertSame(reference, reference.replaceParent(reference.getParent()));
    }
}
