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
package org.xwiki.container.servlet.events;

import org.xwiki.observation.event.Event;

/**
 * Event triggered when a {@link jakarta.servlet.http.HttpSession} is destroyed.
 * <p>
 * This event is the XWiki event triggered when
 * {@link jakarta.servlet.http.HttpSessionListener#sessionDestroyed(jakarta.servlet.http.HttpSessionEvent)} is called.
 * <p>
 * Note that this event should *not* be serializable: we probably don't want it to be sent remotely.
 * <p>
 * The following information are also sent:
 * <ul>
 * <li>source: the {@link jakarta.servlet.http.HttpSession} that is about to be destroyed</li>
 * <li>data: null</li>
 * </ul>
 *
 * @version $Id$
 * @since 17.1.0RC1
 */
public class HttpSessionDestroyedEvent implements Event
{
    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof HttpSessionDestroyedEvent;
    }
}
