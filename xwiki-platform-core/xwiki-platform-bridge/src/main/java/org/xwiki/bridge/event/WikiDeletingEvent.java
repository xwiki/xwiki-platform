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

import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.stability.Unstable;

/**
 * An event triggered just before a wiki is deleted.
 * <p>
 * All the events triggered during the deletion of the wiki (for example the deletion of its descriptor document) are
 * received between this event and either a {@link WikiDeletedEvent} or a {@link WikiDeleteFailedEvent}.
 * </p>
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the wiki identifier as {@link String}</li>
 * <li>data: null</li>
 * </ul>
 *
 * @version $Id$
 * @since 18.10.0RC1
 * @since 18.9.0
 * @since 18.4.8
 * @since 17.10.15
 */
@Unstable
public class WikiDeletingEvent extends AbstractWikiEvent implements BeginFoldEvent
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Matches all {@link WikiDeletingEvent} events.
     */
    public WikiDeletingEvent()
    {

    }

    /**
     * Constructor initializing the event filter with a {@link org.xwiki.observation.event.filter.FixedNameEventFilter},
     * meaning that this event will match only events affecting the same wiki.
     *
     * @param wikiId the wiki identifier
     */
    public WikiDeletingEvent(String wikiId)
    {
        super(wikiId);
    }

    /**
     * Constructor using a custom {@link EventFilter}.
     *
     * @param eventFilter the filter to use for matching events
     */
    public WikiDeletingEvent(EventFilter eventFilter)
    {
        super(eventFilter);
    }
}
