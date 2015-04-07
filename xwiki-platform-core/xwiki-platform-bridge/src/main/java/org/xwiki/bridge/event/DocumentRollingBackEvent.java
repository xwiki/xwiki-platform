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
import org.xwiki.observation.event.filter.EventFilter;

/**
 * An event triggered before a document is rolled back to a previous revision.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the current {com.xpn.xwiki.doc.XWikiDocument} instance</li>
 * <li>data: the current {com.xpn.xwiki.XWikiContext} instance</li>
 * </ul>
 * 
 * @version $Id$
 * @since 5.0M2
 */
public class DocumentRollingBackEvent extends AbstractDocumentEvent
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The revision the document is about to be rolled back to.
     */
    private String revision;

    /**
     * Matches all {@link DocumentRollingBackEvent} events.
     */
    public DocumentRollingBackEvent()
    {
        super();
    }

    /**
     * Matches {@link DocumentRollingBackEvent} events that target the specified document.
     * 
     * @param documentReference the reference of the document to match
     */
    public DocumentRollingBackEvent(DocumentReference documentReference)
    {
        this(documentReference, null);
    }

    /**
     * Matches {@link DocumentRollingBackEvent} events that target the specified document and revision. The revision is
     * matched only if it's not {@code null}.
     * 
     * @param documentReference the reference of the document to match
     * @param revision the revision the document is about to be rolled back to
     */
    public DocumentRollingBackEvent(DocumentReference documentReference, String revision)
    {
        super(documentReference);
        this.revision = revision;
    }

    /**
     * Constructor using a custom {@link EventFilter}.
     * 
     * @param eventFilter the filter to use for matching events
     */
    public DocumentRollingBackEvent(EventFilter eventFilter)
    {
        super(eventFilter);
    }

    /**
     * @return the revision the document is about to be rolled back to
     */
    public String getRevision()
    {
        return revision;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        boolean matches = super.matches(otherEvent);

        if (matches) {
            DocumentRollingBackEvent documentRollingBackEvent = (DocumentRollingBackEvent) otherEvent;
            matches = revision == null || revision.equals(documentRollingBackEvent.getRevision());
        }

        return matches;
    }
}
