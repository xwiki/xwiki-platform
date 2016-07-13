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

import java.util.HashMap;
import java.util.Map;

import org.xwiki.extension.job.history.QuestionRecorder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

/**
 * Base class for {@link QuestionRecorder}s that record/replay questions regarding documents.
 * 
 * @param <Q> The question type
 * @param <A> The answer type
 * @version $Id$
 * @since 7.1RC1
 */
public abstract class AbstractDocumentQuestionRecorder<Q, A> implements QuestionRecorder<Q>
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    protected final Map<DocumentReference, A> answers = new HashMap<>();

    protected A getRecordedAnswer(DocumentReference documentReference)
    {
        A recordedAnswer = this.answers.get(documentReference);
        if (recordedAnswer == null) {
            WikiReference recordedWikiReference = getRecordedWikiReference();
            if (recordedWikiReference != null) {
                return this.answers.get(documentReference.replaceParent(documentReference.getWikiReference(),
                    recordedWikiReference));
            }
        }
        return recordedAnswer;
    }

    private WikiReference getRecordedWikiReference()
    {
        WikiReference recordedWikiReference = null;
        for (DocumentReference documentReference : this.answers.keySet()) {
            if (recordedWikiReference == null) {
                recordedWikiReference = documentReference.getWikiReference();
            } else if (!recordedWikiReference.equals(documentReference.getWikiReference())) {
                // There are multiple wikis recorded.
                return null;
            }
        }
        return recordedWikiReference;
    }
}
