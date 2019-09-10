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
package org.xwiki.store.merge;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.diff.Conflict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MergeManagerResult}.
 *
 * @since 11.8RC1
 * @version $Id$
 */
public class MergeManagerResultTest
{
    @Test
    public void hasConflict()
    {
        MergeManagerResult mergeManagerResult = new MergeManagerResult();
        assertFalse(mergeManagerResult.isModified());
        assertFalse(mergeManagerResult.hasConflicts());
        assertTrue(mergeManagerResult.getLog().isEmpty());

        mergeManagerResult.addConflicts(Collections.emptyList());
        assertFalse(mergeManagerResult.hasConflicts());

        Conflict conflict = mock(Conflict.class);

        mergeManagerResult.addConflicts(Collections.singletonList(conflict));
        assertTrue(mergeManagerResult.hasConflicts());
        assertEquals(1, mergeManagerResult.getConflicts().size());

        mergeManagerResult = new MergeManagerResult();
        mergeManagerResult.getLog().warn("Something");
        assertFalse(mergeManagerResult.hasConflicts());
        assertFalse(mergeManagerResult.getLog().isEmpty());

        mergeManagerResult.getLog().error("Something else");
        assertTrue(mergeManagerResult.hasConflicts());
        assertTrue(mergeManagerResult.getConflicts().isEmpty());
    }
}
