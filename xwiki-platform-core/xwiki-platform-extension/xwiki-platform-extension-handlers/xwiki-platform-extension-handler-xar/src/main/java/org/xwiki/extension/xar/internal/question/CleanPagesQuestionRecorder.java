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

import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.extension.xar.question.CleanPagesQuestion;
import org.xwiki.model.reference.DocumentReference;

/**
 * Component used to record and replay {@link CleanPagesQuestion}s.
 * 
 * @version $Id$
 * @since 7.1RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class CleanPagesQuestionRecorder extends AbstractDocumentQuestionRecorder<CleanPagesQuestion, Boolean>
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void record(CleanPagesQuestion question)
    {
        this.answers.putAll(question.getPages());
    }

    @Override
    public boolean replay(CleanPagesQuestion question)
    {
        boolean answered = true;
        for (Map.Entry<DocumentReference, Boolean> entry : question.getPages().entrySet()) {
            Boolean recordedAnswer = getRecordedAnswer(entry.getKey());
            if (recordedAnswer != null) {
                entry.setValue(recordedAnswer);
            } else {
                // There is at least one page for which we don't have the answer.
                answered = false;
            }
        }
        return answered;
    }
}
