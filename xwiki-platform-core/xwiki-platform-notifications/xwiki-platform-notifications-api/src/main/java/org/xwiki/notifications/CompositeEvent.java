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
package org.xwiki.notifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;
import org.xwiki.text.StringUtils;

/**
 * A group of similar events that compose a "composite" event.
 *
 * @version $Id$
 * @since 9.4RC1
 */
@Unstable
public class CompositeEvent
{
    private List<Event> events = new ArrayList<>();

    private int similarityBetweenEvents;

    /**
     * Construct a CompositeEvent.
     * @param event the first event of the composite event
     */
    public CompositeEvent(Event event)
    {
        events.add(event);
    }

    /**
     * Copy constructor of {@link CompositeEvent}.
     *
     * @param compositeEvent the composite event to copy
     */
    public CompositeEvent(CompositeEvent compositeEvent)
    {
        this.events = new ArrayList<>(compositeEvent.events);
        this.similarityBetweenEvents = compositeEvent.similarityBetweenEvents;
    }

    /**
     * @return the list of the ids of the events
     */
    public List<String> getEventIds()
    {
        ArrayList results = new ArrayList(events.size());
        for (Event event : events) {
            results.add(event.getId());
        }
        return results;
    }

    /**
     * Add an event to the composite event.
     * @param event the event to add
     * @param similarity the similarity between the event to add and the events of the composite events
     * @throws NotificationException if the addition is illegal (lower similarity for example)
     */
    public void add(Event event, int similarity) throws NotificationException
    {
        if (similarity < similarityBetweenEvents) {
            throw new NotificationException("Invalid addition of an event inside a CompositeEvent");
        }
        similarityBetweenEvents = similarity;
        events.add(event);
        // Ensure the events are always sorted by date (more recent firsts)
        Collections.sort(events, (e1, e2) -> e2.getDate().compareTo(e1.getDate()));
    }

    /**
     * @return the greatest similarity between events of the composite event
     */
    public int getSimilarityBetweenEvents()
    {
        return similarityBetweenEvents;
    }

    /**
     * @return the events that compose the current object
     */
    public List<Event> getEvents()
    {
        return events;
    }

    /**
     * @return the type of the events that compose the current object
     */
    public String getType()
    {
        // Fallback type
        String type = events.get(0).getType();
        for (Event event : events) {
            // We are most interested in "advanced" event that we are in "core" events such as "create" or "update",
            // which often are the technical consequences of the real event (ex: a comment has been added).
            if (StringUtils.isNotBlank(event.getType()) && !"create".equals(event.getType())
                    && !"update".equals(event.getType())) {
                type = event.getType();
            }
        }
        return type;
    }

    /**
     * @return the groupId of the first event of the current object
     */
    public String getGroupId()
    {
        return events.get(0).getGroupId();
    }

    /**
     * @return the document of the first event of the current object
     */
    public DocumentReference getDocument()
    {
        return events.get(0).getDocument();
    }

    /**
     * @return the users who performed the events
     */
    public Set<DocumentReference> getUsers()
    {
        Set<DocumentReference> users = new HashSet();
        for (Event event : events) {
            users.add(event.getUser());
        }
        return users;
    }

    /**
     * @return the date of the most recent event
     * @since 9.11RC1
     */
    public Date getDate()
    {
        Iterator<Event> it = events.iterator();
        Event chosenEvent = it.next();
        while (it.hasNext()) {
            Event event = it.next();
            if (event.getDate().compareTo(chosenEvent.getDate()) > 0) {
                chosenEvent = event;
            }
        }
        return chosenEvent.getDate();
    }

    /**
     * @return the dates of the events, sorted by descending order
     */
    public List<Date> getDates()
    {
        List<Date> dates = new ArrayList<>();
        for (Event event : events) {
            dates.add(event.getDate());
        }
        Collections.sort(dates, Collections.reverseOrder());
        return dates;
    }

    /**
     * Remove an event from the current object.
     * @param event the event to remove
     */
    public void remove(Event event)
    {
        events.remove(event);
    }
}
