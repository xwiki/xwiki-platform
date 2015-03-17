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
import org.apache.solr.common.SolrDocument;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.internal.maven.MavenUtils;
import org.xwiki.extension.repository.xwiki.model.jaxb.AbstractExtension;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionAuthor;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionDependency;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionRating;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionScm;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionScmConnection;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionSummary;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersion;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersionSummary;
import org.xwiki.extension.repository.xwiki.model.jaxb.License;
import org.xwiki.extension.repository.xwiki.model.jaxb.ObjectFactory;
import org.xwiki.extension.repository.xwiki.model.jaxb.Property;
import org.xwiki.model.reference.DocumentReference;
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
    public static final String[] EPROPERTIES_SUMMARY = new String[] {XWikiRepositoryModel.PROP_EXTENSION_ID,
    XWikiRepositoryModel.PROP_EXTENSION_TYPE, XWikiRepositoryModel.PROP_EXTENSION_NAME};

    public static final String[] EPROPERTIES_EXTRA = new String[] {XWikiRepositoryModel.PROP_EXTENSION_SUMMARY,
    XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION, XWikiRepositoryModel.PROP_EXTENSION_WEBSITE,
    XWikiRepositoryModel.PROP_EXTENSION_AUTHORS, XWikiRepositoryModel.PROP_EXTENSION_FEATURES,
    XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME, XWikiRepositoryModel.PROP_EXTENSION_SCMURL,
    XWikiRepositoryModel.PROP_EXTENSION_SCMCONNECTION, XWikiRepositoryModel.PROP_EXTENSION_SCMDEVCONNECTION};

    protected static final String DEFAULT_BOOST;

    protected static final String DEFAULT_FL;

    protected static Map<String, Integer> EPROPERTIES_INDEX = new HashMap<String, Integer>();

    protected static String SELECT_EXTENSIONSUMMARY;

    protected static String SELECT_EXTENSION;

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

        // Extension extra
        for (int i = 0; i < EPROPERTIES_EXTRA.length; ++i, ++j) {
            pattern.append(", extension.");
            pattern.append(EPROPERTIES_EXTRA[i]);
            EPROPERTIES_INDEX.put(EPROPERTIES_EXTRA[i], j);
        }

        SELECT_EXTENSION = pattern.toString();

        // Solr

        StringBuilder boostBuilder = new StringBuilder();
        StringBuilder flBuilder = new StringBuilder("wiki,space,name");
        for (SolrField field : XWikiRepositoryModel.SOLR_FIELDS.values()) {
            // Boost
            if (field.boost != null) {
                if (boostBuilder.length() > 0) {
                    boostBuilder.append(' ');
                }
                boostBuilder.append(field.name);
                boostBuilder.append('^');
                boostBuilder.append(field.boost);
            }

            // Fields list
            if (flBuilder.length() > 0) {
                flBuilder.append(',');
            }
            flBuilder.append(field.name);
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

    protected Query createExtensionsQuery(String from, String where, int offset, int number) throws QueryException
    {
        // select

        String select = SELECT_EXTENSION;

        // TODO: add support for real lists: need a HQL or JPQL equivalent to MySQL GROUP_CONCAT
        // * dependencies

        // Link to last version object

        if (where != null) {
            where = "(" + where + ") and ";
        } else {
            where = "";
        }
        where +=
            "extensionVersion." + XWikiRepositoryModel.PROP_VERSION_VERSION + " = extension."
                + XWikiRepositoryModel.PROP_EXTENSION_LASTVERSION;

        return createExtensionsQuery(select, from, where, offset, number, true);
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

    protected BaseObject getExtensionVersionObject(String extensionId, String version) throws XWikiException,
        QueryException
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
        if (version == null) {
            extension = this.extensionObjectFactory.createExtension();
            extensionVersion = null;
        } else {
            BaseObject extensionVersionObject = getExtensionVersionObject(extensionDocument, version);

            if (extensionVersionObject == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            extensionVersion = this.extensionObjectFactory.createExtensionVersion();
            extension = extensionVersion;
            extensionVersion.setVersion((String) getValue(extensionVersionObject,
                XWikiRepositoryModel.PROP_VERSION_VERSION));
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
        extension.setWebsite(StringUtils.defaultIfEmpty(
            (String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_WEBSITE),
            extensionDocument.getExternalURL("view", getXWikiContext())));

        // SCM
        ExtensionScm scm = new ExtensionScm();
        scm.setUrl((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SCMURL));
        scm.setConnection(toScmConnection((String) getValue(extensionObject,
            XWikiRepositoryModel.PROP_EXTENSION_SCMCONNECTION)));
        scm.setDeveloperConnection(toScmConnection((String) getValue(extensionObject,
            XWikiRepositoryModel.PROP_EXTENSION_SCMDEVCONNECTION)));
        extension.setScm(scm);

        // Authors
        List<String> authors = (List<String>) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_AUTHORS);
        if (authors != null) {
            for (String authorId : authors) {
                extension.getAuthors().add(resolveExtensionAuthor(authorId));
            }
        }

        // Features
        List<String> features = (List<String>) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_FEATURES);
        if (features != null) {
            extension.getFeatures().addAll(features);
        }

        // Properties
        List<String> properties =
            (List<String>) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_PROPERTIES);
        if (properties != null) {
            for (String stringProperty : properties) {
                int index = stringProperty.indexOf('=');
                if (index > 0) {
                    Property property = new Property();
                    property.setKey(stringProperty.substring(0, index));
                    property.setStringValue((index + 1) < stringProperty.length() ? stringProperty.substring(index + 1)
                        : "");
                    extension.getProperties().add(property);
                }
            }
        }

        // Dependencies
        if (extensionVersion != null) {
            List<BaseObject> dependencies =
                extensionDocument.getXObjects(XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSREFERENCE);
            if (dependencies != null) {
                for (BaseObject dependencyObject : dependencies) {
                    if (dependencyObject != null) {
                        if (StringUtils.equals(
                            getValue(dependencyObject, XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION,
                                (String) null), version)) {
                            ExtensionDependency dependency = extensionObjectFactory.createExtensionDependency();
                            dependency.setId((String) getValue(dependencyObject,
                                XWikiRepositoryModel.PROP_DEPENDENCY_ID));
                            dependency.setConstraint((String) getValue(dependencyObject,
                                XWikiRepositoryModel.PROP_DEPENDENCY_CONSTRAINT));

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
        Object value = document.getFieldValue(XWikiRepositoryModel.toSolrField(property));

        if (value instanceof Collection) {
            Collection collectionValue = (Collection) value;
            value = collectionValue.size() > 0 ? collectionValue.iterator().next() : null;
        }

        if (emptyIsNull && value instanceof String && ((String) value).isEmpty()) {
            value = null;
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
        extension.setDescription(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION));

        // SCM
        ExtensionScm scm = new ExtensionScm();
        scm.setUrl(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_SCMURL));
        scm.setConnection(toScmConnection(this.<String>getQueryValue(entry,
            XWikiRepositoryModel.PROP_EXTENSION_SCMCONNECTION)));
        scm.setDeveloperConnection(toScmConnection(this.<String>getQueryValue(entry,
            XWikiRepositoryModel.PROP_EXTENSION_SCMDEVCONNECTION)));
        extension.setScm(scm);

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
            extension.setWebsite(xcontext.getWiki().getURL(
                new DocumentReference(xcontext.getWikiId(), documentSpace, documentName), "view", xcontext));
        }

        // Authors
        for (String authorId : ListClass.getListFromString(
            this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_AUTHORS), "|", false)) {
            extension.getAuthors().add(resolveExtensionAuthor(authorId));
        }

        // Features
        extension.getFeatures().addAll(
            ListClass.getListFromString(
                this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_FEATURES), "|", false));

        // License
        License license = this.extensionObjectFactory.createLicense();
        license.setName(this.<String>getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME));
        extension.getLicenses().add(license);

        // Version
        extension.setVersion((String) entry[EPROPERTIES_INDEX.size()]);

        // TODO: add support for
        // * dependencies

        return extension;
    }

    protected ExtensionVersion createExtensionVersionFromSolrDocument(SolrDocument document)
    {
        XWikiContext xcontext = getXWikiContext();

        String documentName = (String) document.getFieldValue("space");
        String documentSpace = (String) document.getFieldValue("name");

        ExtensionVersion extension = this.extensionObjectFactory.createExtensionVersion();

        extension.setId(this.<String>getSolrValue(document, Extension.FIELD_ID, true));
        extension.setType(this.<String>getSolrValue(document, Extension.FIELD_TYPE, true));
        extension.setName(this.<String>getSolrValue(document, Extension.FIELD_NAME, false));
        extension.setSummary(this.<String>getSolrValue(document, Extension.FIELD_SUMMARY, false));
        extension.setDescription(this.<String>getSolrValue(document, Extension.FIELD_DESCRIPTION, false));

        // SCM
        ExtensionScm scm = new ExtensionScm();
        scm.setUrl(this.<String>getSolrValue(document, Extension.FIELD_SCM, true));
        scm.setConnection(toScmConnection(this.<String>getSolrValue(document,
            XWikiRepositoryModel.PROP_EXTENSION_SCMCONNECTION, true)));
        scm.setDeveloperConnection(toScmConnection(this.<String>getSolrValue(document,
            XWikiRepositoryModel.PROP_EXTENSION_SCMDEVCONNECTION, true)));
        if (scm.getUrl() != null || scm.getConnection() != null || scm.getDeveloperConnection() != null) {
            extension.setScm(scm);
        }

        // Rating
        // FIXME: this adds potentially tons of new DB requests to what used to be carefully crafted to produce a single
        // request for the whole search... Should be cached in a field of the document (like the last version is for
        // example).
        DocumentReference extensionDocumentReference =
            new DocumentReference(xcontext.getWikiId(), documentSpace, documentName);
        extension.setRating(getExtensionRating(extensionDocumentReference));

        // Website
        extension.setWebsite(this.<String>getSolrValue(document, Extension.FIELD_WEBSITE, true));
        if (extension.getWebsite() == null) {
            extension.setWebsite(xcontext.getWiki().getURL(
                new DocumentReference(xcontext.getWikiId(), documentSpace, documentName), "view", xcontext));
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
        if (features != null) {
            extension.getFeatures().addAll(features);
        }

        // License
        String licenseName = this.<String>getSolrValue(document, Extension.FIELD_LICENSE, true);
        if (licenseName != null) {
            License license = this.extensionObjectFactory.createLicense();
            license.setName(licenseName);
            extension.getLicenses().add(license);
        }

        // Version
        extension.setVersion(this.<String>getSolrValue(document, Extension.FIELD_VERSION, true));

        // TODO: add support for
        // * dependencies

        return extension;
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
