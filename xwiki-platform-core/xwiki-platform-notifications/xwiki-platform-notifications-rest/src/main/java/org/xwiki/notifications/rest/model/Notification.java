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
package org.xwiki.notifications.rest.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;

/**
 * Represent a serializable version of a notification, retro-compatible with the old notification services.
 *
 * @version $Id$
 * @since 10.4RC1
 */
public class Notification implements Serializable
{
    private CompositeEvent compositeEvent;

    private boolean read;

    private String html;

    private String exception;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Construct a Notification.
     * @param compositeEvent composite event that this notification is handling
     * @param read either or not the current usr has already read this notification
     * @param html html version of the notification
     * @param exception stacktrace of an exception if an error has occurred while rendering this notification
     * @param entityReferenceSerializer serializer to use for document references
     */
    public Notification(CompositeEvent compositeEvent, boolean read, String html, String exception,
            EntityReferenceSerializer<String> entityReferenceSerializer)
    {
        this.compositeEvent = compositeEvent;
        this.read = read;
        this.html = html;
        this.exception = exception;
        this.entityReferenceSerializer = entityReferenceSerializer;
    }

    /**
     * @return the list of the ids of the events that compose the inner composite event
     */
    public Collection<String> getIds()
    {
        return compositeEvent.getEventIds();
    }

    /**
     * @return the type of the inner composite event
     */
    public String getType()
    {
        return compositeEvent.getType();
    }

    /**
     * @return either or not the current usr has already read this notification
     */
    public boolean isRead()
    {
        return read;
    }

    /**
     * @return the dates of the inner events, sorted by descending order
     */
    public Collection<Date> getDates()
    {
        return compositeEvent.getDates();
    }

    /**
     * @return the serialized document reference if the notification concerns a document, null otherwise
     */
    public String getDocument()
    {
        return compositeEvent.getDocument() != null
                ? entityReferenceSerializer.serialize(compositeEvent.getDocument())
                : null;
    }

    /**
     * @return html version of the notification
     */
    public String getHtml()
    {
        return html;
    }

    /**
     * @return the stacktrace of an exception if an error has occurred while rendering this notification, null otherwise
     */
    public String getException()
    {
        return exception;
    }
}
