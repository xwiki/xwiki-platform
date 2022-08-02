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
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.diff.Conflict;
import org.xwiki.logging.LogQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void equalsAndHashCode()
    {
        DocumentModelBridge currentDoc = mock(DocumentModelBridge.class);
        DocumentModelBridge previousDoc = mock(DocumentModelBridge.class);
        DocumentModelBridge nextDoc = mock(DocumentModelBridge.class);
        DocumentModelBridge mergeResult = mock(DocumentModelBridge.class);

        MergeDocumentResult mergeDocumentResult = new MergeDocumentResult(currentDoc, previousDoc, nextDoc);
        mergeDocumentResult.setMergeResult(mergeResult);

        MergeManagerResult contentResult = mock(MergeManagerResult.class);
        when(contentResult.getLog()).thenReturn(new LogQueue());
        mergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.CONTENT, contentResult);

        MergeManagerResult xObjectsResult = mock(MergeManagerResult.class);
        when(xObjectsResult.getLog()).thenReturn(new LogQueue());
        mergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.XOBJECTS, xObjectsResult);

        mergeDocumentResult.getLog().info("Something");

        MergeDocumentResult otherMergeDocumentResult = new MergeDocumentResult(currentDoc, previousDoc, nextDoc);
        otherMergeDocumentResult.setMergeResult(mergeResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.CONTENT, contentResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.XOBJECTS, xObjectsResult);
        otherMergeDocumentResult.getLog().info("Something");

        assertEquals(mergeDocumentResult, otherMergeDocumentResult);
        assertEquals(mergeDocumentResult.hashCode(), otherMergeDocumentResult.hashCode());

        // adding logs doesn't change equality, unless it's an error log
        mergeDocumentResult.getLog().warn("Another log");
        assertEquals(mergeDocumentResult, otherMergeDocumentResult);
        assertEquals(mergeDocumentResult.hashCode(), otherMergeDocumentResult.hashCode());

        otherMergeDocumentResult.getLog().error("SOmething's wrong");
        assertNotEquals(mergeDocumentResult, otherMergeDocumentResult);
        assertNotEquals(mergeDocumentResult.hashCode(), otherMergeDocumentResult.hashCode());

        otherMergeDocumentResult = new MergeDocumentResult(currentDoc, previousDoc, nextDoc);
        otherMergeDocumentResult.setMergeResult(mergeResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.CONTENT, contentResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.XOBJECTS, xObjectsResult);
        otherMergeDocumentResult.getLog().info("Something");

        MergeManagerResult titleResult = mock(MergeManagerResult.class);
        when(titleResult.getLog()).thenReturn(new LogQueue());
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.TITLE, titleResult);

        assertNotEquals(mergeDocumentResult, otherMergeDocumentResult);
        assertNotEquals(mergeDocumentResult.hashCode(), otherMergeDocumentResult.hashCode());

        otherMergeDocumentResult = new MergeDocumentResult(mock(DocumentModelBridge.class), previousDoc, nextDoc);
        otherMergeDocumentResult.setMergeResult(mergeResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.CONTENT, contentResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.XOBJECTS, xObjectsResult);
        otherMergeDocumentResult.getLog().info("Something");

        assertNotEquals(mergeDocumentResult, otherMergeDocumentResult);
        assertNotEquals(mergeDocumentResult.hashCode(), otherMergeDocumentResult.hashCode());

        otherMergeDocumentResult = new MergeDocumentResult(currentDoc, mock(DocumentModelBridge.class), nextDoc);
        otherMergeDocumentResult.setMergeResult(mergeResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.CONTENT, contentResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.XOBJECTS, xObjectsResult);
        otherMergeDocumentResult.getLog().info("Something");

        assertNotEquals(mergeDocumentResult, otherMergeDocumentResult);
        assertNotEquals(mergeDocumentResult.hashCode(), otherMergeDocumentResult.hashCode());

        otherMergeDocumentResult = new MergeDocumentResult(currentDoc, previousDoc, mock(DocumentModelBridge.class));
        otherMergeDocumentResult.setMergeResult(mergeResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.CONTENT, contentResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.XOBJECTS, xObjectsResult);
        otherMergeDocumentResult.getLog().info("Something");

        assertNotEquals(mergeDocumentResult, otherMergeDocumentResult);
        assertNotEquals(mergeDocumentResult.hashCode(), otherMergeDocumentResult.hashCode());

        otherMergeDocumentResult = new MergeDocumentResult(currentDoc, previousDoc, nextDoc);
        otherMergeDocumentResult.setMergeResult(mock(DocumentModelBridge.class));
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.CONTENT, contentResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.XOBJECTS, xObjectsResult);
        otherMergeDocumentResult.getLog().info("Something");

        assertNotEquals(mergeDocumentResult, otherMergeDocumentResult);
        assertNotEquals(mergeDocumentResult.hashCode(), otherMergeDocumentResult.hashCode());

        otherMergeDocumentResult = new MergeDocumentResult(currentDoc, previousDoc, nextDoc);
        otherMergeDocumentResult.setMergeResult(mergeResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.XOBJECTS, xObjectsResult);
        otherMergeDocumentResult.getLog().info("Something");

        assertNotEquals(mergeDocumentResult, otherMergeDocumentResult);
        assertNotEquals(mergeDocumentResult.hashCode(), otherMergeDocumentResult.hashCode());

        otherMergeDocumentResult = new MergeDocumentResult(currentDoc, previousDoc, nextDoc);
        otherMergeDocumentResult.setMergeResult(mergeResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.CONTENT, contentResult);
        otherMergeDocumentResult.putMergeResult(MergeDocumentResult.DocumentPart.XOBJECTS, xObjectsResult);
        otherMergeDocumentResult.getLog().info("Something");
        otherMergeDocumentResult.setModified(true);

        assertNotEquals(mergeDocumentResult, otherMergeDocumentResult);
        assertNotEquals(mergeDocumentResult.hashCode(), otherMergeDocumentResult.hashCode());
    }
}
