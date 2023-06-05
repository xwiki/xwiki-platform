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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.diff.Conflict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
    void equalsAndHashCode()
    {
        MergeManagerResult<DocumentModelBridge, Object> mergeManagerResult = new MergeManagerResult<>();
        MergeManagerResult<DocumentModelBridge, Object> otherMergeManager = new MergeManagerResult<>();
        assertEquals(mergeManagerResult, otherMergeManager);
        assertEquals(mergeManagerResult.hashCode(), otherMergeManager.hashCode());

        DocumentModelBridge result = mock(DocumentModelBridge.class);
        mergeManagerResult.setMergeResult(result);
        otherMergeManager.setMergeResult(result);

        mergeManagerResult.getLog().info("test");
        otherMergeManager.getLog().info("test");

        assertEquals(mergeManagerResult, otherMergeManager);
        assertEquals(mergeManagerResult.hashCode(), otherMergeManager.hashCode());

        // changing logs doesn't impact equals unless it's an error log on a manager without conflicts
        otherMergeManager.getLog().info("other");
        assertEquals(mergeManagerResult, otherMergeManager);
        assertEquals(mergeManagerResult.hashCode(), otherMergeManager.hashCode());

        otherMergeManager.getLog().error("something");
        assertNotEquals(mergeManagerResult, otherMergeManager);
        assertNotEquals(mergeManagerResult.hashCode(), otherMergeManager.hashCode());

        otherMergeManager = new MergeManagerResult<>();
        otherMergeManager.setMergeResult(result);

        List<Conflict<Object>> conflictList = new ArrayList<>();
        conflictList.add(mock(Conflict.class));
        conflictList.add(mock(Conflict.class));

        mergeManagerResult.addConflicts(conflictList);
        otherMergeManager.addConflicts(conflictList);

        assertEquals(mergeManagerResult, otherMergeManager);
        assertEquals(mergeManagerResult.hashCode(), otherMergeManager.hashCode());

        // error log doesn't impact equals here since there was already conflicts listed
        otherMergeManager.getLog().error("something");
        assertEquals(mergeManagerResult, otherMergeManager);
        assertEquals(mergeManagerResult.hashCode(), otherMergeManager.hashCode());

        otherMergeManager = new MergeManagerResult<>();
        otherMergeManager.setMergeResult(mock(DocumentModelBridge.class));
        otherMergeManager.addConflicts(conflictList);
        assertNotEquals(mergeManagerResult, otherMergeManager);
        assertNotEquals(mergeManagerResult.hashCode(), otherMergeManager.hashCode());

        otherMergeManager = new MergeManagerResult<>();
        otherMergeManager.setMergeResult(result);
        otherMergeManager.addConflicts(Arrays.asList(mock(Conflict.class), mock(Conflict.class)));
        assertNotEquals(mergeManagerResult, otherMergeManager);
        assertNotEquals(mergeManagerResult.hashCode(), otherMergeManager.hashCode());

        otherMergeManager = new MergeManagerResult<>();
        otherMergeManager.setMergeResult(result);
        assertNotEquals(mergeManagerResult, otherMergeManager);
        assertNotEquals(mergeManagerResult.hashCode(), otherMergeManager.hashCode());

        otherMergeManager = new MergeManagerResult<>();
        otherMergeManager.setMergeResult(result);
        otherMergeManager.addConflicts(conflictList);
        assertEquals(mergeManagerResult, otherMergeManager);
        assertEquals(mergeManagerResult.hashCode(), otherMergeManager.hashCode());

        otherMergeManager.setModified(true);
        assertNotEquals(mergeManagerResult, otherMergeManager);
        assertNotEquals(mergeManagerResult.hashCode(), otherMergeManager.hashCode());
    }

    @Test
    void getConflictsNumber()
    {
        MergeManagerResult mergeManagerResult = new MergeManagerResult();
        assertEquals(0, mergeManagerResult.getConflictsNumber());

        mergeManagerResult.addConflicts(Collections.singletonList(mock(Conflict.class)));
        mergeManagerResult.getLog().error("A new conflict");
        assertEquals(1, mergeManagerResult.getConflictsNumber());

        mergeManagerResult.getLog().error("Another new conflict");
        assertEquals(2, mergeManagerResult.getConflictsNumber());

        mergeManagerResult.getLog().error("Another one");
        assertEquals(3, mergeManagerResult.getConflictsNumber());

        mergeManagerResult.addConflicts(Collections.singletonList(mock(Conflict.class)));
        mergeManagerResult.getLog().error("A new conflict");
        assertEquals(4, mergeManagerResult.getConflictsNumber());
    }
}
