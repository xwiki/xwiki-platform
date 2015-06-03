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

import java.util.Arrays;

import org.junit.Test;
import org.xwiki.extension.xar.question.CleanPagesQuestion;
import org.xwiki.model.reference.DocumentReference;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CleanPagesQuestionRecorder}.
 * 
 * @version $Id$
 * @since 7.1RC1
 */
public class CleanPagesQuestionRecorderTest
{
    @Test
    public void recordAndReplay()
    {
        DocumentReference aliceReference = new DocumentReference("dev", "Users", "Alice");
        DocumentReference bobReference = new DocumentReference("dev", "Users", "Bob");
        DocumentReference carolReference = new DocumentReference("dev", "Users", "Carol");

        CleanPagesQuestion firstQuestion = new CleanPagesQuestion(Arrays.asList(aliceReference, bobReference));
        firstQuestion.getPages().put(aliceReference, false);

        CleanPagesQuestion secondQuestion = new CleanPagesQuestion(Arrays.asList(carolReference));
        secondQuestion.getPages().put(carolReference, false);

        CleanPagesQuestionRecorder recorder = new CleanPagesQuestionRecorder();

        recorder.record(firstQuestion);
        recorder.record(secondQuestion);

        DocumentReference johnReference = new DocumentReference("drafts", "Users", "John");
        DocumentReference aliceDraftsRef =
            aliceReference.replaceParent(aliceReference.getWikiReference(), johnReference.getWikiReference());
        CleanPagesQuestion thirdQuestion = new CleanPagesQuestion(Arrays.asList(aliceDraftsRef, johnReference));
        assertFalse(recorder.replay(thirdQuestion));
        assertFalse(thirdQuestion.getPages().get(aliceDraftsRef));
        assertTrue(thirdQuestion.getPages().get(johnReference));

        DocumentReference bobDraftsRef =
            bobReference.replaceParent(bobReference.getWikiReference(), johnReference.getWikiReference());
        DocumentReference carolDraftsRef =
            carolReference.replaceParent(carolReference.getWikiReference(), johnReference.getWikiReference());
        CleanPagesQuestion fourthQuestion = new CleanPagesQuestion(Arrays.asList(bobDraftsRef, carolDraftsRef));
        assertTrue(recorder.replay(fourthQuestion));
        assertTrue(fourthQuestion.getPages().get(bobDraftsRef));
        assertFalse(fourthQuestion.getPages().get(carolDraftsRef));
    }
}
