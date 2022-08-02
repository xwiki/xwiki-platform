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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrCoreInitializer;
import org.xwiki.search.solr.SolrException;

/**
 * The entry point of access Solr cores.
 * 
 * @version $Id$
 * @since 12.3RC1
 */
public abstract class AbstractSolr implements Solr, Disposable
{
    @Inject
    protected ComponentManager componentManager;

    @Inject
    protected Logger logger;

    protected final Map<String, SolrClient> clients = new ConcurrentHashMap<>();

    @Override
    public void dispose()
    {
        for (SolrClient client : this.clients.values()) {
            try {
                client.close();
            } catch (IOException e) {
                this.logger.error("Failed to close Solr client", e);
            }
        }
    }

    @Override
    public SolrClient getClient(String name) throws SolrException
    {
        String id = StringUtils.defaultString(name);

        return this.clients.computeIfAbsent(id, this::getSynchronizedClient);
    }

    private synchronized SolrClient getSynchronizedClient(String coreName)
    {
        try {
            // Create the client
            SolrClient solrClient = getInternalSolrClient(coreName);

            // Initialize the client
            if (this.componentManager.hasComponent(SolrCoreInitializer.class, coreName)) {
                SolrCoreInitializer initializer;
                try {
                    initializer = this.componentManager.getInstance(SolrCoreInitializer.class, coreName);
                } catch (ComponentLookupException e) {
                    throw new SolrException("Failed to get the SolrCoreInitializer for core name [{}]", e);
                }

                // If no core already exist create a new core
                if (solrClient == null) {
                    solrClient = createCore(initializer);
                }

                // Custom initialization of the core
                initializer.initialize(solrClient);
            }

            return solrClient;
        } catch (SolrException e) {
            this.logger.error("Failed to create the Solr client for core with name [{}]", coreName, e);

            return null;
        }
    }

    /**
     * @param id the identifier of the core
     * @return the new core or null if no core exist by this name
     * @throws SolrException when failing to create the solr client
     */
    protected abstract SolrClient getInternalSolrClient(String coreName) throws SolrException;

    /**
     * @since 12.10
     */
    protected abstract SolrClient createCore(SolrCoreInitializer initializer) throws SolrException;
}
