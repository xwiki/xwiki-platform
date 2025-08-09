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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.job.IndexerRequest;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Automatically start synchronization at startup.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
@Component
@Named("solr.initializer")
@Singleton
public class SolrIndexInitializeListener implements EventListener
{
    /**
     * The events to listen to that trigger the index update.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new ApplicationReadyEvent(), new WikiReadyEvent());

    private static final List<String> REQUEST_DEFAULT_ID = Arrays.asList("solr", "indexer");

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

    @Inject
    private SolrConfiguration configuration;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

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
        if (this.configuration.synchronizeAtStartup()) {
            SolrConfiguration.SynchronizeAtStartupMode startupMode =
                this.configuration.synchronizeAtStartupMode();
            IndexerRequest request = null;
            ArrayList<String> requestId = new ArrayList<>(REQUEST_DEFAULT_ID);

            try {
                if (event instanceof ApplicationReadyEvent) {
                    if (startupMode == SolrConfiguration.SynchronizeAtStartupMode.FARM) {
                        request = new IndexerRequest();
                        request.setId(requestId);
                    } else if (startupMode == SolrConfiguration.SynchronizeAtStartupMode.WIKI) {
                        // Startup of the application: start indexing the main wiki.
                        request = new IndexerRequest();
                        WikiReference wikiReference = this.wikiDescriptorManager.getMainWikiDescriptor().getReference();
                        request.setRootReference(wikiReference);
                        requestId.add(this.entityReferenceSerializer.serialize(wikiReference));
                        request.setId(requestId);
                    }
                } else if (startupMode == SolrConfiguration.SynchronizeAtStartupMode.WIKI
                    && event instanceof WikiReadyEvent) {
                    WikiReadyEvent wikiReadyEvent = (WikiReadyEvent) event;
                    WikiReference wikiReference = new WikiReference(wikiReadyEvent.getWikiId());
                    request = new IndexerRequest();
                    request.setRootReference(wikiReference);

                    requestId.add(this.entityReferenceSerializer.serialize(wikiReference));
                    request.setId(requestId);
                }

                if (request != null) {
                    // Remove invalid entries after the synchronization
                    request.setCleanInvalid(true);

                    this.solrIndexer.get().startIndex(request);
                }
            } catch (SolrIndexerException | WikiManagerException e) {
                this.logger.error("Failed to start initial Solr index synchronization", e);
            }
        }
    }
}
