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
package org.xwiki.extension.index.internal.listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer;
import org.xwiki.extension.index.internal.ExtensionIndexSolrUtil;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;

/**
 * Cleanup the extension index of entries related to artifacts that are not available anymore (e.g., upgrade or manual
 * manipulation of the directories storing jar files).
 *
 * @version $Id$
 * @since 15.9RC1
 * @since 15.5.3
 */
@Component
@Singleton
@Named("ExtensionIndexApplicationStartedListener")
public class ExtensionIndexApplicationStartedListener implements EventListener
{
    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    @Inject
    private LocalExtensionRepository localExtensionRepository;

    @Inject
    private Solr solr;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private ExtensionIndexSolrUtil extensionIndexSolrUtil;

    @Override
    public String getName()
    {
        return "ExtensionIndexApplicationStartedListener";
    }

    @Override
    public List<Event> getEvents()
    {
        return List.of(new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        try {
            SolrClient client = this.solr.getClient(ExtensionIndexSolrCoreInitializer.NAME);
            SolrQuery solrQuery = new SolrQuery();
            int batchSize = 1000;
            QueryResponse search = client.query(solrQuery.setRows(batchSize).setStart(0));
            List<String> idsToRemove = new ArrayList<>();
            while (!search.getResults().isEmpty()) {
                for (SolrDocument doc : search.getResults()) {
                    String id = this.solrUtils.getId(doc);
                    ExtensionId extensionId = this.extensionIndexSolrUtil.fromSolrId(id);
                    if (!this.coreExtensionRepository.exists(extensionId) && !this.localExtensionRepository.exists(
                        extensionId))
                    {
                        idsToRemove.add(id);
                    }
                }
                search = client.query(solrQuery.setStart(solrQuery.getStart() + batchSize));
            }

            client.deleteById(idsToRemove);
            client.commit();
        } catch (SolrException e) {
            throw new RuntimeException(String.format("Failed to get the solr client for the [%s] index.",
                ExtensionIndexSolrCoreInitializer.NAME), e);
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
