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
package org.xwiki.eventstream;

import java.util.List;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * This interface represents the descriptor of an {@link UntypedRecordableEvent}. As an {@link UntypedRecordableEvent},
 * UntypedRecordableEventDescriptors are registered
 *
 * @version $Id$
 * @since 9.6RC1
 */
@Unstable
public interface UntypedRecordableEventDescriptor extends RecordableEventDescriptor
{
    /**
     * Gets the event validation expression. This expression is a macro that, once rendered, should return "true" in
     * order to trigger the associated event.
     *
     * @return the validation expression
     */
    String getValidationExpression();

    /**
     * Get a list of the events that should trigger this particular event. Each event is represented by its canonical
     * name.
     *
     * @return the event trigger list
     */
    List<String> getEventTriggers();

    /**
     * Get the object that, associated with one of the events returned by {@link #getEventTriggers()} should trigger
     * this particular event.
     *
     * @return the triggering object type
     */
    List<String> getObjectTypes();

    /**
     * Get a {@link DocumentReference} to the author of the descriptor. This reference is useful when evaluating
     * the output of {@link #getValidationExpression()}.
     *
     * @return the author reference
     */
    DocumentReference getAuthorReference();

    /**
     * @return the velocity template that generate the list of targets
     * @since 9.11.2
     * @since 10.0
     */
    default String getTargetExpression() {
        return null;
    }
}
