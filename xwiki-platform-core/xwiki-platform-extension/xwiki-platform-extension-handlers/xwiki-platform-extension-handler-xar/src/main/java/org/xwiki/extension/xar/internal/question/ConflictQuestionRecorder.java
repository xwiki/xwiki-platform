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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.extension.xar.question.ConflictQuestion;

/**
 * Component used to record and replay {@link ConflictQuestion}s.
 * 
 * @version $Id$
 * @since 7.1RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ConflictQuestionRecorder extends AbstractDocumentQuestionRecorder<ConflictQuestion, ConflictAnswer>
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void record(ConflictQuestion question)
    {
        this.answers.put(question.getNextDocument().getDocumentReference(),
            new ConflictAnswer(question.getGlobalAction(), question.isAlways()));
    }

    @Override
    public boolean replay(ConflictQuestion question)
    {
        ConflictAnswer recordedAnswer = getRecordedAnswer(question.getNextDocument().getDocumentReference());
        if (recordedAnswer != null) {
            question.setGlobalAction(recordedAnswer.getGlobalAction());
            question.setAlways(recordedAnswer.isAlways());
            return true;
        }

        return false;
    }
}
