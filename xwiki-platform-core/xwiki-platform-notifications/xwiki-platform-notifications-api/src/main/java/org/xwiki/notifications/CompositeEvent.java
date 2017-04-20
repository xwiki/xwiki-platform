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
import java.util.List;
import java.util.Set;

import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 */
public class CompositeEvent
{
    private List<Event> events = new ArrayList<>();

    private int similarityBetweenEvents = 0;

    public CompositeEvent(Event event)
    {
        events.add(event);
    }

    public void add(Event event, int similarity) throws NotificationException
    {
        if (similarity < similarityBetweenEvents) {
            throw new NotificationException("Invalid addition of an event inside a CompositeEvent");
        }
        similarityBetweenEvents = similarity;
        events.add(event);
    }

    public int getSimilarityBetweenEvents()
    {
        return similarityBetweenEvents;
    }

    public List<Event> getEvents()
    {
        return events;
    }

    public String getType()
    {
        String type = events.get(0).getType();
        for (Event event : events) {
            if (!"create".equals(event.getType()) && !"update".equals(event.getType())) {
                type = event.getType();
            }
        }
        return type;
    }

    public String getGroupId()
    {
        return events.get(0).getGroupId();
    }

    public DocumentReference getDocument()
    {
        return events.get(0).getDocument();
    }

    public Set<DocumentReference> getUsers()
    {
        Set<DocumentReference> users = new HashSet();
        for (Event event : events) {
            users.add(event.getUser());
        }
        return users;
    }

    public List<Date> getDates()
    {
        List<Date> dates = new ArrayList<>();
        for (Event event : events) {
            dates.add(event.getDate());
        }
        Collections.sort(dates);
        return dates;
    }

    public void remove(Event event)
    {
        events.remove(event);
    }
}
