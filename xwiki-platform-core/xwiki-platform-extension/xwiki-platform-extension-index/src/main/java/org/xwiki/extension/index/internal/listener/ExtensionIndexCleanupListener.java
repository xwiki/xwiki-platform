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
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer;
import org.xwiki.extension.index.internal.ExtensionIndexSolrUtil;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;

import static org.xwiki.extension.InstalledExtension.FIELD_INSTALLED_NAMESPACES;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.IS_INSTALLED_EXTENSION;
import static org.xwiki.search.solr.AbstractSolrCoreInitializer.SOLR_FIELD_ID;

/**
 * Cleanup the extension index of entries related to artifacts that are not available anymore (e.g., upgrade or manual
 * manipulation of the directories storing jar files).
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
@Named("ExtensionIndexCleanupListener")
public class ExtensionIndexCleanupListener implements EventListener
{
    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private Solr solr;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private ExtensionIndexSolrUtil extensionIndexSolrUtil;

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return "ExtensionIndexCleanupListener";
    }

    @Override
    public List<Event> getEvents()
    {
        return List.of(new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        Thread thread = new Thread(this::proceed);
        thread.setPriority(thread.getPriority() - 2);
        thread.start();
    }

    /**
     * This method is package-protected for testing purpose. It is not advised to use it outside of the current class.
     */
    void proceed()
    {
        try {
            SolrClient client = this.solr.getClient(ExtensionIndexSolrCoreInitializer.NAME);
            SolrQuery solrQuery = new SolrQuery()
                .setFields(SOLR_FIELD_ID, IS_INSTALLED_EXTENSION, FIELD_INSTALLED_NAMESPACES);
            int batchSize = 1000;
            QueryResponse search = client.query(solrQuery.setRows(batchSize).setStart(0));

            // Compute lazily of a non-core extension can be removed (i.e., it has at least one namespace but can't 
            // be found in the installed extensions repository).
            BiPredicate<SolrDocument, ExtensionId> canDeleteNotCore = (doc, extensionId) ->
                doc.getFieldValue(FIELD_INSTALLED_NAMESPACES) != null
                    && !this.installedExtensionRepository.exists(extensionId);
            while (!search.getResults().isEmpty()) {
                for (SolrDocument doc : search.getResults()) {
                    String id = this.solrUtils.getId(doc);
                    ExtensionId extensionId = this.extensionIndexSolrUtil.fromSolrId(id);
                    boolean isCoreExtension = Objects.equals(doc.getFieldValue(IS_INSTALLED_EXTENSION), false);
                    if ((isCoreExtension && !this.coreExtensionRepository.exists(extensionId))
                        || (!isCoreExtension && canDeleteNotCore.test(doc, extensionId)))
                    {
                        client.deleteById(id);
                    }
                }
                search = client.query(solrQuery.setStart(solrQuery.getStart() + batchSize));
            }

            client.commit();
        } catch (SolrException e) {
            this.logger.error("Failed to get a solr client for the [{}] index.",
                ExtensionIndexSolrCoreInitializer.NAME, e);
        } catch (SolrServerException | IOException e) {
            this.logger.error("Failed to perform a solr query.", e);
        }
    }
}
