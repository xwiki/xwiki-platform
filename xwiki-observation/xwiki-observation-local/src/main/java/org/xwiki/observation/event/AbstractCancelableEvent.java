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
package org.xwiki.observation.event;

import org.xwiki.observation.event.filter.EventFilter;


/**
 * Abstract base class for cancelable events.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public abstract class AbstractCancelableEvent extends AbstractFilterableEvent implements CancelableEvent
{
    /** Serial version ID. Increment only if the <i>serialized</i> version of this class changes. */
    private static final long serialVersionUID = 1L;

    /** Flag storing the state of this event. */
    private boolean canceled;

    /** The reason why the event was canceled. */
    private String reason;

    /**
     * Constructor initializing the event filter with an
     * {@link org.xwiki.observation.event.filter.AlwaysMatchingEventFilter}, meaning that this event will match any
     * other event of the same type.
     */
    protected AbstractCancelableEvent()
    {
    }

    /**
     * Constructor initializing the event filter with a {@link org.xwiki.observation.event.filter.FixedNameEventFilter},
     * meaning that this event will match only events of the same type affecting the same passed name.
     *
     * @param name a generic name that uniquely identifies an event type
     */
    protected AbstractCancelableEvent(String name)
    {
        super(name);
    }

    /**
     * Constructor using a custom {@link EventFilter}.
     *
     * @param eventFilter the filter to use for matching events
     */
    protected AbstractCancelableEvent(EventFilter eventFilter)
    {
        super(eventFilter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.event.CancelableEvent#isCanceled()
     */
    public boolean isCanceled()
    {
        return this.canceled;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.event.CancelableEvent#cancel()
     */
    public void cancel()
    {
        this.canceled = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.event.CancelableEvent#cancel(java.lang.String)
     */
    public void cancel(String reason)
    {
        this.canceled = true;
        this.reason = reason;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.event.CancelableEvent#getReason()
     */
    public String getReason()
    {
        return this.reason;
    }
}

