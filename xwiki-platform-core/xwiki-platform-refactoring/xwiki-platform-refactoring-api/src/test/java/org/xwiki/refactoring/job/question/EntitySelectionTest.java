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
package org.xwiki.refactoring.job.question;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link EntitySelection}.
 *
 * @version $Id$
 */
class EntitySelectionTest
{
    @Test
    void equals()
    {
        EntityReference entityReference = new EntityReference("test", EntityType.PAGE);
        EntitySelection selected = new EntitySelection(entityReference);

        EntitySelection unselected = new EntitySelection(entityReference);
        unselected.setSelected(false);

        assertFalse(selected.equals(null));
        assertTrue(selected.equals(selected));
        assertFalse(selected.equals(entityReference));
        assertFalse(selected.equals(unselected));
        assertTrue(selected.equals(new EntitySelection(entityReference)));
    }

    @Test
    void compareTo()
    {
        EntityReference aEntityReference = new EntityReference("A", EntityType.PAGE);
        EntitySelection aEntitySelection = new EntitySelection(aEntityReference);

        assertThrows(NullPointerException.class, () -> {
            aEntitySelection.compareTo(null);
        });

        assertEquals(0, aEntitySelection.compareTo(aEntitySelection));

        EntitySelection aPrimeEntitySelection = new EntitySelection(aEntityReference);
        assertEquals(0, aEntitySelection.compareTo(aPrimeEntitySelection));

        aEntitySelection.setSelected(true);
        assertEquals(-2, aEntitySelection.compareTo(aPrimeEntitySelection));
        aEntitySelection.setSelected(false);
        assertEquals(-1, aEntitySelection.compareTo(aPrimeEntitySelection));

        EntityReference bEntityReference = new EntityReference("B", EntityType.PAGE);
        EntitySelection bEntitySelection = new EntitySelection(bEntityReference);
        assertEquals(-1, aEntitySelection.compareTo(bEntitySelection));
    }

    @Test
    void compareToEntityReferenceNull()
    {
        EntitySelection o = new EntitySelection(null);
        assertEquals(-1, o.compareTo(new EntitySelection(null)));
    }

    @Test
    void compareToEntitySelectionReferenceNull()
    {
        EntitySelection o = new EntitySelection(new EntityReference("A", EntityType.PAGE));
        assertEquals(1, o.compareTo(new EntitySelection(null)));
    }
}
