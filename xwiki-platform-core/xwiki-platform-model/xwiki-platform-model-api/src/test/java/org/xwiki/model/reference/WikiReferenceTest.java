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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link WikiReference}.
 *
 * @version $Id$
 * @since 2.2M1
 */
public class WikiReferenceTest
{
    @Test
    public void testInvalidType()
    {
        IllegalArgumentException expected = assertThrows(IllegalArgumentException.class,
            () -> new WikiReference(new EntityReference("wiki", EntityType.DOCUMENT)));
        assertEquals("Invalid type [DOCUMENT] for a wiki reference", expected.getMessage());
    }

    @Test
    public void testInvalidParent()
    {
        EntityReference badParent = new EntityReference("other", EntityType.DOCUMENT);
        IllegalArgumentException expected = assertThrows(IllegalArgumentException.class,
            () -> new WikiReference(new EntityReference("wiki", EntityType.WIKI, badParent)));
        assertEquals("Unexpected parent [" + badParent + "] in a wiki reference", expected.getMessage());
    }

    @Test
    void instanceOf()
    {
        assertTrue(new WikiReference("foo") instanceof WikiReference);
        assertTrue(new WikiReference("foo") instanceof EntityReference);
    }
}
