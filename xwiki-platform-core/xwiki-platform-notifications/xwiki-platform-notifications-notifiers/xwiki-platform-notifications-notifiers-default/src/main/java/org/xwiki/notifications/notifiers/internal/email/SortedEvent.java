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
package org.xwiki.notifications.notifiers.internal.email;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.xwiki.notifications.CompositeEvent;

/**
 * Wrap a {@link CompositeEvent} so that we can store the HTML-rendered and plain text-rendered version of the event
 * next to the event.
 *
 * @version $Id$
 * @since 9.11RC1
 */
public class SortedEvent
{
    private CompositeEvent event;

    private String html;

    private String plainText;

    private List<SortedEvent> children = new ArrayList<>();

    /**
     * Create a SortedEvent.
     * @param event the event to wrap
     * @param html the HTML-rendered version of the event
     * @param plainText the plain text-rendered version of the event
     */
    public SortedEvent(CompositeEvent event, String html, String plainText)
    {
        this.event = event;
        this.html = html;
        this.plainText = plainText;
    }

    /**
     * @return get the event
     */
    public CompositeEvent getEvent()
    {
        return event;
    }

    /**
     * @return the HTML-rendered version of the event
     */
    public String getHtml()
    {
        return html;
    }

    /**
     * @return the plain text-rendered version of the event
     */
    public String getPlainText()
    {
        return plainText;
    }

    /**
     * Add an event that concern a document that is a child of the document concern by this event.
     * @param event the event to add as a child
     */
    public void addChild(SortedEvent event)
    {
        children.add(event);
    }

    /**
     * @return the events concerning children of the document concern by this event
     */
    public List<SortedEvent> getChildren()
    {
        Collections.sort(children, Comparator.comparing(e -> e.getEvent().getDocument()));
        return children;
    }

    /**
     * @param sortedEvent event to compare
     * @return true if the current event concerns a document that is a parent of the document concerned by the other
     * event.
     */
    public boolean isParent(SortedEvent sortedEvent)
    {
        return !event.getDocument().equals(sortedEvent.getEvent().getDocument())
                && event.getDocument().getName().equals("WebHome")
                && sortedEvent.getEvent().getDocument().hasParent(event.getDocument().getLastSpaceReference());
    }

    /**
     * @return true if the event has children
     */
    public boolean hasChildren()
    {
        return !children.isEmpty();
    }
}
