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
package org.xwiki.extension.index.internal.job;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.IterableUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.index.ExtensionIndexStatus;
import org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer;
import org.xwiki.extension.index.internal.ExtensionIndexStore;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.repository.search.Searchable;
import org.xwiki.extension.version.Version;
import org.xwiki.job.AbstractJob;

/**
 * Update the index from configured repositories.
 * 
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Named(ExtensionIndexJob.JOB_TYPE)
public class ExtensionIndexJob extends AbstractJob<ExtensionIndexRequest, ExtensionIndexStatus>
{
    /**
     * Type of the job.
     */
    public static final String JOB_TYPE = "extendion.index";

    private static final int SEARCH_BATCH_SIZE = 100;

    @Inject
    private ExtensionIndexStore indexStore;

    @Inject
    private ExtensionRepositoryManager repositories;

    @Inject
    private LocalExtensionRepository localExtensions;

    @Inject
    private CoreExtensionRepository coreExtensions;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected ExtensionIndexStatus createNewStatus(ExtensionIndexRequest request)
    {
        return new DefaultExtensionIndexStatus(request, this.observationManager, this.loggerManager);
    }

    @Override
    protected void runInternal() throws Exception
    {
        // 1: Add local extensions
        boolean updated = addLocalExtensions();

        // 1: Gather all extension from searchable repositories
        updated |= addRemoteExtensions();

        if (updated) {
            // 2: Gather other versions
            updateVersions();

            // 2: Validate extensions to figure out if they are compatible
            validateExtensions();

            // TODO: 3: Analyze extensions to find specific metadata depending on the type
        }
    }

    private void validateExtensions()
    {
        
    }

    private void updateVersions() throws SearchException, SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LAST + ':' + true);

        List<ExtensionId> extensoinIds = this.indexStore.searchExtensionIds(solrQuery);
        Map<String, SortedSet<Version>> extensions = new HashMap<>(extensoinIds.size());
        for (ExtensionId extensionId : extensoinIds) {
            extensions.computeIfAbsent(extensionId.getId(), key -> new TreeSet<>()).add(extensionId.getVersion());
        }

        extensions.forEach(this::updateVersions);

        this.indexStore.commit();
    }

    private void updateVersions(String id, SortedSet<Version> storedVersions)
    {
        // Get all available versions
        IterableResult<Version> versions;
        try {
            versions = this.repositories.resolveVersions(id, 0, -1);
        } catch (ResolveException e) {
            return;
        }

        // TODO: remove indexed extensions not part of the resolved versions ?

        List<Version> newVersions = IterableUtils.toList(versions);

        for (Iterator<Version> it = storedVersions.iterator(); it.hasNext();) {
            Version version = it.next();

            ExtensionId extensionId = new ExtensionId(id, version);
            try {
                this.indexStore.update(extensionId, !it.hasNext(), newVersions);
            } catch (Exception e) {
                this.logger.error("Failed to update the extension [{}]", extensionId, e);
            }
        }
    }

    private Set<ExtensionId> getSearchableExtensionIds() throws SearchException
    {
        Set<ExtensionId> extensions = new HashSet<>();

        for (ExtensionRepository repository : this.repositories.getRepositories()) {
            if (repository instanceof Searchable) {
                Searchable searchableRepository = (Searchable) repository;

                for (int offset = 0; true; offset += SEARCH_BATCH_SIZE) {
                    IterableResult<Extension> result = searchableRepository.search("", offset, SEARCH_BATCH_SIZE);

                    result.forEach(extension -> extensions.add(extension.getId()));

                    if (result.getSize() < SEARCH_BATCH_SIZE) {
                        break;
                    }
                }
            }
        }

        return extensions;
    }

    private boolean addLocalExtensions()
    {
        boolean updated = false;

        this.localExtensions.getLocalExtensions();

        // Add the extension

        return updated;
    }

    private boolean addRemoteExtensions() throws SearchException, ResolveException, SolrServerException, IOException
    {
        boolean updated = false;

        // Search result are not always fully complete and we want all versions so we just keep the id and go through
        // repositories
        Set<ExtensionId> extensionIds = getSearchableExtensionIds();

        for (ExtensionId extensionId : extensionIds) {
            updated |= addExtension(extensionId);
        }

        // Make sure all found extensions are in the store
        this.indexStore.commit();

        return updated;
    }

    private boolean addExtension(ExtensionId extensionId) throws SolrServerException, IOException, ResolveException
    {
        // Not add it if it's a core extension
        if (this.coreExtensions.exists(extensionId)) {
            return false;
        }

        // Not add it if it's a local extension
        if (this.localExtensions.exists(extensionId)) {
            return false;
        }

        // Check if the extension already exist
        if (!this.indexStore.exists(extensionId)) {
            // Get extension
            Extension extension = this.repositories.resolve(extensionId);

            // Add the extension
            return this.indexStore.add(extension, true, true);
        }

        return false;
    }
}
