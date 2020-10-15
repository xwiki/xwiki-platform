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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionAuthor;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.internal.ExtensionFactory;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.extension.repository.search.ExtensionQuery.COMPARISON;
import org.xwiki.extension.repository.search.ExtensionQuery.Filter;
import org.xwiki.extension.repository.search.ExtensionQuery.SortClause;
import org.xwiki.extension.repository.search.SearchException;
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

    private static final Map<String, SearchFieldMapping> SEARCH_FIELD_MAPPING = new HashMap<>();

    private static class SearchFieldMapping
    {
        private String exactField;

        private String matchField;

        private Type type;

        SearchFieldMapping(String solrField)
        {
            this(solrField, solrField);
        }

        SearchFieldMapping(String exactField, String matchField)
        {
            this.exactField = exactField;
            this.matchField = matchField;
        }

        SearchFieldMapping(Type type)
        {
            this.type = type;
        }

        SearchFieldMapping(String exactField, String matchField, Type type)
        {
            this.exactField = exactField;
            this.matchField = matchField;
            this.type = type;
        }
    }

    static {
        SEARCH_FIELD_MAPPING.put(Extension.FIELD_ID,
            new SearchFieldMapping(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID));

        SEARCH_FIELD_MAPPING.put(Extension.FIELD_AUTHOR,
            new SearchFieldMapping(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_AUTHORS_INDEX));
        SEARCH_FIELD_MAPPING.put(Extension.FIELD_AUTHORS,
            new SearchFieldMapping(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_AUTHORS_INDEX));

        SEARCH_FIELD_MAPPING.put(Extension.FIELD_EXTENSIONFEATURES,
            new SearchFieldMapping(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONFEATURES_INDEX));
        SEARCH_FIELD_MAPPING.put(Extension.FIELD_EXTENSIONFEATURE,
            SEARCH_FIELD_MAPPING.get(Extension.FIELD_EXTENSIONFEATURES));
        SEARCH_FIELD_MAPPING.put(Extension.FIELD_FEATURE, SEARCH_FIELD_MAPPING.get(Extension.FIELD_EXTENSIONFEATURES));
        SEARCH_FIELD_MAPPING.put(Extension.FIELD_FEATURES, SEARCH_FIELD_MAPPING.get(Extension.FIELD_EXTENSIONFEATURES));
    }

    @Inject
    private Solr solr;

    @Inject
    private SolrUtils utils;

    @Inject
    private SolrClient client;

    @Inject
    private ExtensionManager extensionManager;

    @Inject
    private ExtensionFactory factory;

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

        this.utils.setString(Extension.FIELD_AUTHORS, extension.getAuthors(), ExtensionAuthor.class, document);
        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_AUTHORS_INDEX,
            extension.getAuthors().stream().map(ExtensionAuthor::getName).collect(Collectors.toList()), document);

        this.utils.set(Extension.FIELD_EXTENSIONFEATURES, extension.getExtensionFeatures(), document);
        ArrayList<String> indexedFeatures = new ArrayList<>(extension.getExtensionFeatures().size() * 2);
        for (ExtensionId feature : extension.getExtensionFeatures()) {
            indexedFeatures.add(feature.getId());
            indexedFeatures.add(ExtensionIdConverter.toString(feature));
        }
        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONFEATURES_INDEX, indexedFeatures, document);

        // TODO: add dependencies
        // TODO: add managed dependencies

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INDEX_DATE, new Date(), document);

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
    public SolrExtension getSolrExtension(ExtensionId extensionId) throws SolrServerException, IOException
    {
        return toSolrExtension(this.client.getById(toSolrId(extensionId)), extensionId);
    }

    private ExtensionRepository getRepository(SolrDocument document)
    {
        String repositoryId = this.utils.get(Extension.FIELD_REPOSITORY, document);

        return this.extensionManager.getRepository(repositoryId);
    }

    private SolrExtension toSolrExtension(SolrDocument document)
    {
        if (document == null) {
            return null;
        }

        ExtensionId extensionId =
            new ExtensionId(this.utils.<String>get(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID, document),
                this.factory.getVersion(this.utils.<String>get(Extension.FIELD_VERSION, document)));

        return toSolrExtension(document, extensionId);
    }

    private SolrExtension toSolrExtension(SolrDocument document, ExtensionId extensionId)
    {
        if (document == null) {
            return null;
        }

        SolrExtension extension = new SolrExtension(getRepository(document), extensionId);

        extension.setType(this.utils.get(Extension.FIELD_TYPE, document));
        extension.setSummary(this.utils.get(Extension.FIELD_SUMMARY, document));
        extension.setWebsite(this.utils.get(Extension.FIELD_WEBSITE, document));
        extension.setCategory(this.utils.get(Extension.FIELD_CATEGORY, document));
        extension.setAllowedNamespaces(this.utils.getCollection(Extension.FIELD_ALLOWEDNAMESPACES, document));
        extension
            .setExtensionFeatures(this.utils.getCollection(Extension.FIELD_AUTHORS, document, ExtensionAuthor.class));
        extension.setExtensionFeatures(
            this.utils.getCollection(Extension.FIELD_EXTENSIONFEATURES, document, ExtensionId.class));

        // TODO: add authors
        // TODO: add dependencies
        // TODO: add managed dependencies

        return extension;
    }

    /**
     * Search extension based of the provided query.
     * 
     * @param query the query
     * @return the found extensions descriptors, empty list if nothing could be found
     * @throws SearchException error when trying to search provided query
     */
    public IterableResult<Extension> search(ExtensionQuery query) throws SearchException
    {
        SolrQuery solrQuery = new SolrQuery(query.getQuery());

        // Pagination
        solrQuery.setStart(query.getOffset());
        solrQuery.setRows(query.getLimit());

        // Sort
        for (SortClause sortClause : query.getSortClauses()) {
            solrQuery.addSort(sortClause.getField(),
                sortClause.getOrder() == ExtensionQuery.ORDER.ASC ? ORDER.asc : ORDER.desc);
        }

        // Filtering
        for (Filter filter : query.getFilters()) {
            solrQuery.addFilterQuery(serializeFilter(filter));
        }

        // Execute the search
        QueryResponse response;
        try {
            response = this.client.query(solrQuery);
        } catch (Exception e) {
            throw new SearchException("Failed to execute Solr query", e);
        }

        SolrDocumentList documents = response.getResults();

        return new CollectionIterableResult<>((int) documents.getNumFound(), (int) documents.getStart(),
            documents.stream().map(this::toSolrExtension).collect(Collectors.toList()));
    }

    private String serializeFilter(Filter filter)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(toSearchFieldName(filter));
        builder.append(':');
        builder.append(toFilterQueryString(filter));

        return builder.toString();
    }

    private String toSearchFieldName(Filter filter)
    {
        SearchFieldMapping mapping = SEARCH_FIELD_MAPPING.get(filter.getField());

        if (mapping != null) {
            if (filter.getComparison() == COMPARISON.EQUAL) {
                if (mapping.exactField != null) {
                    return mapping.exactField;
                }
            } else {
                if (mapping.matchField != null) {
                    return mapping.matchField;
                }
            }
        }

        return filter.getField();
    }

    private String toFilterQueryString(Filter filter)
    {
        SearchFieldMapping mapping = SEARCH_FIELD_MAPPING.get(filter.getField());

        if (mapping != null && mapping.type != null) {
            return this.utils.toFilterQueryString(filter.getValue(), mapping.type);
        } else {
            return this.utils.toFilterQueryString(filter.getValue());
        }
    }
}
