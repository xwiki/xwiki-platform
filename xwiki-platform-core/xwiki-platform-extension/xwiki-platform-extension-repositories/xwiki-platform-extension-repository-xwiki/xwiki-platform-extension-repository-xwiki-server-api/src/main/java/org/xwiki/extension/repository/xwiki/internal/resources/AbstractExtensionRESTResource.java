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

package org.xwiki.extension.repository.xwiki.internal.resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.extension.repository.xwiki.internal.RepositoryManager;
import org.xwiki.extension.repository.xwiki.internal.XWikiRepositoryModel;
import org.xwiki.extension.repository.xwiki.model.jaxb.AbstractExtension;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionAuthor;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionDependency;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionSummary;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersion;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersionSummary;
import org.xwiki.extension.repository.xwiki.model.jaxb.License;
import org.xwiki.extension.repository.xwiki.model.jaxb.ObjectFactory;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rest.XWikiResource;

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
    public final static String[] EPROPERTIES_SUMMARY = new String[] {XWikiRepositoryModel.PROP_EXTENSION_ID,
    XWikiRepositoryModel.PROP_EXTENSION_TYPE, XWikiRepositoryModel.PROP_EXTENSION_NAME};

    public final static String[] EPROPERTIES_EXTRA = new String[] {XWikiRepositoryModel.PROP_EXTENSION_SUMMARY,
    XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION, XWikiRepositoryModel.PROP_EXTENSION_WEBSITE,
    XWikiRepositoryModel.PROP_EXTENSION_AUTHORS, XWikiRepositoryModel.PROP_EXTENSION_FEATURES,
    XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME};

    private static Map<String, Integer> EPROPERTIES_INDEX = new HashMap<String, Integer>();

    private static String SELECT_EXTENSIONSUMMARY;

    private static String SELECT_EXTENSION;

    {
        {
            StringBuilder pattern = new StringBuilder();

            int j = 0;

            // Extension summary
            for (int i = 0; i < EPROPERTIES_SUMMARY.length; ++i, ++j) {
                if (i != 0) {
                    pattern.append(", ");
                }
                pattern.append("extension.");
                pattern.append(EPROPERTIES_SUMMARY[i]);
                EPROPERTIES_INDEX.put(EPROPERTIES_SUMMARY[i], j);
            }

            SELECT_EXTENSIONSUMMARY = pattern.toString();

            // Extension extra
            for (int i = 0; i < EPROPERTIES_EXTRA.length; ++i, ++j) {
                pattern.append(", extension.");
                pattern.append(EPROPERTIES_EXTRA[i]);
                EPROPERTIES_INDEX.put(EPROPERTIES_EXTRA[i], j);
            }

            SELECT_EXTENSION = pattern.toString();
        }
    }

    /**
     * The execution needed to get the annotation author from the context user.
     */
    @Inject
    protected Execution execution;

    @Inject
    protected RepositoryManager repositoryManager;

    /**
     * The object factory for model objects to be used when creating representations.
     */
    protected ObjectFactory objectFactory;

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        this.objectFactory = new ObjectFactory();
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
        return extensionDocument.getObject(XWikiRepositoryModel.EXTENSION_CLASSNAME);
    }

    protected BaseObject getExtensionObject(String extensionId) throws XWikiException, QueryException
    {
        return getExtensionObject(getExistingExtensionDocumentById(extensionId));
    }

    protected BaseObject getExtensionVersionObject(XWikiDocument extensionDocument, String version)
    {
        if (version == null) {
            Vector<BaseObject> objects = extensionDocument.getObjects(XWikiRepositoryModel.EXTENSIONVERSION_CLASSNAME);

            if (objects.isEmpty()) {
                return null;
            } else {
                return objects.lastElement();
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

        if (extensionObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        AbstractExtension extension;
        ExtensionVersion extensionVersion;
        if (version == null) {
            extension = this.objectFactory.createExtension();
            extensionVersion = null;
        } else {
            BaseObject extensionVersionObject = getExtensionVersionObject(extensionDocument, version);

            if (extensionVersionObject == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            extension = extensionVersion = this.objectFactory.createExtensionVersion();
            extensionVersion.setVersion((String) getValue(extensionVersionObject,
                XWikiRepositoryModel.PROP_VERSION_VERSION));
        }

        extension.setId((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ID));
        extension.setType((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_TYPE));

        License license = this.objectFactory.createLicense();
        license.setName((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME));
        extension.getLicenses().add(license);

        extension.setSummary((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SUMMARY));
        extension.setDescription((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION));
        extension.setName((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_NAME));
        extension.setWebsite(StringUtils.defaultIfEmpty(
            (String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_WEBSITE),
            extensionDocument.getExternalURL("view", getXWikiContext())));

        // Authors
        for (String authorId : (List<String>) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_AUTHORS)) {
            extension.getAuthors().add(resolveExtensionAuthor(authorId));
        }

        // Features
        extension.getFeatures().addAll(
            (List<String>) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_FEATURES));

        // Dependencies
        if (extensionVersion != null) {
            Vector<BaseObject> dependencies =
                extensionDocument.getObjects(XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSNAME);
            if (dependencies != null) {
                for (BaseObject dependencyObject : dependencies) {
                    if (dependencyObject != null) {
                        if (StringUtils.equals(
                            getValue(dependencyObject, XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION,
                                (String) null), version)) {
                            ExtensionDependency dependency = objectFactory.createExtensionDependency();
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
            author.setName(xcontext.getWiki().getUserName(authorId, null, false, xcontext));
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

    private <T> T getQueryValue(Object[] entry, String property)
    {
        return (T) entry[EPROPERTIES_INDEX.get(property)];
    }

    private ExtensionVersion createExtensionVersionFromQueryResult(Object[] entry)
    {
        ExtensionVersion extension = this.objectFactory.createExtensionVersion();

        extension.setId(this.<String> getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_ID));
        extension.setType(this.<String> getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_TYPE));
        extension.setName(this.<String> getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_NAME));
        extension.setSummary(this.<String> getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_SUMMARY));
        extension.setDescription(this.<String> getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION));

        // TODO: if no website is provided return the page external URL but should absolutely be done without loading
        // the XWikiDocument
        extension.setWebsite(this.<String> getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_WEBSITE));

        // Authors
        for (String authorId : ListClass.getListFromString(
            this.<String> getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_AUTHORS), "|", false)) {
            extension.getAuthors().add(resolveExtensionAuthor(authorId));
        }

        // Features
        extension.getFeatures().addAll(
            ListClass.getListFromString(
                this.<String> getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_FEATURES), "|", false));

        // License
        License license = this.objectFactory.createLicense();
        license.setName(this.<String> getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME));
        extension.getLicenses().add(license);

        // Version
        extension.setVersion((String) entry[EPROPERTIES_INDEX.size()]);

        // TODO: add support for
        // * dependencies

        return extension;
    }

    protected <E extends ExtensionSummary> void getExtensionSummaries(List<E> extensions, Query query)
        throws QueryException
    {
        List<Object[]> entries = query.execute();

        for (Object[] entry : entries) {
            extensions.add((E) createExtensionSummaryFromQueryResult(entry));
        }
    }

    private ExtensionSummary createExtensionSummaryFromQueryResult(Object[] entry)
    {
        ExtensionSummary extension;
        ExtensionVersionSummary extensionVersion;
        if (entry.length == EPROPERTIES_SUMMARY.length) {
            // It's a extension summary without version
            extension = this.objectFactory.createExtensionSummary();
            extensionVersion = null;
        } else {
            extension = extensionVersion = this.objectFactory.createExtensionVersionSummary();
            extensionVersion.setVersion((String) entry[EPROPERTIES_SUMMARY.length]);
        }

        extension.setId(this.<String> getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_ID));
        extension.setType(this.<String> getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_TYPE));
        extension.setName(this.<String> getQueryValue(entry, XWikiRepositoryModel.PROP_EXTENSION_NAME));

        return extension;
    }

    protected <T> T getValue(BaseObject object, String field)
    {
        return getValue(object, field, (T) null);
    }

    protected <T> T getValue(BaseObject object, String field, T def)
    {
        BaseProperty< ? > property = (BaseProperty< ? >) object.safeget(field);

        return property != null ? (T) property.getValue() : def;
    }

    @Override
    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
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
        XWikiContext xcontext = getXWikiContext();
        if (!xcontext.getWiki().getRightService()
            .hasAccessLevel("view", xcontext.getUser(), document.getPrefixedFullName(), xcontext)) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
    }
}
