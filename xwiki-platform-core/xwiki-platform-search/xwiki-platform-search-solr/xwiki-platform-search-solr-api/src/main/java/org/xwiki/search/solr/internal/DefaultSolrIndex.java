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
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.search.solr.internal.api.SolrIndex;
import org.xwiki.search.solr.internal.api.SolrIndexException;
import org.xwiki.search.solr.internal.api.SolrInstance;

/**
 * Default implementation of the index.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Singleton
public class DefaultSolrIndex implements SolrIndex, EventListener
{
    /**
     * The events to listen to.
     */
    private static List<Event> EVENTS = Arrays.asList((Event) new ApplicationReadyEvent());

    /**
     * Logging framework.
     */
    @Inject
    protected Logger logger;

    /**
     * Communication with the Solr instance.
     */
    @Inject
    protected Provider<SolrInstance> solrInstanceProvider;

    /**
     * Extract contained indexable references.
     */
    @Inject
    protected IndexableReferenceExtractor indexableReferenceExtractor;

    /**
     * Thread in which the indexUpdater will be executed.
     */
    private Thread indexThread;

    /**
     * Component in charge with updating the index.
     */
    @Inject
    @Named("queued")
    private SolrIndex indexUpdater;

    /**
     * TODO DOCUMENT ME!
     */
    public void startThread()
    {
        // Launch the index thread that runs the indexUpdater.
        this.indexThread = new Thread((QueuedSolrIndex) indexUpdater);
        this.indexThread.start();
    }

    @Override
    public void index(EntityReference reference) throws SolrIndexException
    {
        index(Arrays.asList(reference));
    }

    @Override
    public void index(List<EntityReference> references) throws SolrIndexException
    {
        // Build the list of references to index directly
        List<EntityReference> indexableReferences = getUniqueIndexableEntityReferences(references);

        indexUpdater.index(indexableReferences);
    }

    @Override
    public void delete(EntityReference reference) throws SolrIndexException
    {
        delete(Arrays.asList(reference));
    }

    @Override
    public void delete(List<EntityReference> references) throws SolrIndexException
    {
        // Preserve consistency by deleting all the indexable entities contained by each input reference.
        List<EntityReference> indexableReferences = getUniqueIndexableEntityReferences(references);

        indexUpdater.delete(indexableReferences);
    }

    /**
     * @param startReferences the references from where to start the search from.
     * @return the unique list of indexable references starting from each of the input start references.
     * @throws SolrIndexException if problems occur.
     */
    protected List<EntityReference> getUniqueIndexableEntityReferences(List<EntityReference> startReferences)
        throws SolrIndexException
    {
        List<EntityReference> result = new ArrayList<EntityReference>();

        for (EntityReference reference : startReferences) {
            // Avoid duplicates
            if (result.contains(reference)) {
                continue;
            }

            List<EntityReference> containedReferences = indexableReferenceExtractor.getReferences(reference);
            for (EntityReference containedReference : containedReferences) {
                // Avoid duplicates again
                if (result.contains(containedReference)) {
                    continue;
                }

                result.add(containedReference);
            }
        }

        return result;
    }

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
        // Launch the index thread when XWiki has started.
        startThread();
    }
}
