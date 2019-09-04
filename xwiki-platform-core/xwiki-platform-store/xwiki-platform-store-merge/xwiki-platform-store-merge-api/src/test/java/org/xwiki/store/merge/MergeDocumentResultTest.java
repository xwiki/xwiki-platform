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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.diff.Conflict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MergeDocumentResult}.
 *
 * @since 11.8RC1
 * @version $Id$
 */
public class MergeDocumentResultTest
{
    @Test
    public void putMergeResult()
    {
        MergeDocumentResult mergeDocumentResult = new MergeDocumentResult(null, null, null);
        assertFalse(mergeDocumentResult.isModified());
        assertFalse(mergeDocumentResult.hasConflicts());
        assertTrue(mergeDocumentResult.getLog().isEmpty());
        assertTrue(mergeDocumentResult.getConflicts().isEmpty());
        assertNull(mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.TITLE));

        MergeManagerResult mergeManagerResult = new MergeManagerResult();
        mergeManagerResult.getLog().info("An information.");
        mergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.TITLE, mergeManagerResult);
        assertFalse(mergeDocumentResult.isModified());
        assertFalse(mergeDocumentResult.hasConflicts());
        assertTrue(mergeDocumentResult.getConflicts().isEmpty());
        assertEquals(1, mergeDocumentResult.getLog().size());
        assertTrue(mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.TITLE).isEmpty());
        assertSame(mergeManagerResult, mergeDocumentResult.getMergeResult(MergeDocumentResult.DocumentPart.TITLE));

        mergeManagerResult = new MergeManagerResult();
        mergeManagerResult.getLog().error("A conflict.");
        mergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.CONTENT, mergeManagerResult);
        assertFalse(mergeDocumentResult.isModified());
        assertTrue(mergeDocumentResult.hasConflicts());
        assertTrue(mergeDocumentResult.getConflicts().isEmpty());
        assertEquals(2, mergeDocumentResult.getLog().size());
        assertTrue(mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.CONTENT).isEmpty());

        mergeDocumentResult = new MergeDocumentResult(null, null, null);
        Conflict conflict = mock(Conflict.class);
        mergeManagerResult = new MergeManagerResult();
        mergeManagerResult.addConflicts(Collections.singletonList(conflict));
        mergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.ATTACHMENTS, mergeManagerResult);
        assertFalse(mergeDocumentResult.isModified());
        assertTrue(mergeDocumentResult.hasConflicts());
        assertEquals(1, mergeDocumentResult.getConflicts().size());
        assertTrue(mergeDocumentResult.getLog().isEmpty());
        assertEquals(1, mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.ATTACHMENTS).size());

        mergeManagerResult = new MergeManagerResult();
        mergeManagerResult.getLog().error("A conflict.");
        mergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.CONTENT, mergeManagerResult);
        assertFalse(mergeDocumentResult.isModified());
        assertTrue(mergeDocumentResult.hasConflicts());
        assertEquals(1, mergeDocumentResult.getConflicts().size());
        assertEquals(1, mergeDocumentResult.getLog().size());
        assertTrue(mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.CONTENT).isEmpty());
        assertEquals(1, mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.ATTACHMENTS).size());

        mergeManagerResult = new MergeManagerResult();
        mergeManagerResult.getLog().info("Something else");
        mergeManagerResult.setModified(true);
        mergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.TITLE, mergeManagerResult);
        assertTrue(mergeDocumentResult.isModified());
        assertTrue(mergeDocumentResult.hasConflicts());
        assertEquals(1, mergeDocumentResult.getConflicts().size());
        assertEquals(2, mergeDocumentResult.getLog().size());
        assertTrue(mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.CONTENT).isEmpty());
        assertTrue(mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.TITLE).isEmpty());
        assertEquals(1, mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.ATTACHMENTS).size());

        mergeManagerResult = new MergeManagerResult();
        mergeManagerResult.addConflicts(Arrays.asList(conflict, conflict));
        mergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.XOBJECTS, mergeManagerResult);
        assertTrue(mergeDocumentResult.isModified());
        assertTrue(mergeDocumentResult.hasConflicts());
        assertEquals(3, mergeDocumentResult.getConflicts().size());
        assertEquals(2, mergeDocumentResult.getLog().size());
        assertTrue(mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.CONTENT).isEmpty());
        assertTrue(mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.TITLE).isEmpty());
        assertEquals(1, mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.ATTACHMENTS).size());
        assertEquals(2, mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.XOBJECTS).size());
    }

    @Test
    public void putMergeResultTwice()
    {
        MergeDocumentResult mergeDocumentResult = new MergeDocumentResult(null, null, null);
        assertNull(mergeDocumentResult.getMergeResult(MergeDocumentResult.DocumentPart.CONTENT));
        mergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.CONTENT, new MergeManagerResult());
        assertNotNull(mergeDocumentResult.getMergeResult(MergeDocumentResult.DocumentPart.CONTENT));
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            mergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.CONTENT, new MergeManagerResult());
        });
        assertEquals("The merge result of document part [CONTENT] has already been put.", exception.getMessage());
    }
}
