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
package org.xwiki.extension.xar.internal.question;

import org.junit.Test;
import org.xwiki.extension.xar.question.ConflictQuestion;
import org.xwiki.extension.xar.question.ConflictQuestion.GlobalAction;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ConflictQuestionRecorder}.
 * 
 * @version $Id$
 * @since 7.1RC1
 */
public class ConflictQuestionRecorderTest
{
    @Test
    public void recordAndReplay()
    {
        XWikiDocument alice = mock(XWikiDocument.class, "Alice");
        when(alice.getDocumentReference()).thenReturn(new DocumentReference("dev", "Users", "Alice"));
        ConflictQuestion aliceQuestion = new ConflictQuestion(null, null, alice, null);
        aliceQuestion.setGlobalAction(GlobalAction.CURRENT);

        XWikiDocument bob = mock(XWikiDocument.class, "Bob");
        when(bob.getDocumentReference()).thenReturn(new DocumentReference("dev", "Users", "Bob"));
        ConflictQuestion bobQuestion = new ConflictQuestion(null, null, bob, null);
        bobQuestion.setGlobalAction(GlobalAction.PREVIOUS);
        bobQuestion.setAlways(true);

        ConflictQuestionRecorder recorder = new ConflictQuestionRecorder();
        recorder.record(aliceQuestion);
        recorder.record(bobQuestion);

        XWikiDocument carol = mock(XWikiDocument.class, "Carol");
        when(carol.getDocumentReference()).thenReturn(new DocumentReference("drafts", "Users", "Carol"));
        ConflictQuestion carolQuestion = new ConflictQuestion(null, null, carol, null);
        assertFalse(recorder.replay(carolQuestion));
        assertEquals(GlobalAction.MERGED, carolQuestion.getGlobalAction());
        assertFalse(carolQuestion.isAlways());

        XWikiDocument aliceDrafts = mock(XWikiDocument.class, "AliceDrafts");
        when(aliceDrafts.getDocumentReference()).thenReturn(new DocumentReference("drafts", "Users", "Alice"));
        ConflictQuestion aliceDraftsQuestion = new ConflictQuestion(null, null, aliceDrafts, null);
        assertTrue(recorder.replay(aliceDraftsQuestion));
        assertEquals(GlobalAction.CURRENT, aliceDraftsQuestion.getGlobalAction());
        assertFalse(aliceDraftsQuestion.isAlways());

        XWikiDocument bobDrafts = mock(XWikiDocument.class, "BobDrafts");
        when(bobDrafts.getDocumentReference()).thenReturn(new DocumentReference("drafts", "Users", "Bob"));
        ConflictQuestion bobDraftsQuestion = new ConflictQuestion(null, null, bobDrafts, null);
        assertTrue(recorder.replay(bobDraftsQuestion));
        assertEquals(GlobalAction.PREVIOUS, bobDraftsQuestion.getGlobalAction());
        assertTrue(bobDraftsQuestion.isAlways());
    }
}
