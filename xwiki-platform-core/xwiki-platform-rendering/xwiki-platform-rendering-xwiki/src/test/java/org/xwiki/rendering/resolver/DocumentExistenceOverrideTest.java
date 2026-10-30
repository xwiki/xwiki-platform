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
package org.xwiki.rendering.resolver;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for {@link DocumentExistenceOverride}.
 *
 * @version $Id$
 */
class DocumentExistenceOverrideTest
{
    private static final DocumentReference EXISTING = new DocumentReference("wiki", "Space", "Existing");

    private static final DocumentReference MISSING = new DocumentReference("wiki", "Space", "Missing");

    private static final DocumentReference OTHER = new DocumentReference("wiki", "Space", "Other");

    @Test
    void getExistence()
    {
        DocumentExistenceOverride existenceOverride =
            new DocumentExistenceOverride(Map.of(EXISTING, true, MISSING, false));

        assertEquals(Optional.of(true), existenceOverride.getExistence(EXISTING));
        assertEquals(Optional.of(false), existenceOverride.getExistence(MISSING));
        assertEquals(Optional.empty(), existenceOverride.getExistence(OTHER));
    }

    @Test
    void getExistenceWithNone()
    {
        assertEquals(Optional.empty(), DocumentExistenceOverride.NONE.getExistence(EXISTING));
    }

    @Test
    void getExistenceIgnoresLocale()
    {
        DocumentExistenceOverride existenceOverride =
            new DocumentExistenceOverride(Map.of(new DocumentReference(EXISTING, Locale.FRENCH), true));

        assertEquals(Optional.of(true), existenceOverride.getExistence(EXISTING));
        assertEquals(Optional.of(true), existenceOverride.getExistence(new DocumentReference(EXISTING, Locale.GERMAN)));
    }

    @Test
    void equalsAndHashCode()
    {
        DocumentExistenceOverride existenceOverride = new DocumentExistenceOverride(Map.of(EXISTING, true));

        assertEquals(new DocumentExistenceOverride(Map.of(EXISTING, true)), existenceOverride);
        assertEquals(new DocumentExistenceOverride(Map.of(EXISTING, true)).hashCode(), existenceOverride.hashCode());
        assertNotEquals(new DocumentExistenceOverride(Map.of(EXISTING, false)), existenceOverride);
    }
}
