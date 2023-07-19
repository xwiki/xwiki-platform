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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.diff.Conflict;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link MergeDocumentResultScript}.
 * 
 * @version $Id$
 */
public class MergeDocumentResultScriptTest
{
    private XWikiDocument mockDocument(XWikiContext xcontext)
    {
        XWikiDocument xdocument = mock(XWikiDocument.class);
        Document document = mock(Document.class);
        when(xdocument.newDocument(xcontext)).thenReturn(document);

        return xdocument;
    }

    @Test
    public void misc()
    {
        XWikiContext xcontext = mock(XWikiContext.class);

        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        List<Conflict<Object>> allConflicts = mock(List.class);
        when(mergeDocumentResult.getConflicts()).thenReturn(allConflicts);
        List<Conflict<?>> contentConflicts = mock(List.class);
        when(mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.CONTENT)).thenReturn(contentConflicts);
        XWikiDocument currentXDocument = mockDocument(xcontext);
        when(mergeDocumentResult.getCurrentDocument()).thenReturn(currentXDocument);
        XWikiDocument mergedXDocument = mockDocument(xcontext);
        when(mergeDocumentResult.getMergeResult()).thenReturn(mergedXDocument);
        XWikiDocument nextXDocument = mockDocument(xcontext);
        when(mergeDocumentResult.getNextDocument()).thenReturn(nextXDocument);
        XWikiDocument previousXDocument = mockDocument(xcontext);
        when(mergeDocumentResult.getPreviousDocument()).thenReturn(previousXDocument);

        MergeDocumentResultScript result = new MergeDocumentResultScript(mergeDocumentResult, xcontext);

        assertSame(allConflicts, result.getAllConflicts());
        assertSame(contentConflicts, result.getContentConflicts());
        assertSame(currentXDocument.newDocument(xcontext), result.getCurrentDocument());
        assertSame(mergedXDocument.newDocument(xcontext), result.getMergedDocument());
        assertSame(nextXDocument.newDocument(xcontext), result.getNextDocument());
        assertSame(previousXDocument.newDocument(xcontext), result.getPreviousDocument());
    }

    @Test
    void hasOnlyContentConflicts()
    {
        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        XWikiDocument currentXDocument = mockDocument(xcontext);
        when(mergeDocumentResult.getCurrentDocument()).thenReturn(currentXDocument);
        XWikiDocument mergedXDocument = mockDocument(xcontext);
        when(mergeDocumentResult.getMergeResult()).thenReturn(mergedXDocument);
        XWikiDocument nextXDocument = mockDocument(xcontext);
        when(mergeDocumentResult.getNextDocument()).thenReturn(nextXDocument);
        XWikiDocument previousXDocument = mockDocument(xcontext);
        when(mergeDocumentResult.getPreviousDocument()).thenReturn(previousXDocument);

        MergeDocumentResultScript result = new MergeDocumentResultScript(mergeDocumentResult, xcontext);
        assertFalse(result.hasOnlyContentConflicts());

        when(mergeDocumentResult.getConflicts(MergeDocumentResult.DocumentPart.CONTENT))
            .thenReturn(Collections.singletonList(mock(Conflict.class)));
        when(mergeDocumentResult.getConflictsNumber()).thenReturn(2);
        assertFalse(result.hasOnlyContentConflicts());

        when(mergeDocumentResult.getConflictsNumber()).thenReturn(1);
        assertTrue(result.hasOnlyContentConflicts());
    }
}
