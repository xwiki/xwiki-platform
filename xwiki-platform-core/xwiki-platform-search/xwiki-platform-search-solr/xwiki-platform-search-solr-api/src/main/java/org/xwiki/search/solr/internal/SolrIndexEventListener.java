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
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
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
import com.xpn.xwiki.internal.event.EntityEvent;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyAddedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;

/**
 * Event listener that monitors changes in the wiki and updates the Solr index accordingly.
 * 
 * @version $Id$
 * @since 5.1M2
 */
@Component
@Named("solr.update")
@Singleton
public class SolrIndexEventListener implements EventListener
{
    /**
     * The events to listen to that trigger the index update.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new DocumentUpdatedEvent(),
        new DocumentCreatedEvent(), new DocumentDeletedEvent(), new AttachmentAddedEvent(),
        new AttachmentDeletedEvent(), new AttachmentUpdatedEvent(), new XObjectAddedEvent(), new XObjectDeletedEvent(),
        new XObjectUpdatedEvent(), new XObjectPropertyAddedEvent(), new XObjectPropertyDeletedEvent(),
        new XObjectPropertyUpdatedEvent(), new WikiDeletedEvent());

    /**
     * Logging framework.
     */
    @Inject
    protected Logger logger;

    /**
     * The solr index.
     * <p>
     * Lazily initialize the {@link SolrIndexer} to not initialize it too early.
     */
    @Inject
    private Provider<SolrIndexer> solrIndexer;

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
            if (event instanceof DocumentUpdatedEvent) {
                XWikiDocument document = (XWikiDocument) source;

                this.solrIndexer.get().index(document.getDocumentReference(), false);
            } else if (event instanceof DocumentCreatedEvent) {
                XWikiDocument document = (XWikiDocument) source;

                if (!Locale.ROOT.equals(document.getLocale())) {
                    // If a new translation is added to a document reindex the whole document (could be optimized a bit
                    // by reindexing only the parent locales but that would always include objects and attachments
                    // anyway)
                    this.solrIndexer.get().index(new DocumentReference(document.getDocumentReference(), null), true);
                } else {
                    this.solrIndexer.get().index(
                        new DocumentReference(document.getDocumentReference(), document.getLocale()), false);
                }
            } else if (event instanceof DocumentDeletedEvent) {
                XWikiDocument document = ((XWikiDocument) source).getOriginalDocument();

                this.solrIndexer.get().delete(
                    new DocumentReference(document.getDocumentReference(), document.getLocale()), false);
            } else if (event instanceof AttachmentUpdatedEvent || event instanceof AttachmentAddedEvent) {
                XWikiDocument document = (XWikiDocument) source;
                String fileName = ((AbstractAttachmentEvent) event).getName();
                XWikiAttachment attachment = document.getAttachment(fileName);

                this.solrIndexer.get().index(attachment.getReference(), false);
            } else if (event instanceof AttachmentDeletedEvent) {
                XWikiDocument document = ((XWikiDocument) source).getOriginalDocument();
                String fileName = ((AbstractAttachmentEvent) event).getName();
                XWikiAttachment attachment = document.getAttachment(fileName);

                this.solrIndexer.get().delete(attachment.getReference(), false);
            } else if (event instanceof XObjectUpdatedEvent || event instanceof XObjectAddedEvent) {
                EntityEvent entityEvent = (EntityEvent) event;

                this.solrIndexer.get().index(entityEvent.getReference(), false);
            } else if (event instanceof XObjectDeletedEvent) {
                EntityEvent entityEvent = (EntityEvent) event;

                this.solrIndexer.get().delete(entityEvent.getReference(), false);
            } else if (event instanceof XObjectPropertyUpdatedEvent || event instanceof XObjectPropertyAddedEvent) {
                EntityEvent entityEvent = (EntityEvent) event;

                this.solrIndexer.get().index(entityEvent.getReference(), false);
            } else if (event instanceof XObjectPropertyDeletedEvent) {
                EntityEvent entityEvent = (EntityEvent) event;

                this.solrIndexer.get().delete(entityEvent.getReference(), false);
            } else if (event instanceof WikiDeletedEvent) {
                String wikiName = (String) source;
                WikiReference wikiReference = new WikiReference(wikiName);

                this.solrIndexer.get().delete(wikiReference, false);
            }
        } catch (Exception e) {
            this.logger.error("Failed to handle event [{}] with source [{}]", event, source, e);
        }
    }
}
