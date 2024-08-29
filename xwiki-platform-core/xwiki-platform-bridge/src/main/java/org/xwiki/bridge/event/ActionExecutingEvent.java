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
package org.xwiki.bridge.event;

import org.xwiki.observation.event.BeginEvent;
import org.xwiki.observation.event.CancelableEvent;

/**
 * An event triggered whenever a client request (action) is processed, like {@code /upload/} or {@code /view/}. A
 * specific event corresponds to only one {@link #getActionName()}  action type.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the current {com.xpn.xwiki.doc.XWikiDocument} instance</li>
 * <li>data: the current {com.xpn.xwiki.XWikiContext} instance</li>
 * </ul>
 * 
 * @version $Id$
 * @since 3.2M3
 */
// TODO: use the enumerated Action class when it's implemented
public class ActionExecutingEvent extends AbstractActionExecutionEvent implements CancelableEvent, BeginEvent
{
    /**
     * Flag storing the state of this event.
     */
    private boolean canceled;

    /**
     * The reason why the event was canceled.
     */
    private String reason;

    /**
     * Match any {@link ActionExecutingEvent}.
     */
    public ActionExecutingEvent()
    {

    }

    /**
     * Constructor initializing the action name of the event.
     * 
     * @param actionName the name of the executed action
     */
    public ActionExecutingEvent(String actionName)
    {
        super(actionName);
    }

    @Override
    public boolean isCanceled()
    {
        return this.canceled;
    }

    @Override
    public void cancel()
    {
        this.canceled = true;
    }

    @Override
    public void cancel(String reason)
    {
        this.canceled = true;
        this.reason = reason;
    }

    @Override
    public String getReason()
    {
        return this.reason;
    }
}
