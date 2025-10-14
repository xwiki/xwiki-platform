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

package org.xwiki.repository.internal.resources;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.solr.common.SolrDocument;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.DefaultExtensionScmConnection;
import org.xwiki.extension.Extension;
import org.xwiki.extension.RemoteExtension;
import org.xwiki.extension.internal.ExtensionFactory;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.repository.ExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.xwiki.model.jaxb.AbstractExtension;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionAuthor;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionDependency;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionId;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionIssueManagement;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionRating;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionRepository;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionScm;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionScmConnection;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionSummary;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionSupportPlan;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionSupporter;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersion;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersionSummary;
import org.xwiki.extension.repository.xwiki.model.jaxb.License;
import org.xwiki.extension.repository.xwiki.model.jaxb.Namespaces;
import org.xwiki.extension.repository.xwiki.model.jaxb.ObjectFactory;
import org.xwiki.extension.repository.xwiki.model.jaxb.Property;
import org.xwiki.extension.version.Version;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.RatingsManagerFactory;
import org.xwiki.repository.internal.ExtensionStore;
import org.xwiki.repository.internal.RepositoryManager;
import org.xwiki.repository.internal.XWikiRepositoryModel;
import org.xwiki.repository.internal.XWikiRepositoryModel.SolrField;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Base class for the annotation REST services, to implement common functionality to all annotation REST services.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public abstract class AbstractExtensionRESTResource extends XWikiResource implements Initializable
{
    protected static final String DEFAULT_BOOST;

    protected static final String DEFAULT_FL;

    protected static final Map<String, Integer> EPROPERTIES_INDEX =
        Map.of(XWikiRepositoryModel.PROP_EXTENSION_ID, 0, XWikiRepositoryModel.PROP_EXTENSION_NAME, 1,
            XWikiRepositoryModel.PROP_EXTENSION_TYPE, 2, XWikiRepositoryModel.PROP_VERSION_VERSION, 3);

    // Note that the order of the retrieved information is stable and documented by EPROPERTIES_INDEX,
    // therefore it shouldn't be changed or it would break some of the APIs relying on it.
    protected static final String SELECT_EXTENSIONSUMMARY = String.format(
        "doc.name, doc.space, " + "extension.%s, extension.%s, extension.%s", XWikiRepositoryModel.PROP_EXTENSION_ID,
        XWikiRepositoryModel.PROP_EXTENSION_NAME, XWikiRepositoryModel.PROP_EXTENSION_TYPE);

    protected static final String SELECT_EXTENSION_VERSION = String.format(
        "doc.name, doc.space, " + "extensionVersion.%s, extensionVersion.%s, extensionVersion.%s, "
            + "extensionVersion.%s, extensionVersion.%s",
        XWikiRepositoryModel.PROP_EXTENSION_ID, XWikiRepositoryModel.PROP_EXTENSION_NAME,
        XWikiRepositoryModel.PROP_EXTENSION_TYPE, XWikiRepositoryModel.PROP_VERSION_VERSION,
        XWikiRepositoryModel.PROP_VERSION_INDEX);

    static {
        // Solr

        StringBuilder boostBuilder = new StringBuilder();
        StringBuilder flBuilder = new StringBuilder("wiki,spaces,name");
        for (SolrField field : XWikiRepositoryModel.SOLR_FIELDS.values()) {
            // Boost
            if (field.boostValue != null) {
                if (!boostBuilder.isEmpty()) {
                    boostBuilder.append(' ');
                }
                boostBuilder.append(field.boostName);
                boostBuilder.append('^');
                boostBuilder.append(field.boostValue);
            }

            // Fields list
            if (field.name != null) {
                if (!flBuilder.isEmpty()) {
                    flBuilder.append(',');
                }
                flBuilder.append(field.name);
            }
        }
        DEFAULT_BOOST = boostBuilder.toString();
        DEFAULT_FL = flBuilder.toString();
    }

    @Inject
    protected RepositoryManager repositoryManager;

    @Inject
    protected ContextualAuthorizationManager authorization;

    /**
     * Used to extract a document reference from a {@link SolrDocument}.
     */
    @Inject
    protected DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @Inject
    protected ExtensionFactory extensionFactory;

    @Inject
    protected ExtensionStore extensionStore;

    @Inject
    private RatingsManagerFactory ratingsManagerFactory;

    /**
     * The object factory for model objects to be used when creating representations.
     */
    protected ObjectFactory extensionObjectFactory;

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        this.extensionObjectFactory = new ObjectFactory();
    }

    public XWikiDocument getExistingExtensionDocumentById(String extensionId) throws QueryException, XWikiException
    {
        XWikiDocument document = this.extensionStore.getExistingExtensionDocumentById(extensionId);

        if (document == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return document;
    }

    protected Query createExtensionsCountQuery(String from, String where) throws QueryException
    {
        // select

        String select = "count(extension." + XWikiRepositoryModel.PROP_EXTENSION_ID + ")";

        return createExtensionsQuery(select, from, where, 0, -1, false, false);
    }

    protected long getExtensionsCountResult(Query query) throws QueryException
    {
        return ((Number) query.execute().get(0)).intValue();
    }

    protected Query createExtensionsSummariesQuery(String from, String where, int offset, int number, boolean versions,
        boolean pageVersion) throws QueryException
    {
        String select = SELECT_EXTENSIONSUMMARY;

        if (versions && pageVersion) {
            select = SELECT_EXTENSION_VERSION;
        }

        return createExtensionsQuery(select, from, where, offset, number, versions, pageVersion);
    }

    private Query createExtensionsQuery(String select, String from, String where, int offset, int number,
        boolean versions, boolean pageVersion) throws QueryException
    {
        // select

        StringBuilder queryStr = new StringBuilder("select ");
        queryStr.append(select);

        if (versions && !pageVersion) {
            queryStr.append(", extensionVersion." + XWikiRepositoryModel.PROP_VERSION_VERSION + "");
        }

        queryStr.append(" from Document doc");

        // from
        if (!pageVersion) {
            queryStr.append(", doc.object(" + XWikiRepositoryModel.EXTENSION_CLASSNAME + ") as extension");
        }
        if (versions) {
            queryStr
                .append(", doc.object(" + XWikiRepositoryModel.EXTENSIONVERSION_CLASSNAME + ") as extensionVersion");
        }

        if (from != null) {
            queryStr.append(',');
            queryStr.append(from);
        }

        // where

        queryStr.append(" where ");
        if (where != null) {
            queryStr.append('(');
            queryStr.append(where);
            queryStr.append(')');
        }
        if (versions && pageVersion) {
            queryStr.append(" order by extensionVersion." + XWikiRepositoryModel.PROP_VERSION_INDEX);
        } else {
            if (where != null) {
                queryStr.append(" and ");
            }
            queryStr.append("extension." + XWikiRepositoryModel.PROP_EXTENSION_VALIDEXTENSION + " = 1");
        }

        Query query = this.queryManager.createQuery(queryStr.toString(), Query.XWQL);

        if (offset > 0) {
            query.setOffset(offset);
        }
        if (number > 0) {
            query.setLimit(number);
        }

        return query;
    }

    protected BaseObject getExtensionObject(XWikiDocument extensionDocument)
    {
        return extensionDocument.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
    }

    protected BaseObject getExtensionObject(String extensionId) throws XWikiException, QueryException
    {
        return getExtensionObject(getExistingExtensionDocumentById(extensionId));
    }

    protected void addExtensionAuthors(AbstractExtension extension, Collection<String> ids)
    {
        if (ids != null) {
            for (String authorId : ids) {
                extension.getAuthors().add(resolveExtensionAuthor(authorId));
            }
        }
    }

    protected void addExtensionSupportPlans(AbstractExtension extension, Collection<String> ids)
    {
        if (ids != null) {
            for (String id : ids) {
                ExtensionSupportPlan supportPlan = resolveExtensionSupportPlan(id);
                if (supportPlan != null) {
                    extension.getSupportPlans().add(supportPlan);
                }
            }

            // Set recommended to true by default if there is any active support plan
            if (!extension.getSupportPlans().isEmpty()) {
                extension.setRecommended(true);
            }
        }
    }

    protected void addLicense(AbstractExtension extension, String licenseName)
    {
        if (StringUtils.isNotBlank(licenseName)) {
            License license = this.extensionObjectFactory.createLicense();
            license.setName(licenseName);
            extension.getLicenses().add(license);
        }
    }

    protected <E extends AbstractExtension> E createExtension(XWikiDocument extensionDocument, String version)
        throws XWikiException
    {
        BaseObject extensionObject = getExtensionObject(extensionDocument);
        XWikiDocument versionDocument =
            this.extensionStore.getExtensionVersionDocument(extensionDocument, version, getXWikiContext());
        DocumentReference extensionDocumentReference = extensionDocument.getDocumentReference();

        if (extensionObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        AbstractExtension extension;
        ExtensionVersion extensionVersion;
        BaseObject extensionVersionObject;
        List<BaseObject> extensionVersionObjects;
        if (version == null) {
            extension = this.extensionObjectFactory.createExtension();
            extensionVersion = null;
            extensionVersionObject = extensionObject;
            extensionVersionObjects = List.of(extensionVersionObject);
        } else {
            extensionVersionObject =
                this.repositoryManager.getExtensionVersionObject(extensionDocument, version, getXWikiContext());

            if (extensionVersionObject == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            extensionVersion = this.extensionObjectFactory.createExtensionVersion();
            extension = extensionVersion;
            extensionVersion.setVersion(
                this.extensionStore.getValue(extensionVersionObject, XWikiRepositoryModel.PROP_VERSION_VERSION));

            extensionVersionObjects = List.of(extensionVersionObject, extensionObject);
        }

        extension.setId(this.extensionStore.getValue(extensionVersionObjects, XWikiRepositoryModel.PROP_EXTENSION_ID));
        extension.setType(StringUtils.stripToNull(
            this.extensionStore.getValue(extensionVersionObjects, XWikiRepositoryModel.PROP_EXTENSION_TYPE)));

        extension.setRating(getExtensionRating(extensionDocumentReference));
        extension.setSummary(
            this.extensionStore.getValue(extensionVersionObjects, XWikiRepositoryModel.PROP_EXTENSION_SUMMARY));
        extension.setDescription(
            this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION));
        extension
            .setName(this.extensionStore.getValue(extensionVersionObjects, XWikiRepositoryModel.PROP_EXTENSION_NAME));
        extension.setCategory(
            this.extensionStore.getValue(extensionVersionObjects, XWikiRepositoryModel.PROP_EXTENSION_CATEGORY));
        extension.setWebsite(StringUtils.defaultIfEmpty(
            this.extensionStore.getValue(extensionVersionObjects, XWikiRepositoryModel.PROP_EXTENSION_WEBSITE),
            extensionDocument.getExternalURL("view", getXWikiContext())));

        // Recommended
        extension.setRecommended(this.extensionStore.getBooleanValue(extensionObject,
            XWikiRepositoryModel.PROP_EXTENSION_RECOMMENDED, false));

        // Support plans
        addExtensionSupportPlans(extension,
            this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SUPPORTPLANS));

        // SCM
        ExtensionScm scm = new ExtensionScm();
        scm.setUrl(this.extensionStore.getValue(extensionVersionObjects, XWikiRepositoryModel.PROP_EXTENSION_SCMURL));
        scm.setConnection(toScmConnection(
            this.extensionStore.getValue(extensionVersionObjects, XWikiRepositoryModel.PROP_EXTENSION_SCMCONNECTION)));
        scm.setDeveloperConnection(toScmConnection(this.extensionStore.getValue(extensionVersionObjects,
            XWikiRepositoryModel.PROP_EXTENSION_SCMDEVCONNECTION)));
        extension.setScm(scm);

        // Issue Management
        ExtensionIssueManagement issueManagement = new ExtensionIssueManagement();
        issueManagement.setSystem(this.extensionStore.getValue(extensionVersionObjects,
            XWikiRepositoryModel.PROP_EXTENSION_ISSUEMANAGEMENT_SYSTEM));
        issueManagement.setUrl(this.extensionStore.getValue(extensionVersionObjects,
            XWikiRepositoryModel.PROP_EXTENSION_ISSUEMANAGEMENT_URL));
        if (StringUtils.isNotEmpty(issueManagement.getSystem()) || StringUtils.isNotEmpty(issueManagement.getUrl())) {
            extension.setIssueManagement(issueManagement);
        }

        // Authors
        addExtensionAuthors(extension,
            this.extensionStore.getValue(extensionVersionObjects, XWikiRepositoryModel.PROP_EXTENSION_AUTHORS));

        // Features
        List<String> features =
            this.extensionStore.getValue(extensionVersionObject, XWikiRepositoryModel.PROP_VERSION_FEATURES);

        extractFeatureInformation(features, extension);

        // License
        addLicense(extension,
            this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME));

        // Allowed namespaces
        List<String> namespaces =
            this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ALLOWEDNAMESPACES);
        Integer namespacesEmpty = this.extensionStore.getValue(extensionObject,
            XWikiRepositoryModel.PROP_EXTENSION_ALLOWEDNAMESPACES_EMPTY, 0);
        if (namespaces != null && (!namespaces.isEmpty() || namespacesEmpty == 1)) {
            Namespaces restNamespaces = this.extensionObjectFactory.createNamespaces();
            restNamespaces.withNamespaces(namespaces);
            extension.setAllowedNamespaces(restNamespaces);
        }

        // Properties
        addProperties(extension,
            this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_PROPERTIES));

        if (extensionVersion != null) {
            List<String> repositories =
                this.extensionStore.getValue(extensionVersionObjects, XWikiRepositoryModel.PROP_VERSION_REPOSITORIES);
            extensionVersion.withRepositories(toExtensionRepositories(repositories));

            // Dependencies
            List<BaseObject> dependencies =
                versionDocument.getXObjects(XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSREFERENCE);
            if (dependencies != null) {
                for (BaseObject dependencyObject : dependencies) {
                    if (dependencyObject != null) {
                        if (Strings.CS.equals(this.extensionStore.getValue(dependencyObject,
                            XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION, null), version)) {
                            ExtensionDependency dependency = extensionObjectFactory.createExtensionDependency();
                            dependency.setId(this.extensionStore.getValue(dependencyObject,
                                XWikiRepositoryModel.PROP_DEPENDENCY_ID));
                            dependency.setConstraint(this.extensionStore.getValue(dependencyObject,
                                XWikiRepositoryModel.PROP_DEPENDENCY_CONSTRAINT));
                            dependency.setOptional(this.extensionStore.getBooleanValue(dependencyObject,
                                XWikiRepositoryModel.PROP_DEPENDENCY_OPTIONAL, false));
                            List<String> dependencyRepositories = this.extensionStore.getValue(dependencyObject,
                                XWikiRepositoryModel.PROP_DEPENDENCY_REPOSITORIES);
                            dependency.withRepositories(toExtensionRepositories(dependencyRepositories));

                            extensionVersion.getDependencies().add(dependency);
                        }
                    }
                }
            }
        }

        return (E) extension;
    }

    private void extractFeatureInformation(Collection<String> features, AbstractExtension extension)
    {
        if (features != null && !features.isEmpty()) {
            for (String feature : features) {
                org.xwiki.extension.ExtensionId extensionId = ExtensionIdConverter.toExtensionId(feature, null);
                ExtensionId extensionFeature = this.extensionObjectFactory.createExtensionId();
                extensionFeature.setId(extensionId.getId());
                if (extensionId.getVersion() != null) {
                    extensionFeature.setVersion(extensionId.getVersion().getValue());
                }
                extension.getExtensionFeatures().add(extensionFeature);
                extension.getFeatures().add(extensionFeature.getId());
            }
        }
    }

    protected ExtensionScmConnection toScmConnection(String connectionString)
    {
        if (connectionString != null) {
            org.xwiki.extension.ExtensionScmConnection connection = new DefaultExtensionScmConnection(connectionString);

            ExtensionScmConnection restConnection = new ExtensionScmConnection();
            restConnection.setPath(connection.getPath());
            restConnection.setSystem(connection.getSystem());

            return restConnection;
        }

        return null;
    }

    protected List<ExtensionRepository> toExtensionRepositories(List<String> repositories)
    {
        if (repositories != null) {
            List<ExtensionRepository> restRepositories = new ArrayList<>(repositories.size());

            for (String repositoryString : repositories) {
                try {
                    restRepositories.add(toExtensionRepository(repositoryString));
                } catch (URISyntaxException e) {
                    this.slf4Jlogger.warn("Failed to parse repository descriptor [{}]: {}", repositoryString,
                        ExceptionUtils.getRootCauseMessage(e));
                }
            }

            return restRepositories;
        }

        return null;
    }

    protected ExtensionRepository toExtensionRepository(String repositoryString) throws URISyntaxException
    {
        if (repositoryString != null) {
            ExtensionRepositoryDescriptor descriptor =
                XWikiRepositoryModel.toRepositoryDescriptor(repositoryString, extensionFactory);

            ExtensionRepository restRepository = new ExtensionRepository();
            restRepository.setId(descriptor.getId());
            restRepository.setType(descriptor.getType());
            restRepository.setUri(descriptor.getURI().toString());

            return restRepository;
        }

        return null;
    }

    protected ExtensionAuthor resolveExtensionAuthor(String authorId)
    {
        ExtensionAuthor author = this.extensionObjectFactory.createExtensionAuthor();

        XWikiContext xcontext = getXWikiContext();

        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(authorId, xcontext);
        } catch (Exception e) {
            document = null;
        }

        if (document != null && !document.isNew()) {
            author.setName(xcontext.getWiki().getPlainUserName(document.getDocumentReference(), xcontext));
            author.setUrl(document.getExternalURL("view", xcontext));
        } else {
            author.setName(authorId);
        }

        return author;
    }

    protected ExtensionSupportPlan resolveExtensionSupportPlan(String supportPlanId)
    {
        XWikiContext xcontext = getXWikiContext();

        org.xwiki.extension.ExtensionSupportPlan supportPlan =
            this.extensionStore.resolveExtensionSupportPlan(supportPlanId, xcontext);

        if (supportPlan != null) {
            ExtensionSupportPlan restSupportPlan = this.extensionObjectFactory.createExtensionSupportPlan();

            restSupportPlan.setSupporter(resolveExtensionSupporter(supportPlan.getSupporter()));
            restSupportPlan.setName(supportPlan.getName());
            restSupportPlan.setPaying(supportPlan.isPaying());
            restSupportPlan.setUrl(supportPlan.getURL().toString());

            return restSupportPlan;
        }

        return null;
    }

    protected ExtensionSupporter resolveExtensionSupporter(org.xwiki.extension.ExtensionSupporter supporter)
    {
        if (supporter != null) {
            ExtensionSupporter restSupporter = this.extensionObjectFactory.createExtensionSupporter();

            restSupporter.setName(supporter.getName());
            restSupporter.setUrl(supporter.getURL().toString());

            return restSupporter;
        }

        return null;
    }

    protected ExtensionSupporter resolveExtensionSupporter(String supporterId) throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();

        org.xwiki.extension.ExtensionSupporter supporter =
            this.extensionStore.resolveExtensionSupporter(supporterId, xcontext);

        return resolveExtensionSupporter(supporter);
    }

    protected <T> T getSolrValue(SolrDocument document, String property, boolean emptyIsNull)
    {
        return getSolrValue(document, property, emptyIsNull, null);
    }

    protected <T> T getSolrValue(SolrDocument document, String property, boolean emptyIsNull, T def)
    {
        Object value = document.getFieldValue(XWikiRepositoryModel.toSolrField(property));

        if (value instanceof Collection collectionValue) {
            value = !collectionValue.isEmpty() ? collectionValue.iterator().next() : null;
        }

        if (value == null || (emptyIsNull && value instanceof String stringValue && stringValue.isEmpty())) {
            value = def;
        }

        return (T) value;
    }

    protected <T> Collection<T> getSolrValues(SolrDocument document, String property)
    {
        return (Collection) document.getFieldValues(XWikiRepositoryModel.toSolrField(property));
    }

    protected <T> T getQueryValue(Object[] entry, String property)
    {
        return (T) entry[EPROPERTIES_INDEX.get(property)];
    }

    protected ExtensionVersion createExtensionVersionFromSolrDocument(SolrDocument document)
    {
        XWikiContext xcontext = getXWikiContext();

        ExtensionVersion extension = this.extensionObjectFactory.createExtensionVersion();

        extension.setId(getSolrValue(document, Extension.FIELD_ID, true));
        extension.setType(StringUtils.stripToNull(getSolrValue(document, Extension.FIELD_TYPE, true)));
        extension.setName(getSolrValue(document, Extension.FIELD_NAME, false));
        extension.setSummary(getSolrValue(document, Extension.FIELD_SUMMARY, false));

        // Recommended
        extension.setRecommended(getSolrValue(document, RemoteExtension.FIELD_RECOMMENDED, false, false));

        // Support plans
        addExtensionSupportPlans(extension, getSolrValues(document, RemoteExtension.FIELD_SUPPORT_PLANS));

        // SCM
        ExtensionScm scm = new ExtensionScm();
        scm.setUrl(getSolrValue(document, Extension.FIELD_SCM, true));
        scm.setConnection(
            toScmConnection(getSolrValue(document, XWikiRepositoryModel.PROP_EXTENSION_SCMCONNECTION, true)));
        scm.setDeveloperConnection(
            toScmConnection(getSolrValue(document, XWikiRepositoryModel.PROP_EXTENSION_SCMDEVCONNECTION, true)));
        if (scm.getUrl() != null || scm.getConnection() != null || scm.getDeveloperConnection() != null) {
            extension.setScm(scm);
        }

        // Issue Management
        ExtensionIssueManagement issueManagement = new ExtensionIssueManagement();
        issueManagement
            .setSystem(getSolrValue(document, XWikiRepositoryModel.PROP_EXTENSION_ISSUEMANAGEMENT_SYSTEM, true));
        issueManagement.setUrl(getSolrValue(document, XWikiRepositoryModel.PROP_EXTENSION_ISSUEMANAGEMENT_URL, true));
        if (issueManagement.getSystem() != null || issueManagement.getUrl() != null) {
            extension.setIssueManagement(issueManagement);
        }

        // Rating
        ExtensionRating extensionRating = this.extensionObjectFactory.createExtensionRating();
        extensionRating.setTotalVotes(getSolrValue(document, XWikiRepositoryModel.PROP_RATING_TOTALVOTES, false, 0));
        extensionRating
            .setAverageVote(getSolrValue(document, XWikiRepositoryModel.PROP_RATING_AVERAGEVOTE, false, 0.0f));
        extension.setRating(extensionRating);

        // Website
        extension.setWebsite(getSolrValue(document, Extension.FIELD_WEBSITE, true));
        if (extension.getWebsite() == null) {
            DocumentReference extensionDocumentReference = this.solrDocumentReferenceResolver.resolve(document);
            extension.setWebsite(xcontext.getWiki().getURL(extensionDocumentReference, xcontext));
        }

        // Authors
        addExtensionAuthors(extension, getSolrValues(document, Extension.FIELD_AUTHORS));

        // Features
        Collection<String> features = getSolrValues(document, Extension.FIELD_FEATURES);
        extractFeatureInformation(features, extension);

        // License
        addLicense(extension, getSolrValue(document, Extension.FIELD_LICENSE, true));

        // Allowed namespaces
        Collection<String> namespaces = getSolrValues(document, Extension.FIELD_ALLOWEDNAMESPACES);
        if (namespaces != null && !namespaces.isEmpty()) {
            Namespaces restNamespaces = this.extensionObjectFactory.createNamespaces();
            restNamespaces.withNamespaces(namespaces);
            extension.setAllowedNamespaces(restNamespaces);
        }

        // Version
        extension.setVersion(getSolrValue(document, Extension.FIELD_VERSION, true));

        // Properties
        addProperties(extension, getSolrValues(document, Extension.FIELD_PROPERTIES));

        // TODO: add support for
        // * dependencies

        return extension;
    }

    protected Map<String, String> addProperties(AbstractExtension extension, Collection<String> properties)
    {
        Map<String, String> map = null;

        if (properties != null) {
            map = new HashMap<>();

            for (String stringProperty : properties) {
                int index = stringProperty.indexOf('=');
                if (index > 0) {
                    Property property = new Property();
                    property.setKey(stringProperty.substring(0, index));
                    property.setStringValue(
                        (index + 1) < stringProperty.length() ? stringProperty.substring(index + 1) : "");
                    extension.getProperties().add(property);
                }
            }
        }

        return map;
    }

    protected ExtensionRating getExtensionRating(DocumentReference extensionDocumentReference)
    {
        ExtensionRating extensionRating = this.extensionObjectFactory.createExtensionRating();

        try {
            RatingsManager ratingsManager =
                this.ratingsManagerFactory.getRatingsManager(RatingsManagerFactory.DEFAULT_APP_HINT);
            AverageRating averageRating = ratingsManager.getAverageRating(extensionDocumentReference);
            extensionRating.setTotalVotes(averageRating.getNbVotes());
            extensionRating.setAverageVote(averageRating.getAverageVote());
        } catch (RatingsException e) {
            extensionRating.setTotalVotes(0);
            extensionRating.setAverageVote(0);
        }

        return extensionRating;
    }

    protected ExtensionVersionSummary createExtensionVersionSummary(String extensionId, String type, String name,
        Version version)
    {
        ExtensionVersionSummary extensionVersion = this.extensionObjectFactory.createExtensionVersionSummary();
        extensionVersion.setVersion(version.getValue());
        extensionVersion.setId(extensionId);
        extensionVersion.setType(type);
        extensionVersion.setName(name);

        return extensionVersion;
    }

    protected <E extends ExtensionSummary> void getExtensionSummaries(List<E> extensions, Query query)
        throws QueryException
    {
        List<Object[]> entries = query.execute();

        for (Object[] entry : entries) {
            extensions.add((E) createExtensionSummaryFromQueryResult(entry));
        }
    }

    protected ExtensionSummary createExtensionSummaryFromQueryResult(Object[] entry)
    {
        ExtensionSummary extension;
        int versionIndex = EPROPERTIES_INDEX.get(XWikiRepositoryModel.PROP_VERSION_VERSION);
        if (entry.length == versionIndex) {
            // It's a extension summary without version
            extension = this.extensionObjectFactory.createExtensionSummary();
        } else {
            ExtensionVersionSummary extensionVersion = this.extensionObjectFactory.createExtensionVersionSummary();
            extensionVersion.setVersion((String) entry[versionIndex]);
            extension = extensionVersion;
        }

        extension.setId(getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_ID));
        extension.setType(StringUtils.stripToNull(getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_TYPE)));
        extension.setName(getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_NAME));

        return extension;
    }

    protected ResponseBuilder getAttachmentResponse(XWikiAttachment xwikiAttachment) throws XWikiException
    {
        if (xwikiAttachment == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        ResponseBuilder response = Response.ok();

        XWikiContext xcontext = getXWikiContext();

        response = response.type(xwikiAttachment.getMimeType(xcontext));
        response = response.entity(xwikiAttachment.getContent(xcontext));
        response =
            response.header("content-disposition", "attachment; filename=\"" + xwikiAttachment.getFilename() + "\"");

        return response;
    }

    protected void checkRights(XWikiDocument document) throws XWikiException
    {
        if (!this.authorization.hasAccess(Right.VIEW, document.getDocumentReference())) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
    }
}
