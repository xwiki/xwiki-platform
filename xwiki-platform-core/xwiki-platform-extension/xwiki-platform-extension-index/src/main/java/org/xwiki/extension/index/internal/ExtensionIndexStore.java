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
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionAuthor;
import org.xwiki.extension.ExtensionComponent;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.ExtensionSupportPlan;
import org.xwiki.extension.ExtensionSupportPlans;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.RemoteExtension;
import org.xwiki.extension.index.IndexedExtensionQuery;
import org.xwiki.extension.index.security.ExtensionSecurityAnalysisResult;
import org.xwiki.extension.index.security.SecurityVulnerabilityDescriptor;
import org.xwiki.extension.internal.ExtensionFactory;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.internal.converter.ExtensionSupportPlanConverter;
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
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.script.service.ScriptService;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;

import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.IS_CORE_EXTENSION;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.IS_REVIEWED_SAFE;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.IS_SAFE_EXPLANATIONS;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_ADVICE;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_COUNT;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_CVSS;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_ID;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_LINK;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_FIX_VERSION;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_MAX_CVSS;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID;

/**
 * An helper to manipulate the store of indexed extensions.
 *
 * @version $Id$
 * @since 12.10
 */
@Component(roles = ExtensionIndexStore.class)
@Singleton
public class ExtensionIndexStore implements Initializable, Disposable
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

    private static final String BOOST = ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID + "^10.0 "
        + Extension.FIELD_NAME + "^9.0 " + ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONFEATURES_INDEX
        + "^8.0 " + Extension.FIELD_SUMMARY + "^7.0 " + ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPONENTS_INDEX
        + "^7.0 " + Extension.FIELD_CATEGORY + "^6.0 " + Extension.FIELD_TYPE + "^5.0 ";

    @Inject
    private Solr solr;

    @Inject
    private SolrUtils utils;

    private SolrClient client;

    @Inject
    private ExtensionManager extensionManager;

    @Inject
    private ExtensionFactory factory;

    @Inject
    private ExtensionIndexSolrUtil extensionIndexSolrUtil;

    @Inject
    private CacheManager cacheManager;

    private Cache<SolrExtension> cache;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private Set<String> modifiedIds = ConcurrentHashMap.newKeySet();

    private int documentsToStore;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.client = this.solr.getClient(ExtensionIndexSolrCoreInitializer.NAME);
        } catch (SolrException e) {
            throw new InitializationException("Failed to get the extension index Solr core", e);
        }

        try {
            this.cache = this.cacheManager.createNewCache(new LRUCacheConfiguration("extension.index", 500));
        } catch (Exception e) {
            throw new InitializationException("Failed to create the group cache", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.cache.dispose();
    }

    /**
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public void commit() throws SolrServerException, IOException
    {
        // Reset counter
        this.documentsToStore = 0;

        this.lock.writeLock().lock();

        try {
            // Commit
            this.client.commit();

            // Invalidate modified cached entries
            this.modifiedIds.forEach(this.cache::remove);
            this.modifiedIds.clear();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void addMofiedId(String id)
    {
        this.lock.readLock().lock();

        try {
            this.modifiedIds.add(id);
        } finally {
            this.lock.readLock().unlock();
        }
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
            + this.utils.toCompleteFilterQueryString(this.extensionIndexSolrUtil.toSolrId(extensionId)));

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

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID,
            this.extensionIndexSolrUtil.toSolrId(extensionId), document);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LAST,
            last, document);

        add(document);
    }

    /**
     * Update variable informations (support plans, ratings, etc.).
     *
     * @param extensionId the identifier of the extension to update
     * @param remoteExtension the remote extension from which to extract variable information
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public void update(ExtensionId extensionId, RemoteExtension remoteExtension) throws SolrServerException, IOException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID,
            this.extensionIndexSolrUtil.toSolrId(extensionId), document);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, RemoteExtension.FIELD_SUPPORT_PLANS,
            ExtensionSupportPlanConverter.toStringList(remoteExtension.getSupportPlans().getSupportPlans()), document);

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
     * Update a given extension with the provided security analysis results.
     *
     * @param extensionId the extension id of the extension to update
     * @param result the security analysis results
     * @throws IOException If there is a low-level I/O error
     * @throws SolrServerException if there is an error on the server
     */
    public void update(ExtensionId extensionId, ExtensionSecurityAnalysisResult result)
        throws SolrServerException, IOException
    {
        SolrInputDocument doc = new SolrInputDocument();

        this.utils.set(AbstractSolrCoreInitializer.SOLR_FIELD_ID, this.extensionIndexSolrUtil.toSolrId(extensionId),
            doc);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, SOLR_FIELD_EXTENSIONID, extensionId.getId(), doc);
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, Extension.FIELD_VERSION,
            extensionId.getVersion().getValue(), doc);

        if (!result.getSecurityVulnerabilities().isEmpty()) {
            this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, SECURITY_MAX_CVSS, result.getMaxCVSS(), doc);
        } else {
            // Remove the CVSS score if the new list of security vulnerabilities becomes empty.
            this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, SECURITY_MAX_CVSS, null, doc);
        }
        Stream<String> cveIds =
            result.getSecurityVulnerabilities().stream().map(SecurityVulnerabilityDescriptor::getId);
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, SECURITY_CVE_ID, cveIds.collect(Collectors.toList()),
            doc);
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, SECURITY_CVE_LINK,
            result.getSecurityVulnerabilities().stream().map(SecurityVulnerabilityDescriptor::getURL)
                .collect(Collectors.toList()),
            doc);
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, SECURITY_CVE_CVSS,
            result.getSecurityVulnerabilities().stream().map(SecurityVulnerabilityDescriptor::getScore)
                .collect(Collectors.toList()),
            doc);
        String fixVersion =
            result.getSecurityVulnerabilities().stream().map(SecurityVulnerabilityDescriptor::getFixVersion)
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).map(Version::getValue).orElse(null);
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, SECURITY_FIX_VERSION, fixVersion, doc);
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, SECURITY_ADVICE, result.getAdvice(), doc);
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, SECURITY_CVE_COUNT,
            result.getSecurityVulnerabilities().size(), doc);
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, IS_CORE_EXTENSION, result.isCoreExtension(), doc);
        List<Boolean> safeMapping = result.getSecurityVulnerabilities().stream()
            .map(SecurityVulnerabilityDescriptor::isSafe).collect(Collectors.toList());
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, IS_REVIEWED_SAFE, safeMapping, doc);
        List<String> reviewExplanations = result.getSecurityVulnerabilities().stream()
            .map(SecurityVulnerabilityDescriptor::getReviews).collect(Collectors.toList());
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, IS_SAFE_EXPLANATIONS, reviewExplanations, doc);

        add(doc);
        commit();
    }

    /**
     * Update variable informations (support plans, ratings, etc.) by copying it from another version.
     *
     * @param extensionId the identifier of the extension to update
     * @param copyVersion the version of the extension to copy
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public boolean update(ExtensionId extensionId, Version copyVersion) throws SolrServerException, IOException
    {
        // Get the version to copy
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.addFilterQuery(
            ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID + ':' + this.utils.toCompleteFilterQueryString(
                this.extensionIndexSolrUtil.toSolrId(new ExtensionId(extensionId.getId(), copyVersion))));
        solrQuery.setFields(RemoteExtension.FIELD_SUPPORT_PLANS, RemoteExtension.FIELD_RECOMMENDED,
            RatingExtension.FIELD_TOTAL_VOTES, RatingExtension.FIELD_AVERAGE_VOTE);
        solrQuery.setRows(1);
        QueryResponse response = search(solrQuery);
        SolrDocumentList documents = response.getResults();
        if (documents.isEmpty()) {
            return false;
        }

        SolrDocument copyDocument = documents.get(0);

        // Update

        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID,
            this.extensionIndexSolrUtil.toSolrId(extensionId), document);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, RemoteExtension.FIELD_SUPPORT_PLANS,
            copyDocument.get(RemoteExtension.FIELD_SUPPORT_PLANS), document);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, RemoteExtension.FIELD_RECOMMENDED,
            copyDocument.get(RemoteExtension.FIELD_RECOMMENDED), document);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, RemoteExtension.FIELD_RECOMMENDED,
            copyDocument.get(RemoteExtension.FIELD_RECOMMENDED), document);

        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, RatingExtension.FIELD_TOTAL_VOTES,
            copyDocument.get(RatingExtension.FIELD_TOTAL_VOTES), document);
        this.utils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, RatingExtension.FIELD_AVERAGE_VOTE,
            copyDocument.get(RatingExtension.FIELD_AVERAGE_VOTE), document);

        add(document);

        return true;
    }

    /**
     * @param extensionId the id of the extension to update
     * @param namespace the namespace for which to update the extension
     * @param installed true if the extension is installed on the passed namespace, false if uninstalled
     * @throws IOException if there is a communication error with the server
     * @throws SolrServerException if there is an error on the server
     */
    public void updateInstalled(ExtensionId extensionId, String namespace, boolean installed)
        throws SolrServerException, IOException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID,
            this.extensionIndexSolrUtil.toSolrId(extensionId), document);

        // Update installed
        this.utils.setAtomic(
            installed ? SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD_DISTINCT : SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE,
            InstalledExtension.FIELD_INSTALLED_NAMESPACES, this.extensionIndexSolrUtil.toStoredNamespace(namespace),
            document);

        // Update compatible
        if (installed) {
            updateCompatible(document, namespace, false, null);
        } else {
            Version compatibleVersion = getCompatibleVersion(extensionId.getId(), namespace);

            if (compatibleVersion == null) {
                updateCompatible(document, namespace, true, null);
            }
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

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID,
            this.extensionIndexSolrUtil.toSolrId(extensionId), document);

        updateCompatible(document, namespace, compatible, incompatible);

        add(document);
    }

    private void updateCompatible(SolrInputDocument document, String namespace, Boolean compatible,
        Boolean incompatible)
    {
        if (compatible != null) {
            this.utils.setAtomic(
                compatible.booleanValue() ? SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD_DISTINCT
                    : SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE,
                ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPATIBLE_NAMESPACES,
                this.extensionIndexSolrUtil.toStoredNamespace(namespace), document);
        }

        if (incompatible != null) {
            this.utils.setAtomic(
                incompatible.booleanValue() ? SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD_DISTINCT
                    : SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE,
                ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INCOMPATIBLE_NAMESPACES,
                this.extensionIndexSolrUtil.toStoredNamespace(namespace), document);
        }
    }

    public Boolean isCompatible(ExtensionId extensionId, String namespace) throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID + ':'
            + this.utils.toCompleteFilterQueryString(this.extensionIndexSolrUtil.toSolrId(extensionId)));

        solrQuery.setFields(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPATIBLE_NAMESPACES,
            ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INCOMPATIBLE_NAMESPACES);

        solrQuery.setRows(1);

        QueryResponse response = search(solrQuery);

        SolrDocumentList documents = response.getResults();
        if (!documents.isEmpty()) {
            SolrDocument document = documents.get(0);

            String solrNamespace = this.extensionIndexSolrUtil.toStoredNamespace(namespace);

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

    /**
     * @param extension the extension to add to the index
     * @param last true if it's the last version of this extension id
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    public void add(Extension extension, boolean last) throws SolrServerException, IOException
    {
        SolrInputDocument document = new SolrInputDocument();

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID,
            this.extensionIndexSolrUtil.toSolrId(extension.getId()), document);

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

        this.utils.setString(Extension.FIELD_COMPONENTS, extension.getComponents(), ExtensionComponent.class, document);
        for (ExtensionComponent component : extension.getComponents()) {
            document.addField(ExtensionIndexSolrCoreInitializer.toComponentFieldName(component.getRoleType()),
                component.getRoleHint());
        }

        this.utils.setString(Extension.FIELD_EXTENSIONFEATURES, extension.getExtensionFeatures(), ExtensionId.class,
            document);
        for (ExtensionId feature : extension.getExtensionFeatures()) {
            document.addField(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONFEATURES_INDEX, feature.getId());
            document.addField(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONFEATURES_INDEX,
                ExtensionIdConverter.toString(feature));
        }

        // TODO: add dependencies
        // TODO: add managed dependencies

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INDEX_DATE, new Date(), document);

        this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LAST, last, document);

        // Remote
        if (extension instanceof RemoteExtension remoteExtension) {
            addSupportPlans(remoteExtension.getSupportPlans(), document);
        }

        // Set installed state
        this.extensionIndexSolrUtil.updateInstalledState(extension.getId(), document);

        add(document);
    }

    private void addSupportPlans(ExtensionSupportPlans supportPlans, SolrInputDocument document)
    {
        this.utils.setString(RemoteExtension.FIELD_SUPPORT_PLANS, supportPlans.getSupportPlans(),
            ExtensionSupportPlan.class, document);
    }

    private boolean add(SolrInputDocument document) throws SolrServerException, IOException
    {
        // Add the document to the Solr queue
        this.client.add(document);

        // Remember the modified entry
        addMofiedId((String) document.getFieldValue(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_ID));

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
        String id = this.extensionIndexSolrUtil.toSolrId(extensionId);

        SolrExtension solrExtension = this.cache.get(id);

        if (solrExtension == null) {
            solrExtension = toSolrExtension(this.client.getById(id), extensionId);

            this.cache.set(id, solrExtension);
        }

        return solrExtension;
    }

    /**
     * @param extensionId the extension id
     * @return the list of CVEs attached to a given extension id
     * @throws IOException If there is a low-level I/O error
     * @throws SolrServerException if there is an error on the server
     */
    public List<String> getCVEIDs(ExtensionId extensionId) throws SolrServerException, IOException
    {
        SolrDocument byId = this.client.getById(this.extensionIndexSolrUtil.toSolrId(extensionId));
        if (byId == null) {
            return List.of();
        }
        List<String> securityCveID = (List<String>) byId.get(SECURITY_CVE_ID);
        if (securityCveID == null) {
            return List.of();
        }
        return securityCveID;
    }

    private ExtensionRepository getRepository(SolrDocument document)
    {
        String repositoryId = this.utils.get(Extension.FIELD_REPOSITORY, document);

        return this.extensionManager.getRepository(repositoryId);
    }

    public ExtensionId getExtensionId(SolrDocument document)
    {
        return new ExtensionId(
            this.utils.<String>get(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID, document),
            this.factory.getVersion(this.utils.<String>get(Extension.FIELD_VERSION, document)));
    }

    public SolrExtension toSolrExtension(SolrDocument document)
    {
        if (document == null) {
            return null;
        }

        ExtensionId extensionId = getExtensionId(document);

        return toSolrExtension(document, extensionId);
    }

    private SolrExtension toSolrExtension(SolrDocument document, ExtensionId extensionId)
    {
        if (document == null) {
            return null;
        }

        SolrExtension extension = new SolrExtension(getRepository(document), extensionId);

        toSolrExtension(document, extension);

        return extension;
    }

    public void toSolrExtension(SolrDocument document, SolrExtension extension)
    {
        extension.setType(this.utils.get(Extension.FIELD_TYPE, document));

        extension.setName(this.utils.get(Extension.FIELD_NAME, document));
        extension.setSummary(this.utils.get(Extension.FIELD_SUMMARY, document));
        extension.setWebsite(this.utils.get(Extension.FIELD_WEBSITE, document));
        extension.setCategory(this.utils.get(Extension.FIELD_CATEGORY, document));
        extension.setAllowedNamespaces(this.utils.getCollection(Extension.FIELD_ALLOWEDNAMESPACES, document));

        extension.setTotalVotes(this.utils.get(RatingExtension.FIELD_TOTAL_VOTES, document, 0));
        extension.setAverageVote(this.utils.get(RatingExtension.FIELD_AVERAGE_VOTE, document, 0f));

        extension.setAuthors(this.utils.getCollection(Extension.FIELD_AUTHORS, document, ExtensionAuthor.class));
        extension.setSupportPlans(
            this.utils.getCollection(RemoteExtension.FIELD_SUPPORT_PLANS, document, ExtensionSupportPlan.class));
        extension
            .setComponents(this.utils.getCollection(Extension.FIELD_COMPONENTS, document, ExtensionComponent.class));
        extension.setExtensionFeatures(
            this.utils.getCollection(Extension.FIELD_EXTENSIONFEATURES, document, ExtensionId.class));
        extension.setSupportPlans(
            this.utils.getCollection(RemoteExtension.FIELD_SUPPORT_PLANS, document, ExtensionSupportPlan.class));
        extension.setRecommended(this.utils.get(RemoteExtension.FIELD_RECOMMENDED, document,
            !extension.getSupportPlans().getSupportPlans().isEmpty()));

        extension.setLast(this.utils.get(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LAST, document, false));

        extension.setCompatibleNamespaces(
            getNamespaces(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPATIBLE_NAMESPACES, document));
        extension.setIncompatibleNamespaces(
            getNamespaces(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INCOMPATIBLE_NAMESPACES, document));

        // TODO: add dependencies
        // TODO: add managed dependencies
    }

    private Collection<String> getNamespaces(String fieldName, SolrDocument document)
    {
        Collection<String> storedNamespaces = this.utils.getCollection(fieldName, document);
        return this.extensionIndexSolrUtil.fromStoredNamespaces(storedNamespaces);
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

        createSolrQuery(query, solrQuery);

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

    public void createSolrQuery(ExtensionQuery query, SolrQuery solrQuery)
    {
        if (StringUtils.isNotBlank(query.getQuery())) {
            solrQuery.setQuery(query.getQuery());

            // Use the Extended DisMax Query Parser to set a boost configuration (which is not a feature supported by
            // the Standard Query Parser)
            solrQuery.set("defType", "edismax");
            solrQuery.set("qf", BOOST);

            // Add aliases for well known components roles
            // TODO: make it extensible
            solrQuery.set("f.component_macro.qf",
                ExtensionIndexSolrCoreInitializer.toComponentFieldName(Macro.class.getName()));
            solrQuery.set("f.component_scriptservice.qf",
                ExtensionIndexSolrCoreInitializer.toComponentFieldName(ScriptService.class.getName()));
            solrQuery.set("f.component_parser.qf",
                ExtensionIndexSolrCoreInitializer.toComponentFieldName(Parser.class.getName()));
            solrQuery.set("f.component_renderer.qf",
                ExtensionIndexSolrCoreInitializer.toComponentFieldName(PrintRendererFactory.class.getName()));
            solrQuery.set("f.component_blockrenderer.qf",
                ExtensionIndexSolrCoreInitializer.toComponentFieldName(BlockRenderer.class.getName()));
            solrQuery.set("f.component_inputFilter.qf",
                ExtensionIndexSolrCoreInitializer.toComponentFieldName(InputFilterStreamFactory.class.getName()));
            solrQuery.set("f.component_outputFilter.qf",
                ExtensionIndexSolrCoreInitializer.toComponentFieldName(OutputFilterStreamFactory.class.getName()));
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

        // Indexed
        if (query instanceof IndexedExtensionQuery) {
            IndexedExtensionQuery indexedQuery = (IndexedExtensionQuery) query;

            // Compatible
            if (indexedQuery.getCompatible() != null) {
                StringBuilder builder = new StringBuilder();

                if (!indexedQuery.getCompatible()) {
                    builder.append('-');
                }
                builder.append(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPATIBLE_NAMESPACES);
                builder.append(':');

                builder.append('(');
                builder.append(StringUtils.join(indexedQuery.getCompatibleNamespaces().stream()
                    .map(n -> this.utils.toCompleteFilterQueryString(this.extensionIndexSolrUtil.toStoredNamespace(n)))
                    .iterator(), " OR "));
                builder.append(')');

                solrQuery.addFilterQuery(builder.toString());
            }

            // Installed
            if (indexedQuery.getInstalled() != null) {
                StringBuilder builder = new StringBuilder();

                if (!indexedQuery.getInstalled()) {
                    builder.append('-');
                }
                builder.append(InstalledExtension.FIELD_INSTALLED_NAMESPACES);
                builder.append(':');

                builder.append('(');
                builder.append(StringUtils.join(indexedQuery.getInstalledNamespaces().stream()
                    .map(n -> this.utils.toCompleteFilterQueryString(this.extensionIndexSolrUtil.toStoredNamespace(n)))
                    .iterator(), " OR "));
                builder.append(')');

                solrQuery.addFilterQuery(builder.toString());
            }

            // If no specific compatible or installed criteria only search for latest versions
            if (indexedQuery.getCompatible() == null && indexedQuery.getInstalled() == null) {
                solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LAST + ':' + true);
            }
        } else {
            // Only search for latest versions
            solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LAST + ':' + true);
        }
    }

    private String serializeFilter(Filter filter)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(toSearchFieldName(filter));
        builder.append(':');
        if (filter.getComparison() == COMPARISON.MATCH) {
            builder.append('*');
            builder.append(toFilterQueryString(filter));
            builder.append('*');
        } else {
            builder.append(toCompleteFilterQueryString(filter));
        }

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

    private String toCompleteFilterQueryString(Filter filter)
    {
        SearchFieldMapping mapping = SEARCH_FIELD_MAPPING.get(filter.getField());

        if (mapping != null && mapping.type != null) {
            return this.utils.toCompleteFilterQueryString(filter.getValue(), mapping.type);
        } else {
            return this.utils.toCompleteFilterQueryString(filter.getValue());
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
            extensionId.add(this.extensionIndexSolrUtil.fromSolrId(this.utils.getId(document)));
        }

        return extensionId;
    }

    /**
     * Performs a query to the Solr server.
     *
     * @param params an object holding all key/value parameters to send along the request
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} containing the response from the server
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
            + this.utils.toCompleteFilterQueryString(extensionId));

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
            + this.utils.toCompleteFilterQueryString(extensionId));

        if (withNamespace) {
            solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPATIBLE_NAMESPACES + ":"
                + this.utils.toCompleteFilterQueryString(this.extensionIndexSolrUtil.toStoredNamespace(namespace)));
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
