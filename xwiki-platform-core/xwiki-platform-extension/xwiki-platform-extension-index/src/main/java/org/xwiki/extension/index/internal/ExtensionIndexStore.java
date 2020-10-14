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
package org.xwiki.extension.index.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionAuthor;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;

/**
 * An helper to manipulate the store of indexed extensions.
 * 
 * @version $Id$
 * @since 12.9RC1
 */
@Component(roles = ExtensionIndexStore.class)
@Singleton
public class ExtensionIndexStore implements Initializable
{
    private static final int COMMIT_BATCH_SIZE = 100;

    @Inject
    private Solr solr;

    @Inject
    private SolrUtils utils;

    @Inject
    private SolrClient client;

    private int documentsToStore;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.client = this.solr.getClient(ExtensionIndexSolrCoreInitializer.NAME);
        } catch (SolrException e) {
            throw new InitializationException("Failed to get the extension index Solr core", e);
        }
    }

    /**
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public void commit() throws SolrServerException, IOException
    {
        // Reset counter
        this.documentsToStore = 0;

        // Commit
        this.client.commit();
    }

    /**
     * @param extensionId the extension id
     * @return the identifier of the Solr document holding the extension
     */
    public String toSolrId(ExtensionId extensionId)
    {
        return ExtensionIdConverter.toString(extensionId);
    }

    /**
     * @param extensionId the extension id and version
     * @return true if a document corresponding to the passed extension can be found in the index
     * @throws SolrServerException
     * @throws IOException
     */
    public boolean exists(ExtensionId extensionId) throws SolrServerException, IOException
    {
        return exists(extensionId, null);
    }

    /**
     * @param extensionId the extension id and version
     * @param local true/false to explicitly search of a local extension, null otherwise
     * @return true if a document corresponding to the passed extension can be found in the index
     * @throws SolrServerException
     * @throws IOException
     */
    public boolean exists(ExtensionId extensionId, Boolean local) throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.addFilterQuery("id:" + this.utils.toFilterQueryString(extensionId.getId()));
        solrQuery.addFilterQuery("version:" + this.utils.toFilterQueryString(extensionId.getVersion().getValue()));

        if (local != null) {
            solrQuery.addFilterQuery("local:" + local.booleanValue());
        }

        // We don't want to actually get the document, we just want to know if one exist
        solrQuery.setRows(0);

        return client.query(solrQuery).getResults().getNumFound() > 0;
    }

    public void addLocal(ExtensionId extensionId) throws SolrServerException, IOException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID, toSolrId(extensionId), document);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LOCAL,
            false, document);

        add(document);
    }
    
    public void removeLocal(ExtensionId extensionId) throws SolrServerException, IOException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID, toSolrId(extensionId), document);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LOCAL,
            false, document);

        add(document);
    }

    public void removeInstalled(ExtensionId extensionId) throws SolrServerException, IOException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID, toSolrId(extensionId), document);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET,
            ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INSTALLED, false, document);
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET,
            ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INSTALLED_NAMESPACES, Collections.emptyList(), document);

        add(document);
    }

    /**
     * @param extension the installed extension
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public void updateInstalled(InstalledExtension extension) throws SolrServerException, IOException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID, toSolrId(extension.getId()), document);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LOCAL,
            true, document);
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET,
            ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INSTALLED, true, document);
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET,
            ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INSTALLED_NAMESPACES, extension.getNamespaces(), document);

        add(document);
    }

    /**
     * @param extension the extension to add to the index
     * @param force true if the extension should always be saved even if it already exist
     * @return true of the extension was saved
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public boolean add(Extension extension, boolean force) throws SolrServerException, IOException
    {
        if (!force && exists(extension.getId())) {
            return false;
        }

        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID, toSolrId(extension.getId()), document);

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID, extension.getId().getId(), document);
        this.utils.set(Extension.FIELD_VERSION, extension.getId().getVersion().getValue(), document);

        this.utils.set(Extension.FIELD_TYPE, extension.getType(), document);
        this.utils.set(Extension.FIELD_REPOSITORY, extension.getRepository().getDescriptor().getId(), document);
        this.utils.set(Extension.FIELD_ALLOWEDNAMESPACES, extension.getAllowedNamespaces(), document);
        this.utils.set(Extension.FIELD_SUMMARY, extension.getSummary(), document);
        this.utils.set(Extension.FIELD_WEBSITE, extension.getWebSite(), document);
        this.utils.set(Extension.FIELD_AUTHORS,
            extension.getAuthors().stream().map(ExtensionAuthor::getName).collect(Collectors.toList()), document);
        this.utils.set(Extension.FIELD_CATEGORY, extension.getCategory(), document);

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INDEX_DATE, new Date(), document);

        if (extension instanceof LocalExtension) {
            this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LOCAL, true, document);

            if (extension instanceof InstalledExtension) {
                this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INSTALLED, true, document);
                this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INSTALLED_NAMESPACES,
                    ((InstalledExtension) extension).getNamespaces(), document);
            }
        }

        add(document);

        return true;
    }

    private boolean add(SolrInputDocument document) throws SolrServerException, IOException
    {
        // Add the document to the Solr queue
        this.client.add(document);

        // Check if it should be auto committed
        this.documentsToStore++;
        if (this.documentsToStore == COMMIT_BATCH_SIZE) {
            commit();

            // The document has been committed
            return true;
        }

        // The document has not been committed
        return false;
    }

    /**
     * @param extensionId the id of the extension
     * @return the found extension or null of none could be found
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public Extension getExtension(ExtensionId extensionId) throws SolrServerException, IOException
    {
        SolrDocument document = this.client.getById(toSolrId(extensionId));

        if (document == null) {
            return null;
        }

        SolrExtension extension = new SolrExtension(extensionId);

        // Get the corresponding repository to procy the real extension

        return extension;
    }
}
