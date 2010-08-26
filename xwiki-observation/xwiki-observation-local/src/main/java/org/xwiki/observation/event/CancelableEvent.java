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


/**
 * This event can be canceled by the receiver. Sender of this event must take care of undoing any changes in this case.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public interface CancelableEvent extends Event
{
    /**
     * Check if this event was canceled by one of the receivers.
     * 
     * @return true if the event was canceled, false otherwise
     */
    boolean isCanceled();

    /**
     * Cancel the event. The actual canceling will be performed by the sender.
     */
    void cancel();

    /**
     * Cancel the event, giving a reason why. The actual canceling will be performed by the sender.
     * 
     * @param reason the reason why the event was canceled
     */
    void cancel(String reason);

    /**
     * Get the reason why the event was canceled.
     * 
     * @return reason for cancel or null of the event was not canceled or canceled using {@link #cancel()}
     */
    String getReason();
}

