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
package org.xwiki.search.solr.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.search.solr.internal.api.SolrIndexer;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.AbstractAttachmentEvent;
import com.xpn.xwiki.internal.event.AttachmentAddedEvent;
import com.xpn.xwiki.internal.event.AttachmentDeletedEvent;
import com.xpn.xwiki.internal.event.AttachmentUpdatedEvent;

/**
 * Event listener that monitors changes in the wiki and updates the Solr index accordingly.
 * 
 * @version $Id$
 * @since 5.1M2
 */
@Component
@Named("solr")
public class SolrIndexEventListener implements EventListener
{
    /**
     * The events to listen to that trigger the index update.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new DocumentUpdatedEvent(),
        new DocumentCreatedEvent(), new DocumentDeletedEvent(), new AttachmentAddedEvent(),
        new AttachmentDeletedEvent(), new AttachmentUpdatedEvent(), new WikiCreatedEvent(), new WikiDeletedEvent());

    /**
     * Logging framework.
     */
    @Inject
    protected Logger logger;

    /**
     * The solr index.
     */
    @Inject
    private SolrIndexer solrIndexer;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return this.getClass().getName();
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        try {
            if (event instanceof DocumentUpdatedEvent || event instanceof DocumentCreatedEvent) {
                XWikiDocument document = (XWikiDocument) source;

                this.solrIndexer.index(document.getDocumentReference());
            } else if (event instanceof DocumentDeletedEvent) {
                XWikiDocument document = (XWikiDocument) source;

                this.solrIndexer.delete(document.getDocumentReference());
            } else if (event instanceof AttachmentUpdatedEvent || event instanceof AttachmentAddedEvent) {
                XWikiDocument document = (XWikiDocument) source;
                String fileName = ((AbstractAttachmentEvent) event).getName();
                XWikiAttachment attachment = document.getAttachment(fileName);

                this.solrIndexer.index(attachment.getReference());
            } else if (event instanceof AttachmentDeletedEvent) {
                XWikiDocument document = (XWikiDocument) source;
                String fileName = ((AbstractAttachmentEvent) event).getName();
                XWikiAttachment attachment = document.getAttachment(fileName);

                this.solrIndexer.delete(attachment.getReference());
            } else if (event instanceof WikiCreatedEvent) {
                String wikiName = (String) source;
                WikiReference wikiReference = new WikiReference(wikiName);

                this.solrIndexer.index(wikiReference);
            } else if (event instanceof WikiDeletedEvent) {
                String wikiName = (String) source;
                WikiReference wikiReference = new WikiReference(wikiName);

                this.solrIndexer.delete(wikiReference);
            }
        } catch (Exception e) {
            logger.error("Failed to handle event [{}] with source [{}]", event, source, e);
        }
    }
}
