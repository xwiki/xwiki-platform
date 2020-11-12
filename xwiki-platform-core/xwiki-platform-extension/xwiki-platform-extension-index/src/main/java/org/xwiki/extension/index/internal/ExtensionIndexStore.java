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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionAuthor;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.RemoteExtension;
import org.xwiki.extension.index.IndexedExtensionQuery;
import org.xwiki.extension.internal.ExtensionFactory;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.rating.RatingExtension;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.extension.repository.search.ExtensionQuery.COMPARISON;
import org.xwiki.extension.repository.search.ExtensionQuery.Filter;
import org.xwiki.extension.repository.search.ExtensionQuery.SortClause;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.version.Version;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;

/**
 * An helper to manipulate the store of indexed extensions.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@Component(roles = ExtensionIndexStore.class)
@Singleton
public class ExtensionIndexStore implements Initializable
{
    private static final String ROOT_NAMESPACE = "{root}";

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

    /*
     * private static final String BOOST = ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID + "^10.0 " +
     * Extension.FIELD_NAME + "^9.0 " + ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONFEATURES_INDEX + "^8.0 " +
     * Extension.FIELD_SUMMARY + "^7.0 " + Extension.FIELD_CATEGORY + "^6.0 " + Extension.FIELD_TYPE + "^5.0 ";
     */

    private static final String BOOST = Extension.FIELD_NAME + "^10.0 ";

    @Inject
    private Solr solr;

    @Inject
    private SolrUtils utils;

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
     * @param solrId the identifier of the Solr document holding the extension
     * @return the extension id
     */
    public ExtensionId fromSolrId(String solrId)
    {
        return ExtensionIdConverter.toExtensionId(solrId, null, this.factory);
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
     * @param local true if it's the searched extension is a local extension
     * @return true if a document corresponding to the passed extension can be found in the index
     * @throws SolrServerException
     * @throws IOException
     */
    public boolean exists(ExtensionId extensionId, Boolean local) throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID + ':'
            + this.utils.toFilterQueryString(toSolrId(extensionId)));

        if (local != null) {
            solrQuery.addFilterQuery(Extension.FIELD_REPOSITORY + ":local");
        }

        // We don't want to actually get the document, we just want to know if one exist
        solrQuery.setRows(0);

        return this.client.query(solrQuery).getResults().getNumFound() > 0;
    }

    /**
     * @param extensionId the id of the extension to update
     * @param last true if it's the last version of this extension id
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public void updateLast(ExtensionId extensionId, boolean last) throws SolrServerException, IOException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID, toSolrId(extensionId), document);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LAST,
            last, document);

        add(document);
    }

    /**
     * Update variable informations (recommended tag, ratings, etc.).
     * 
     * @param remoteExtension the remote extension from which to extract variable information
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public void update(RemoteExtension remoteExtension) throws SolrServerException, IOException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID, toSolrId(remoteExtension.getId()), document);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, RemoteExtension.FIELD_RECOMMENDED,
            remoteExtension.isRecommended(), document);

        if (remoteExtension instanceof RatingExtension) {
            RatingExtension ratingExtension = (RatingExtension) remoteExtension;

            this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, RatingExtension.FIELD_TOTAL_VOTES,
                ratingExtension.getRating().getTotalVotes(), document);
            this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, RatingExtension.FIELD_AVERAGE_VOTE,
                ratingExtension.getRating().getAverageVote(), document);
        }

        add(document);
    }

    /**
     * @param extensionId the id of the extension to update
     * @param namespace the namespace for which to update the extension
     * @param compatible true if the extension is compatible with the passed namespace
     * @param incompatible true if the extension is incompatible with the passed namespace
     * @throws IOException if there is a communication error with the server
     * @throws SolrServerException if there is an error on the server
     */
    public void updateCompatible(ExtensionId extensionId, String namespace, Boolean compatible, Boolean incompatible)
        throws SolrServerException, IOException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID, toSolrId(extensionId), document);

        if (compatible != null) {
            this.utils.setAtomic(
                compatible.booleanValue() ? SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD_DISTINCT
                    : SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE,
                ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPATIBLE_NAMESPACES, toStoredNamespace(namespace),
                document);
        }

        if (incompatible != null) {
            this.utils.setAtomic(
                incompatible.booleanValue() ? SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD_DISTINCT
                    : SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE,
                ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INCOMPATIBLE_NAMESPACES, toStoredNamespace(namespace),
                document);
        }

        add(document);
    }

    public Boolean isCompatible(ExtensionId extensionId, String namespace) throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID + ':'
            + this.utils.toFilterQueryString(toSolrId(extensionId)));

        solrQuery.setFields(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPATIBLE_NAMESPACES,
            ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INCOMPATIBLE_NAMESPACES);

        solrQuery.setRows(1);

        QueryResponse response = search(solrQuery);

        SolrDocumentList documents = response.getResults();
        if (!documents.isEmpty()) {
            SolrDocument document = documents.get(0);

            String solrNamespace = toStoredNamespace(namespace);

            List<String> compatibleNamespaces = this.utils
                .<List<String>>get(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPATIBLE_NAMESPACES, document);

            if (compatibleNamespaces != null && compatibleNamespaces.contains(solrNamespace)) {
                return true;
            }

            List<String> incompatibleNamespaces = this.utils
                .<List<String>>get(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INCOMPATIBLE_NAMESPACES, document);

            if (incompatibleNamespaces != null && incompatibleNamespaces.contains(solrNamespace)) {
                return false;
            }
        }

        return null;
    }

    private String toStoredNamespace(Namespace namespace)
    {
        return namespace != null ? toStoredNamespace(namespace.toString()) : ROOT_NAMESPACE;
    }

    private String toStoredNamespace(String namespace)
    {
        if (namespace == null) {
            return ROOT_NAMESPACE;
        }

        return namespace;
    }

    /**
     * @param remoteExtension the {@link RemoteExtension} version of the extension
     * @param extension the extension to add to the index
     * @param last true if it's the last version of this extension id
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public void add(Extension extension, boolean last) throws SolrServerException, IOException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID, toSolrId(extension.getId()), document);

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID, extension.getId().getId(), document);
        this.utils.set(Extension.FIELD_VERSION, extension.getId().getVersion().getValue(), document);

        this.utils.set(Extension.FIELD_TYPE, extension.getType(), document);
        this.utils.set(Extension.FIELD_REPOSITORY, extension.getRepository().getDescriptor().getId(), document);

        this.utils.set(Extension.FIELD_NAME, extension.getName(), document);
        this.utils.set(Extension.FIELD_SUMMARY, extension.getSummary(), document);
        this.utils.set(Extension.FIELD_WEBSITE, extension.getWebSite(), document);

        if (extension.getAllowedNamespaces() != null) {
            this.utils.set(Extension.FIELD_ALLOWEDNAMESPACES, extension.getAllowedNamespaces(), document);
        }

        this.utils.set(Extension.FIELD_CATEGORY, extension.getCategory(), document);

        this.utils.setString(Extension.FIELD_AUTHORS, extension.getAuthors(), ExtensionAuthor.class, document);
        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_AUTHORS_INDEX,
            extension.getAuthors().stream().map(ExtensionAuthor::getName).collect(Collectors.toList()), document);

        this.utils.setString(Extension.FIELD_EXTENSIONFEATURES, extension.getExtensionFeatures(), ExtensionId.class,
            document);
        ArrayList<String> indexedFeatures = new ArrayList<>(extension.getExtensionFeatures().size() * 2);
        for (ExtensionId feature : extension.getExtensionFeatures()) {
            indexedFeatures.add(feature.getId());
            indexedFeatures.add(ExtensionIdConverter.toString(feature));
        }
        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONFEATURES_INDEX, indexedFeatures, document);

        // TODO: add dependencies
        // TODO: add managed dependencies

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INDEX_DATE, new Date(), document);

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LAST, last, document);

        add(document);
    }

    private boolean add(SolrInputDocument document) throws SolrServerException, IOException
    {
        // Add the document to the Solr queue
        this.client.add(document);

        // Check if it should be auto committed
        ++this.documentsToStore;
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

        extension.setName(this.utils.get(Extension.FIELD_NAME, document));
        extension.setSummary(this.utils.get(Extension.FIELD_SUMMARY, document));
        extension.setWebsite(this.utils.get(Extension.FIELD_WEBSITE, document));
        extension.setCategory(this.utils.get(Extension.FIELD_CATEGORY, document));
        extension.setAllowedNamespaces(this.utils.getCollection(Extension.FIELD_ALLOWEDNAMESPACES, document));

        extension.setRecommended(this.utils.get(RemoteExtension.FIELD_RECOMMENDED, document, false));

        extension.setTotalVotes(this.utils.get(RatingExtension.FIELD_TOTAL_VOTES, document, 0));
        extension.setAverageVote(this.utils.get(RatingExtension.FIELD_AVERAGE_VOTE, document, 0f));

        extension.setAuthors(this.utils.getCollection(Extension.FIELD_AUTHORS, document, ExtensionAuthor.class));
        extension.setExtensionFeatures(
            this.utils.getCollection(Extension.FIELD_EXTENSIONFEATURES, document, ExtensionId.class));

        extension.setLast(this.utils.get(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LAST, document, false));

        extension.setCompatibleNamespaces(this.utils.<Namespace>getCollection(
            ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPATIBLE_NAMESPACES, document, Namespace.class));
        extension.setIncompatibleNamespaces(this.utils.<Namespace>getCollection(
            ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INCOMPATIBLE_NAMESPACES, document, Namespace.class));

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
        SolrQuery solrQuery = new SolrQuery();

        if (StringUtils.isNotBlank(query.getQuery())) {
            solrQuery.setQuery(query.getQuery());

            // Use the Extended DisMax Query Parser to set a boost configuration (which is not a feature supported by
            // the Standard Query Parser)
            solrQuery.set("defType", "edismax");
            solrQuery.set("bf", BOOST);

            solrQuery.setFields(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID, Extension.FIELD_VERSION,
                Extension.FIELD_NAME);
        }

        // Pagination
        if (query.getOffset() > 0) {
            solrQuery.setStart(query.getOffset());
        }
        if (query.getLimit() > 0) {
            solrQuery.setRows(query.getLimit());
        } else {
            solrQuery.setRows(Integer.MAX_VALUE);
        }

        // Sort
        for (SortClause sortClause : query.getSortClauses()) {
            SearchFieldMapping fieldMapping = SEARCH_FIELD_MAPPING.get(sortClause.getField());
            String fieldName = fieldMapping != null && fieldMapping.exactField != null ? fieldMapping.exactField
                : sortClause.getField();
            solrQuery.addSort(fieldName, sortClause.getOrder() == ExtensionQuery.ORDER.ASC ? ORDER.asc : ORDER.desc);
        }
        // Set default ordering
        if (StringUtils.isEmpty(query.getQuery())) {
            // Sort by rating by default when search query is empty
            solrQuery.addSort(RatingExtension.FIELD_AVERAGE_VOTE, ORDER.desc);
            solrQuery.addSort(RatingExtension.FIELD_TOTAL_VOTES, ORDER.desc);
        } else {
            // Sort by score by default when search query is not empty
            solrQuery.addSort("score", ORDER.desc);
        }

        // Filtering
        for (Filter filter : query.getFilters()) {
            solrQuery.addFilterQuery(serializeFilter(filter));
        }

        // Compatible
        if (query instanceof IndexedExtensionQuery) {
            IndexedExtensionQuery indexedQuery = (IndexedExtensionQuery) query;

            if (indexedQuery.getCompatible() != null) {
                StringBuilder builder = new StringBuilder();

                if (!indexedQuery.getCompatible()) {
                    builder.append('-');
                }
                builder.append(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPATIBLE_NAMESPACES);
                builder.append(':');

                builder.append('(');
                builder.append(StringUtils.join(indexedQuery.getCompatibleNamespaces().stream()
                    .map(n -> this.utils.toFilterQueryString(toStoredNamespace(n))).iterator(), " OR "));
                builder.append(')');

                solrQuery.addFilterQuery(builder.toString());
            } else {
                // Only search for latest versions
                solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LAST + ':' + true);
            }
        } else {
            // Only search for latest versions
            solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LAST + ':' + true);
        }

        // Execute the search
        QueryResponse response;
        try {
            response = search(solrQuery);
        } catch (Exception e) {
            throw new SearchException("Failed to search extension for query [" + query + "]", e);
        }

        SolrDocumentList documents = response.getResults();

        List<Extension> extensions = documents.stream().map(this::toSolrExtension).collect(Collectors.toList());

        return new CollectionIterableResult<>((int) documents.getNumFound(), (int) documents.getStart(), extensions);
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

    /**
     * @param solrQuery an object holding all key/value parameters to send along the request
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} containing the response from the server
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public Set<ExtensionId> searchExtensionIds(SolrQuery solrQuery) throws SolrServerException, IOException
    {
        if (solrQuery.getRows() == null) {
            solrQuery.setRows(Integer.MAX_VALUE);
        }
        solrQuery.setFields(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID);

        QueryResponse response = search(solrQuery);

        SolrDocumentList documents = response.getResults();

        Set<ExtensionId> extensionId = new LinkedHashSet<>(documents.size());
        for (SolrDocument document : documents) {
            extensionId.add(fromSolrId(this.utils.getId(document)));
        }

        return extensionId;
    }

    /**
     * Performs a query to the Solr server.
     *
     * @param params an object holding all key/value parameters to send along the request
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} containing the response from the server
     * @throws SearchException when failing to execute the Solr query
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public QueryResponse search(SolrParams params) throws SolrServerException, IOException
    {
        return this.client.query(params);
    }

    /**
     * @param extensionId the identifier of the extension
     * @return the versions available for the provided id, null if no extension can be found with this id
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public Collection<Version> getIndexedVersions(String extensionId) throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.setFields(Extension.FIELD_VERSION);

        solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID + ':'
            + this.utils.toFilterQueryString(extensionId));

        QueryResponse response = search(solrQuery);

        SolrDocumentList documents = response.getResults();
        if (documents.isEmpty()) {
            return null;
        }

        return documents.stream().map(d -> this.utils.<Version>get(Extension.FIELD_VERSION, d, Version.class))
            .collect(Collectors.toList());
    }

    /**
     * @param extensionId the identifier of the extension
     * @return the {@link Version} for which the extension is compatible (in any namespace)
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public Version getCompatibleVersion(String extensionId) throws SolrServerException, IOException
    {
        return getCompatibleVersion(extensionId, null, false);
    }

    /**
     * @param extensionId the identifier of the extension
     * @param namespace the namespace on which to check compatibility
     * @return the {@link Version} for which the extension is compatible (in any namespace)
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public Version getCompatibleVersion(String extensionId, String namespace) throws SolrServerException, IOException
    {
        return getCompatibleVersion(extensionId, namespace, true);
    }

    /**
     * @param extensionId the identifier of the extension
     * @param namespace the namespace on which to check compatibility
     * @param withNamespace true if the namespace should be included in the search
     * @return the {@link Version} for which the extension is compatible (in any namespace)
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    private Version getCompatibleVersion(String extensionId, String namespace, boolean withNamespace)
        throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.setFields(Extension.FIELD_VERSION);

        solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID + ':'
            + this.utils.toFilterQueryString(extensionId));

        if (withNamespace) {
            solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPATIBLE_NAMESPACES + ":"
                + this.utils.toFilterQueryString(toStoredNamespace(namespace)));
        } else {
            solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPATIBLE_NAMESPACES + ":[* TO *]");
        }

        solrQuery.setRows(1);

        QueryResponse response = search(solrQuery);

        SolrDocumentList documents = response.getResults();
        if (documents.isEmpty()) {
            return null;
        }

        return this.utils.get(Extension.FIELD_VERSION, documents.get(0), Version.class);
    }
}
