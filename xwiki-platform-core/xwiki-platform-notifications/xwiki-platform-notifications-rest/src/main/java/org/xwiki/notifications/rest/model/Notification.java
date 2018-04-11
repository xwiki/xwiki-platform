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

import java.util.Collection;
import java.util.Date;

import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;

/**
 * @version $Id$
 * @since 10.3RC1
 */
public class Notification
{
    private CompositeEvent compositeEvent;

    private boolean read;

    private String html;

    private String exception;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    public Notification(CompositeEvent compositeEvent, boolean read, String html, String exception,
            EntityReferenceSerializer<String> entityReferenceSerializer)
    {
        this.compositeEvent = compositeEvent;
        this.read = read;
        this.html = html;
        this.exception = exception;
        this.entityReferenceSerializer = entityReferenceSerializer;
    }

    public Collection<String> getIds()
    {
        return compositeEvent.getEventIds();
    }

    public String getType()
    {
        return compositeEvent.getType();
    }

    public boolean isRead()
    {
        return read;
    }

    public void setRead(boolean read)
    {
        this.read = read;
    }

    public Collection<Date> getDates()
    {
        return compositeEvent.getDates();
    }

    public String getDocument()
    {
        return compositeEvent.getDocument() != null
                ? entityReferenceSerializer.serialize(compositeEvent.getDocument())
                : null;
    }

    public String getHtml()
    {
        return html;
    }

    public void setHtml(String html)
    {
        this.html = html;
    }

    public String getException()
    {
        return exception;
    }

    public void setException(String exception)
    {
        this.exception = exception;
    }
}
