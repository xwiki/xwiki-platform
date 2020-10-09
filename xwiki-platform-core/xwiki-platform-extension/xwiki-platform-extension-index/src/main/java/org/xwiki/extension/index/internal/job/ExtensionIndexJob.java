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
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.index.ExtensionIndexStatus;
import org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.version.Version;
import org.xwiki.job.AbstractJob;
import org.xwiki.search.solr.Solr;

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
    private Solr solr;

    @Inject
    private ExtensionRepositoryManager repositories;

    @Inject
    private LocalExtensionRepository localExtensions;

    @Inject
    private CoreExtensionRepository coreExtensions;

    private SolrClient client;

    private int documentsToStore;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    private void commit()
    {
        // Reset counter
        this.documentsToStore = 0;

        this.client.commit();
    }

    @Override
    protected void runInternal() throws Exception
    {
        this.client = this.solr.getClient(ExtensionIndexSolrCoreInitializer.NAME);

        // 1: Add local extensions
        addLocalExtensions();

        // 1: Gather all extension from searchable repositories
        addRemoteExtensions();

        // 2: Validate extensions to figure out if they are compatible

        // 3: Analyze extensions to find specific metadata depending on the type

    }

    private Set<String> getSearchableExtensionIds() throws SearchException
    {
        Set<String> extensions = new HashSet<>();

        ExtensionQuery query = new ExtensionQuery();
        query.setLimit(SEARCH_BATCH_SIZE);

        for (int offset = 0; true; offset += SEARCH_BATCH_SIZE) {
            query.setOffset(offset);

            IterableResult<Extension> result = this.repositories.search(query);

            result.forEach(extension -> extensions.add(extension.getId().getId()));

            if (result.getSize() < SEARCH_BATCH_SIZE) {
                break;
            }
        }

        return extensions;
    }

    private void addLocalExtensions()
    {
        this.localExtensions.getLocalExtensions();

        // Add the extension
    }

    private void addRemoteExtensions() throws SearchException
    {
        // Search result are not always fully complete and we want all versions so we just keep the id and go through
        // repositories
        Set<String> extensionIds = getSearchableExtensionIds();

        extensionIds.forEach(this::addExtension);

    }

    private void addExtension(String extensionId)
    {
        // Get all available versions
        IterableResult<Version> versions = this.repositories.resolveVersions(extensionId, 0, -1);

        versions.forEach(version -> addExtension(extensionId, version));
    }

    private void addExtension(String id, Version version) throws SolrServerException, IOException
    {
        ExtensionId extensionId = new ExtensionId(id, version);

        // Not add it if it's a core extension
        if (this.coreExtensions.exists(extensionId)) {
            return;
        }

        // Not add it if it's a local extension
        if (this.localExtensions.exists(extensionId)) {
            return;
        }

        // Check if the extension already exist
        String solrId = toSolrId(extensionId);
        if (this.client.getById(solrId) == null) {
            // Get extension
            Extension extension = this.repositories.resolve(extensionId);

            // Add the extension
            storeExtension(extension);
        }
    }

    private String toSolrId(ExtensionId extensionId)
    {
        return ExtensionIdConverter.toString(extensionId);
    }
}
