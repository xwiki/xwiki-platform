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
package org.xwiki.refactoring.internal.event;

import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;

/**
 * Base class for refactoring events.
 * 
 * @version $Id$
 * @since 11.1RC1
 */
public abstract class AbstractEvent implements Event
{
    /**
     * Indicates if this event was canceled.
     **/
    private boolean canceled;

    /**
     * The reason why the event was canceled.
     **/
    private String reason;

    /**
     * Check if this event was canceled by one of the listeners.
     *
     * @return {@code true} if the event was canceled, {@code false} otherwise
     */
    public boolean isCanceled()
    {
        return this.canceled;
    }

    /**
     * Cancel the event, preventing the default behavior. The actual canceling will be performed by the code that
     * triggered the event.
     */
    public void cancel()
    {
        cancel(null);
    }

    /**
     * Cancel the event, giving a reason why. The actual canceling will be performed by the code that triggered the
     * event.
     *
     * @param reason the reason why the event was canceled
     */
    public void cancel(String reason)
    {
        if (this instanceof CancelableEvent) {
            this.canceled = true;
            this.reason = reason;
        } else {
            throw new UnsupportedOperationException("This event can't be canceled!");
        }
    }

    /**
     * Get the reason why the event was canceled.
     *
     * @return reason for cancel or {@code null} if the event was not canceled or canceled using {@link #cancel()}
     */
    public String getReason()
    {
        return this.reason;
    }

    @Override
    public boolean matches(Object occuringEvent)
    {
        return occuringEvent != null && this.getClass().isAssignableFrom(occuringEvent.getClass());
    }
}
