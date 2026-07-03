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
package com.xpn.xwiki.events;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.DocumentEventType;

/**
 * Descriptor for the {@link org.xwiki.bridge.event.DocumentDeletedEvent}.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
@Named(DocumentDeletedEventDescriptor.EVENT_TYPE)
public class DocumentDeletedEventDescriptor extends AbstractXWikiRecordableEventDescriptor
{
    /**
     * Name of the supported type (as it is stored in Activity Stream).
     */
    public static final String EVENT_TYPE = DocumentEventType.DELETE;

    /**
     * Construct a DocumentDeletedEventDescriptor.
     */
    public DocumentDeletedEventDescriptor()
    {
        super("core.events.delete.description",
                "core.events.appName");
    }

    @Override
    public String getEventType()
    {
        // Match the name used by Activity Stream.
        return EVENT_TYPE;
    }

    @Override
    public EventFilter getFilter()
    {
        return EventFilter.WIKI_SPACE_AND_DOCUMENT_FILTER;
    }

    @Override
    public String getEventTypeIcon()
    {
        return "trash";
    }
}
