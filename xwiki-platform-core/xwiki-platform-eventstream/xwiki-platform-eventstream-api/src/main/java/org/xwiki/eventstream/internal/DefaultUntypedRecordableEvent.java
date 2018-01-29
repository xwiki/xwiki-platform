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
package org.xwiki.eventstream.internal;

import java.util.Collections;
import java.util.Set;

import org.xwiki.eventstream.TargetableEvent;
import org.xwiki.eventstream.UntypedRecordableEvent;

/**
 * Default implementation of the {@link UntypedRecordableEvent} interface.
 *
 * @version $Id$
 * @since 9.6RC1
 */
public class DefaultUntypedRecordableEvent implements UntypedRecordableEvent, TargetableEvent
{
    private String eventType;

    private Set<String> target;

    DefaultUntypedRecordableEvent(String eventType)
    {
        this.eventType = eventType;
        this.target = Collections.emptySet();
    }

    DefaultUntypedRecordableEvent()
    {
        this.eventType = "";
        this.target = Collections.emptySet();
    }

    /**
     * Construct a DefaultUntypedRecordableEvent.
     * @param eventType event type
     * @param target list of target (can't be null !)
     *
     * @since 9.11.2
     * @since 10.0
     */
    DefaultUntypedRecordableEvent(String eventType, Set<String> target)
    {
        this.eventType = eventType;
        this.target = target;
    }

    @Override
    public String getEventType()
    {
        return this.eventType;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent != null && otherEvent instanceof UntypedRecordableEvent;
    }

    @Override
    public Set<String> getTarget()
    {
        return this.target;
    }
}
