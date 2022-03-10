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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.EndEvent;
import org.xwiki.observation.event.filter.EventFilter;

/**
 * An event triggered after one or several versions of a document history have been deleted.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the current {com.xpn.xwiki.doc.XWikiDocument} instance</li>
 * <li>data: the current {com.xpn.xwiki.XWikiContext} instance</li>
 * </ul>
 * 
 * @version $Id$
 * @since 13.5RC1
 */
public class DocumentVersionRangeDeletedEvent extends AbstractDocumentEvent implements EndEvent
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;

    private String from;

    private String to;

    /**
     * Matches all {@link DocumentVersionRangeDeletedEvent} events.
     */
    public DocumentVersionRangeDeletedEvent()
    {
    }

    /**
     * @param documentReference the reference of the document to match
     * @param from the oldest version deleted
     * @param to the most recent version deleted
     */
    public DocumentVersionRangeDeletedEvent(DocumentReference documentReference, String from, String to)
    {
        super(documentReference);

        this.from = from;
        this.to = to;
    }

    /**
     * Constructor using a custom {@link EventFilter} to match the document reference.
     * 
     * @param eventFilter the filter to use for matching events
     */
    public DocumentVersionRangeDeletedEvent(EventFilter eventFilter)
    {
        super(eventFilter);
    }

    /**
     * @return the from
     */
    public String getFrom()
    {
        return this.from;
    }

    /**
     * @return the to
     */
    public String getTo()
    {
        return this.to;
    }
}
