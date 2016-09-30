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
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.solr.common.SolrDocument;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.RemoteExtension;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.internal.maven.MavenUtils;
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
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersion;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersionSummary;
import org.xwiki.extension.repository.xwiki.model.jaxb.License;
import org.xwiki.extension.repository.xwiki.model.jaxb.Namespaces;
import org.xwiki.extension.repository.xwiki.model.jaxb.ObjectFactory;
import org.xwiki.extension.repository.xwiki.model.jaxb.Property;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.ratings.AverageRatingApi;
import org.xwiki.ratings.RatingsManager;
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
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * Base class for the annotation REST services, to implement common functionality to all annotation REST services.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public abstract class AbstractExtensionRESTResource extends XWikiResource implements Initializable
{
    public static final String[] EPROPERTIES_SUMMARY =
        new String[] { XWikiRepositoryModel.PROP_EXTENSION_ID, XWikiRepositoryModel.PROP_EXTENSION_TYPE,
        XWikiRepositoryModel.PROP_EXTENSION_NAME, XWikiRepositoryModel.PROP_EXTENSION_PROPERTIES };

    protected static final String DEFAULT_BOOST;

    protected static final String DEFAULT_FL;

    protected static Map<String, Integer> EPROPERTIES_INDEX = new HashMap<String, Integer>();

    protected static String SELECT_EXTENSIONSUMMARY;

    static {
        StringBuilder pattern = new StringBuilder();

        int j = 0;

        pattern.append("doc.name");
        EPROPERTIES_INDEX.put("doc.name", j++);
        pattern.append(", ");
        pattern.append("doc.space");
        EPROPERTIES_INDEX.put("doc.space", j++);

        // Extension summary
        for (int i = 0; i < EPROPERTIES_SUMMARY.length; ++i, ++j) {
            String value = EPROPERTIES_SUMMARY[i];
            pattern.append(", extension.");
            pattern.append(value);
            EPROPERTIES_INDEX.put(value, j);
        }

        SELECT_EXTENSIONSUMMARY = pattern.toString();

        // Solr

        StringBuilder boostBuilder = new StringBuilder();
        StringBuilder flBuilder = new StringBuilder("wiki,spaces,name");
        for (SolrField field : XWikiRepositoryModel.SOLR_FIELDS.values()) {
            // Boost
            if (field.boostValue != null) {
                if (boostBuilder.length() > 0) {
                    boostBuilder.append(' ');
                }
                boostBuilder.append(field.boostName);
                boostBuilder.append('^');
                boostBuilder.append(field.boostValue);
            }

            // Fields list
            if (field.name != null) {
                if (flBuilder.length() > 0) {
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

    @Inject
    @Named("separate")
    protected RatingsManager ratingsManager;

    /**
     * Used to extract a document reference from a {@link SolrDocument}.
     */
    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

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
        XWikiDocument document = this.repositoryManager.getExistingExtensionDocumentById(extensionId);

        if (document == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return document;
    }

    protected Query createExtensionsCountQuery(String from, String where) throws QueryException
    {
        // select

        String select = "count(extension." + XWikiRepositoryModel.PROP_EXTENSION_ID + ")";

        return createExtensionsQuery(select, from, where, 0, -1, false);
    }

    protected long getExtensionsCountResult(Query query) throws QueryException
    {
        return ((Number) query.execute().get(0)).intValue();
    }

    protected Query createExtensionsSummariesQuery(String from, String where, int offset, int number, boolean versions)
        throws QueryException
    {
        String select = SELECT_EXTENSIONSUMMARY;

        return createExtensionsQuery(select, from, where, offset, number, versions);
    }

    private Query createExtensionsQuery(String select, String from, String where, int offset, int number,
        boolean versions) throws QueryException
    {
        // select

        StringBuilder queryStr = new StringBuilder("select ");
        queryStr.append(select);

        if (versions) {
            queryStr.append(", extensionVersion." + XWikiRepositoryModel.PROP_VERSION_VERSION + "");
        }

        // from

        queryStr
            .append(" from Document doc, doc.object(" + XWikiRepositoryModel.EXTENSION_CLASSNAME + ") as extension");

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
            queryStr.append(" and ");
        }
        queryStr.append("extension." + XWikiRepositoryModel.PROP_EXTENSION_VALIDEXTENSION + " = 1");

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

    protected BaseObject getExtensionVersionObject(XWikiDocument extensionDocument, String version)
    {
        if (version == null) {
            List<BaseObject> objects =
                extensionDocument.getXObjects(XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE);

            if (objects == null || objects.isEmpty()) {
                return null;
            } else {
                return objects.get(objects.size() - 1);
            }
        }

        return extensionDocument.getObject(XWikiRepositoryModel.EXTENSIONVERSION_CLASSNAME, "version", version, false);
    }

    protected BaseObject getExtensionVersionObject(String extensionId, String version)
        throws XWikiException, QueryException
    {
        return getExtensionVersionObject(getExistingExtensionDocumentById(extensionId), version);
    }

    protected <E extends AbstractExtension> E createExtension(XWikiDocument extensionDocument, String version)
    {
        BaseObject extensionObject = getExtensionObject(extensionDocument);
        DocumentReference extensionDocumentReference = extensionDocument.getDocumentReference();

        if (extensionObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        AbstractExtension extension;
        ExtensionVersion extensionVersion;
        BaseObject extensionVersionObject;
        if (version == null) {
            extension = this.extensionObjectFactory.createExtension();
            extensionVersion = null;
            extensionVersionObject = null;
        } else {
            extensionVersionObject = getExtensionVersionObject(extensionDocument, version);

            if (extensionVersionObject == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            extensionVersion = this.extensionObjectFactory.createExtensionVersion();
            extension = extensionVersion;
            extensionVersion
                .setVersion((String) getValue(extensionVersionObject, XWikiRepositoryModel.PROP_VERSION_VERSION));
        }

        extension.setId((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ID));
        extension.setType((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_TYPE));

        License license = this.extensionObjectFactory.createLicense();
        license.setName((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME));
        extension.getLicenses().add(license);

        extension.setRating(getExtensionRating(extensionDocumentReference));
        extension.setSummary((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SUMMARY));
        extension.setDescription((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION));
        extension.setName((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_NAME));
        extension.setCategory((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_CATEGORY));
        extension.setWebsite(
            StringUtils.defaultIfEmpty((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_WEBSITE),
                extensionDocument.getExternalURL("view", getXWikiContext())));

        // Recommended
        Integer recommended = getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_RECOMMENDED, 0);
        extension.setRecommended(recommended.intValue() == 1);

        // SCM
        ExtensionScm scm = new ExtensionScm();
        scm.setUrl((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SCMURL));
        scm.setConnection(
            toScmConnection((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SCMCONNECTION)));
        scm.setDeveloperConnection(
            toScmConnection((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SCMDEVCONNECTION)));
        extension.setScm(scm);

        // Issue Management
        ExtensionIssueManagement issueManagement = new ExtensionIssueManagement();
        issueManagement
            .setSystem((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ISSUEMANAGEMENT_SYSTEM));
        issueManagement
            .setUrl((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ISSUEMANAGEMENT_URL));
        if (StringUtils.isNotEmpty(issueManagement.getSystem()) || StringUtils.isNotEmpty(issueManagement.getUrl())) {
            extension.setIssueManagement(issueManagement);
        }

        // Authors
        List<String> authors = (List<String>) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_AUTHORS);
        if (authors != null) {
            for (String authorId : authors) {
                extension.getAuthors().add(resolveExtensionAuthor(authorId));
            }
        }

        // Features
        List<String> features;
        if (extensionVersionObject != null) {
            features = (List<String>) getValue(extensionVersionObject, XWikiRepositoryModel.PROP_VERSION_FEATURES);
        } else {
            features = (List<String>) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_FEATURES);
        }
        if (features != null && !features.isEmpty()) {
            extension.withFeatures(features);
            for (String feature : features) {
                org.xwiki.extension.ExtensionId extensionId = ExtensionIdConverter.toExtensionId(feature, null);
                ExtensionId extensionFeature = this.extensionObjectFactory.createExtensionId();
                extensionFeature.setId(extensionId.getId());
                if (extensionId.getVersion() != null) {
                    extensionFeature.setVersion(extensionId.getVersion().getValue());
                }
                extension.getExtensionFeatures().add(extensionFeature);
            }
        }

        // Allowed namespaces
        List<String> namespaces =
            (List<String>) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ALLOWEDNAMESPACES);
        if (namespaces != null && !namespaces.isEmpty()) {
            Namespaces restNamespaces = this.extensionObjectFactory.createNamespaces();
            restNamespaces.withNamespaces(namespaces);
            extension.setAllowedNamespaces(restNamespaces);
        }

        // Repositories
        if (extensionVersionObject != null) {
            List<String> repositories =
                (List<String>) getValue(extensionVersionObject, XWikiRepositoryModel.PROP_VERSION_REPOSITORIES);
            extensionVersion.withRepositories(toExtensionRepositories(repositories));
        }

        // Properties
        addProperties(extension,
            (List<String>) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_PROPERTIES));

        // Dependencies
        if (extensionVersion != null) {
            List<BaseObject> dependencies =
                extensionDocument.getXObjects(XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSREFERENCE);
            if (dependencies != null) {
                for (BaseObject dependencyObject : dependencies) {
                    if (dependencyObject != null) {
                        if (StringUtils.equals(getValue(dependencyObject,
                            XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION, (String) null), version)) {
                            ExtensionDependency dependency = extensionObjectFactory.createExtensionDependency();
                            dependency
                                .setId((String) getValue(dependencyObject, XWikiRepositoryModel.PROP_DEPENDENCY_ID));
                            dependency.setConstraint(
                                (String) getValue(dependencyObject, XWikiRepositoryModel.PROP_DEPENDENCY_CONSTRAINT));
                            List<String> repositories = (List<String>) getValue(dependencyObject,
                                XWikiRepositoryModel.PROP_DEPENDENCY_REPOSITORIES);
                            dependency.withRepositories(toExtensionRepositories(repositories));

                            extensionVersion.getDependencies().add(dependency);
                        }
                    }
                }
            }
        }

        return (E) extension;
    }

    protected ExtensionScmConnection toScmConnection(String connectionString)
    {
        if (connectionString != null) {
            org.xwiki.extension.ExtensionScmConnection connection =
                MavenUtils.toExtensionScmConnection(connectionString);

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
                    this.slf4Jlogger.warn("Failed to parse repository descriptor [{}]", repositoryString,
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
            ExtensionRepositoryDescriptor descriptor = XWikiRepositoryModel.toRepositoryDescriptor(repositoryString);

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
        ExtensionAuthor author = new ExtensionAuthor();

        XWikiContext xcontext = getXWikiContext();

        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(authorId, xcontext);
        } catch (XWikiException e) {
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

    protected void getExtensions(List<ExtensionVersion> extensions, Query query) throws QueryException
    {
        List<Object[]> entries = query.execute();

        for (Object[] entry : entries) {
            extensions.add(createExtensionVersionFromQueryResult(entry));
        }
    }

    protected <T> T getSolrValue(SolrDocument document, String property, boolean emptyIsNull)
    {
        return getSolrValue(document, property, emptyIsNull, null);
    }

    protected <T> T getSolrValue(SolrDocument document, String property, boolean emptyIsNull, T def)
    {
        Object value = document.getFieldValue(XWikiRepositoryModel.toSolrField(property));

        if (value instanceof Collection) {
            Collection collectionValue = (Collection) value;
            value = collectionValue.size() > 0 ? collectionValue.iterator().next() : null;
        }

        if (value == null || (emptyIsNull && value instanceof String && ((String) value).isEmpty())) {
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

    protected ExtensionVersion createExtensionVersionFromQueryResult(Object[] entry)
    {
        XWikiContext xcontext = getXWikiContext();

        String documentName = (String) entry[0];
        String documentSpace = (String) entry[1];

        ExtensionVersion extension = this.extensionObjectFactory.createExtensionVersion();

        extension.setId(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_ID));
        extension.setType(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_TYPE));
        extension.setName(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_NAME));
        extension.setSummary(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_SUMMARY));

        // Recommended
        Integer recommended = this.<Integer>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_RECOMMENDED);
        extension.setRecommended(recommended != null && recommended.intValue() == 1);

        // SCM
        ExtensionScm scm = new ExtensionScm();
        scm.setUrl(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_SCMURL));
        scm.setConnection(
            toScmConnection(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_SCMCONNECTION)));
        scm.setDeveloperConnection(
            toScmConnection(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_SCMDEVCONNECTION)));
        extension.setScm(scm);

        // Issue Management
        ExtensionIssueManagement issueManagement = new ExtensionIssueManagement();
        issueManagement
            .setSystem(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_ISSUEMANAGEMENT_SYSTEM));
        issueManagement
            .setUrl(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_ISSUEMANAGEMENT_URL));
        if (StringUtils.isNotEmpty(issueManagement.getSystem()) || StringUtils.isNotEmpty(issueManagement.getUrl())) {
            extension.setIssueManagement(issueManagement);
        }

        // Rating
        DocumentReference extensionDocumentReference =
            new DocumentReference(xcontext.getWikiId(), documentSpace, documentName);
        // FIXME: this adds potentially tons of new request to what used to be carefully crafted to produce a single
        // request for the whole search... Should be cached in a filed of the document (like the last version is for
        // example).
        extension.setRating(getExtensionRating(extensionDocumentReference));

        // Website
        extension.setWebsite(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_WEBSITE));
        if (StringUtils.isBlank(extension.getWebsite())) {
            extension.setWebsite(xcontext.getWiki()
                .getURL(new DocumentReference(xcontext.getWikiId(), documentSpace, documentName), "view", xcontext));
        }

        // Authors
        for (String authorId : ListClass.getListFromString(
            this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_AUTHORS), "|", false)) {
            extension.getAuthors().add(resolveExtensionAuthor(authorId));
        }

        // Features
        List<String> features = ListClass.getListFromString(
            this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_FEATURES), "|", false);
        if (features != null && !features.isEmpty()) {
            extension.withFeatures(features);
            for (String feature : features) {
                org.xwiki.extension.ExtensionId extensionId = ExtensionIdConverter.toExtensionId(feature, null);
                ExtensionId extensionFeature = this.extensionObjectFactory.createExtensionId();
                extensionFeature.setId(extensionId.getId());
                if (extensionId.getVersion() != null) {
                    extensionFeature.setVersion(extensionId.getVersion().getValue());
                }
                extension.getExtensionFeatures().add(extensionFeature);
            }
        }

        // Allowed namespaces
        String namespacesString =
            this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_ALLOWEDNAMESPACES);
        if (!StringUtils.isEmpty(namespacesString)) {
            List<String> namespaces = ListClass.getListFromString(namespacesString, "|", false);
            Namespaces restNamespaces = this.extensionObjectFactory.createNamespaces();
            restNamespaces.withNamespaces(namespaces);
            extension.setAllowedNamespaces(restNamespaces);
        }

        // License
        License license = this.extensionObjectFactory.createLicense();
        license.setName(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME));
        extension.getLicenses().add(license);

        // Version
        extension.setVersion((String) entry[EPROPERTIES_INDEX.size()]);

        // TODO: add support for
        // * description
        // * dependencies

        return extension;
    }

    protected ExtensionVersion createExtensionVersionFromSolrDocument(SolrDocument document)
    {
        XWikiContext xcontext = getXWikiContext();

        ExtensionVersion extension = this.extensionObjectFactory.createExtensionVersion();

        extension.setId(this.<String>getSolrValue(document, Extension.FIELD_ID, true));
        extension.setType(this.<String>getSolrValue(document, Extension.FIELD_TYPE, true));
        extension.setName(this.<String>getSolrValue(document, Extension.FIELD_NAME, false));
        extension.setSummary(this.<String>getSolrValue(document, Extension.FIELD_SUMMARY, false));

        // Recommended
        Boolean recommended = this.<Boolean>getSolrValue(document, RemoteExtension.FIELD_RECOMMENDED, false, false);
        if (recommended == Boolean.TRUE) {
            extension.setRecommended(recommended);
        }

        // SCM
        ExtensionScm scm = new ExtensionScm();
        scm.setUrl(this.<String>getSolrValue(document, Extension.FIELD_SCM, true));
        scm.setConnection(toScmConnection(
            this.<String>getSolrValue(document, XWikiRepositoryModel.PROP_EXTENSION_SCMCONNECTION, true)));
        scm.setDeveloperConnection(toScmConnection(
            this.<String>getSolrValue(document, XWikiRepositoryModel.PROP_EXTENSION_SCMDEVCONNECTION, true)));
        if (scm.getUrl() != null || scm.getConnection() != null || scm.getDeveloperConnection() != null) {
            extension.setScm(scm);
        }

        // Issue Management
        ExtensionIssueManagement issueManagement = new ExtensionIssueManagement();
        issueManagement.setSystem(
            this.<String>getSolrValue(document, XWikiRepositoryModel.PROP_EXTENSION_ISSUEMANAGEMENT_SYSTEM, true));
        issueManagement
            .setUrl(this.<String>getSolrValue(document, XWikiRepositoryModel.PROP_EXTENSION_ISSUEMANAGEMENT_URL, true));
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
        extension.setWebsite(this.<String>getSolrValue(document, Extension.FIELD_WEBSITE, true));
        if (extension.getWebsite() == null) {
            DocumentReference extensionDocumentReference = this.solrDocumentReferenceResolver.resolve(document);
            extension.setWebsite(xcontext.getWiki().getURL(extensionDocumentReference, xcontext));
        }

        // Authors
        Collection<String> authors = this.<String>getSolrValues(document, Extension.FIELD_AUTHORS);
        if (authors != null) {
            for (String authorId : authors) {
                extension.getAuthors().add(resolveExtensionAuthor(authorId));
            }
        }

        // Features
        Collection<String> features = this.<String>getSolrValues(document, Extension.FIELD_FEATURES);
        if (features != null && !features.isEmpty()) {
            extension.withFeatures(features);
            for (String feature : features) {
                org.xwiki.extension.ExtensionId extensionId = ExtensionIdConverter.toExtensionId(feature, null);
                ExtensionId extensionFeature = this.extensionObjectFactory.createExtensionId();
                extensionFeature.setId(extensionId.getId());
                if (extensionId.getVersion() != null) {
                    extensionFeature.setVersion(extensionId.getVersion().getValue());
                }
                extension.getExtensionFeatures().add(extensionFeature);
            }
        }

        // License
        String licenseName = this.<String>getSolrValue(document, Extension.FIELD_LICENSE, true);
        if (licenseName != null) {
            License license = this.extensionObjectFactory.createLicense();
            license.setName(licenseName);
            extension.getLicenses().add(license);
        }

        // Allowed namespaces
        Collection<String> namespaces = this.<String>getSolrValues(document, Extension.FIELD_ALLOWEDNAMESPACES);
        if (namespaces != null && !namespaces.isEmpty()) {
            Namespaces restNamespaces = this.extensionObjectFactory.createNamespaces();
            restNamespaces.withNamespaces(namespaces);
            extension.setAllowedNamespaces(restNamespaces);
        }

        // Version
        extension.setVersion(this.<String>getSolrValue(document, Extension.FIELD_VERSION, true));

        // Properties
        addProperties(extension, this.<String>getSolrValues(document, Extension.FIELD_PROPERTIES));

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
            AverageRatingApi averageRating =
                new AverageRatingApi(ratingsManager.getAverageRating(extensionDocumentReference));
            extensionRating.setTotalVotes(averageRating.getNbVotes());
            extensionRating.setAverageVote(averageRating.getAverageVote());
        } catch (XWikiException e) {
            extensionRating.setTotalVotes(0);
            extensionRating.setAverageVote(0);
        }

        return extensionRating;
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
        ExtensionVersionSummary extensionVersion;
        int versionIndex = EPROPERTIES_INDEX.get(EPROPERTIES_SUMMARY[EPROPERTIES_SUMMARY.length - 1]) + 1;
        if (entry.length == versionIndex) {
            // It's a extension summary without version
            extension = this.extensionObjectFactory.createExtensionSummary();
            extensionVersion = null;
        } else {
            extension = extensionVersion = this.extensionObjectFactory.createExtensionVersionSummary();
            extensionVersion.setVersion((String) entry[versionIndex]);
        }

        extension.setId(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_ID));
        extension.setType(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_TYPE));
        extension.setName(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_NAME));

        return extension;
    }

    protected <T> T getValue(BaseObject object, String field)
    {
        return getValue(object, field, (T) null);
    }

    protected <T> T getValue(BaseObject object, String field, T def)
    {
        BaseProperty<?> property = (BaseProperty<?>) object.safeget(field);

        return property != null ? (T) property.getValue() : def;
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
