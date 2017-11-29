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
import java.util.Date;
import java.util.List;

import org.xwiki.model.reference.DocumentReference;
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

    private List<SortedEvent> eventsWithTheSameDocument = new ArrayList<>();

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
        DocumentReference document = getDocument();
        return document != null
                && !document.equals(sortedEvent.getDocument())
                && document.getName().equals("WebHome")
                && sortedEvent.getDocument().hasParent(document.getLastSpaceReference());
    }

    /**
     * @return true if the event has children
     */
    public boolean hasChildren()
    {
        return !children.isEmpty();
    }

    /**
     * @return a list of sorted events that concern the same document
     */
    public List<SortedEvent> getEventsWithTheSameDocument()
    {
        return eventsWithTheSameDocument;
    }

    /**
     * Add a sorted event in the list of events that concern the same document.
     * @param event event to add
     */
    public void addEventWithTheSameDocument(SortedEvent event)
    {
        eventsWithTheSameDocument.add(event);
        if (event.hasChildren()) {
            children.addAll(event.children);
            event.children.clear();
        }
    }

    /**
     * @return document concerned by the event (can be null)
     */
    public DocumentReference getDocument()
    {
        return event.getDocument();
    }

    /**
     * @return the date of the most recent event
     */
    public Date getDate()
    {
        return event.getDate();
    }
}
