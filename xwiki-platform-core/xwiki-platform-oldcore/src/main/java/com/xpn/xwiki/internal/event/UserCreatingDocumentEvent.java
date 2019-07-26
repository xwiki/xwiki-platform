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
package com.xpn.xwiki.internal.event;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.model.reference.DocumentReference;

/**
 * Same as {@link DocumentUpdatingEvent} but with an information about which user is responsible for the modifications.
 * 
 * @version $Id$
 * @since 11.6
 * @since 10.11.10
 */
public class UserCreatingDocumentEvent extends DocumentCreatingEvent implements UserEvent
{
    private DocumentReference userReference;

    /**
     * Constructor initializing the event filter with an
     * {@link org.xwiki.observation.event.filter.AlwaysMatchingEventFilter}, meaning that this event will match any
     * other document delete event.
     */
    public UserCreatingDocumentEvent()
    {
    }

    /**
     * Constructor initializing the event filter with a {@link org.xwiki.observation.event.filter.FixedNameEventFilter},
     * meaning that this event will match only delete events affecting the same document.
     * 
     * @param userReference the user responsible for the modifications
     * @param documentReference the reference of the document to match
     */
    public UserCreatingDocumentEvent(DocumentReference userReference, DocumentReference documentReference)
    {
        super(documentReference);

        this.userReference = userReference;
    }

    /**
     * @return the the user responsible for the modifications
     */
    @Override
    public DocumentReference getUserReference()
    {
        return this.userReference;
    }
}
