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

import java.util.List;
import java.util.Vector;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.extension.repository.xwiki.internal.XWikiRepositoryModel;
import org.xwiki.extension.repository.xwiki.model.jaxb.AbstractExtension;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionDependency;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionSummary;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersion;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersionSummary;
import org.xwiki.extension.repository.xwiki.model.jaxb.Extensions;
import org.xwiki.extension.repository.xwiki.model.jaxb.License;
import org.xwiki.extension.repository.xwiki.model.jaxb.ObjectFactory;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Property;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * Base class for the annotation REST services, to implement common functionality to all annotation REST services.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public abstract class AbstractExtensionRESTResource extends XWikiResource implements Initializable
{
    /**
     * The execution needed to get the annotation author from the context user.
     */
    @Inject
    protected Execution execution;

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

    protected Query createExtensionsQuery(String from, String where, int offset, int number) throws QueryException
    {
        // select

        String select =
            "extension.id, extension.type, extension.name"
                + ", extension.summary, extension.description, extension.website, extension.authors, extension.features"
                + ", extensionVersion.version";

        if (where != null) {
            where += " and extensionVersion.version = extension.lastVersion";
        } else {
            where = "extensionVersion.version = extension.lastVersion";
        }

        // TODO: add support for lists: need a HQL or JPQL equivalent to MySQL GROUP_CONCAT
        // solution yet
        // * dependencies

        return createExtensionsQuery(select, from, where, offset, number, true);
    }

    protected Query createExtensionsSummariesQuery(String from, String where, int offset, int number, boolean versions)
        throws QueryException
    {
        String select = "extension.id, extension.type, extension.name";

        return createExtensionsQuery(select, from, where, offset, number, versions);
    }

    private Query createExtensionsQuery(String select, String from, String where, int offset, int number,
        boolean versions) throws QueryException
    {
        // select

        StringBuilder queryStr = new StringBuilder("select ");
        queryStr.append(select);

        if (versions) {
            queryStr.append(", extensionVersion.version");
        }

        // from

        queryStr
            .append(" from Document doc, doc.object(" + XWikiRepositoryModel.EXTENSION_CLASSNAME + ") as extension");

        if (versions) {
            queryStr
                .append(", doc.object(" + XWikiRepositoryModel.EXTENSIONVERSION_CLASSNAME + ") as extensionVersion");
        }

        // where

        if (from != null) {
            queryStr.append(',');
            queryStr.append(from);
        }

        queryStr.append(" where ");
        if (where != null) {
            queryStr.append(where);
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

    protected Document getExtensionDocument(String extensionId) throws XWikiException, QueryException
    {
        Query query =
            this.queryManager.createQuery("from doc.object(" + XWikiRepositoryModel.EXTENSION_CLASSNAME
                + ") as extension where extension.id = :extensionId", Query.XWQL);

        query.bindValue("extensionId", extensionId);

        List<String> documentNames = query.execute();

        if (documentNames.isEmpty()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return Utils.getXWikiApi(this.componentManager).getDocument(documentNames.get(0));
    }

    protected com.xpn.xwiki.api.Object getExtensionObject(Document extensionDocument)
    {
        return extensionDocument.getObject(XWikiRepositoryModel.EXTENSION_CLASSNAME);
    }

    protected com.xpn.xwiki.api.Object getExtensionObject(String extensionId) throws XWikiException, QueryException
    {
        return getExtensionObject(getExtensionDocument(extensionId));
    }

    protected com.xpn.xwiki.api.Object getExtensionVersionObject(Document extensionDocument, String version)
    {
        if (version == null) {
            Vector<com.xpn.xwiki.api.Object> objects =
                extensionDocument.getObjects(XWikiRepositoryModel.EXTENSIONVERSION_CLASSNAME);

            if (objects.isEmpty()) {
                return null;
            } else {
                return objects.lastElement();
            }
        }

        return extensionDocument.getObject(XWikiRepositoryModel.EXTENSIONVERSION_CLASSNAME, "version", version, false);
    }

    protected com.xpn.xwiki.api.Object getExtensionVersionObject(String extensionId, String version)
        throws XWikiException, QueryException
    {
        return getExtensionVersionObject(getExtensionDocument(extensionId), version);
    }

    protected <E extends AbstractExtension> E createExtension(Document extensionDocument, String version)
    {
        com.xpn.xwiki.api.Object extensionObject = getExtensionObject(extensionDocument);

        if (extensionObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        AbstractExtension extension;
        ExtensionVersion extensionVersion;
        if (version == null) {
            extension = this.objectFactory.createExtension();
            extensionVersion = null;
        } else {
            com.xpn.xwiki.api.Object extensionVersionObject = getExtensionVersionObject(extensionDocument, version);

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
        extension.setLicense(license);

        extension.setSummary((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SUMMARY));
        extension.setDescription((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION));
        extension.setName((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_NAME));
        extension.setWebsite((String) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_WEBSITE));

        extension.getAuthors().addAll(
            (List<String>) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_AUTHORS));
        extension.getFeatures().addAll(
            (List<String>) getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_FEATURES));

        if (extensionVersion != null) {
            for (com.xpn.xwiki.api.Object dependencyObject : extensionDocument.getObjects(
                XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSNAME,
                XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION, version)) {
                ExtensionDependency dependency = new ExtensionDependency();
                dependency.setId((String) getValue(dependencyObject, XWikiRepositoryModel.PROP_DEPENDENCY_ID));
                dependency
                    .setVersion((String) getValue(dependencyObject, XWikiRepositoryModel.PROP_DEPENDENCY_VERSION));

                extensionVersion.getDependencies().add(dependency);
            }
        }

        return (E) extension;
    }

    protected Extensions getExtensionSummaries(Query query) throws QueryException
    {
        Extensions extensions = this.objectFactory.createExtensions();

        getExtensionSummaries(extensions.getExtensionSummaries(), query);

        return extensions;
    }

    protected void getExtensions(List<ExtensionVersion> extensions, Query query) throws QueryException
    {
        List<Object[]> entries = query.execute();

        for (Object[] entry : entries) {
            extensions.add(createExtensionVersionFromQueryResult(entry));
        }
    }

    private ExtensionVersion createExtensionVersionFromQueryResult(Object[] entry)
    {
        ExtensionVersion extension = this.objectFactory.createExtensionVersion();

        extension.setId((String) entry[0]);
        extension.setType((String) entry[1]);
        extension.setName((String) entry[2]);
        extension.setVersion((String) entry[3]);
        extension.setSummary((String) entry[4]);
        extension.setDescription((String) entry[5]);
        extension.setWebsite((String) entry[6]);

        extension.getAuthors().addAll(ListClass.getListFromString((String) entry[7], "|", false));
        extension.getFeatures().addAll(ListClass.getListFromString((String) entry[8], "|", false));

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
        if (entry.length == 3) {
            extension = this.objectFactory.createExtensionSummary();
            extensionVersion = null;
        } else {
            extension = extensionVersion = this.objectFactory.createExtensionVersionSummary();
            extensionVersion.setVersion((String) entry[3]);
        }

        extension.setId((String) entry[0]);
        extension.setType((String) entry[1]);
        extension.setName((String) entry[2]);

        return extension;
    }

    protected Object getValue(com.xpn.xwiki.api.Object object, String field)
    {
        return getValue(object, field, null);
    }

    protected <T> T getValue(com.xpn.xwiki.api.Object object, String field, T def)
    {
        Property property = object.getProperty(field);

        return property != null ? (T) property.getValue() : def;
    }

    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    protected ResponseBuilder getAttachmentResponse(Attachment xwikiAttachment) throws XWikiException
    {
        if (xwikiAttachment == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        ResponseBuilder response = Response.ok();

        response.type(xwikiAttachment.getMimeType());
        response.entity(xwikiAttachment.getContent());
        response.header("Content-Disposition", "attachment; filename=\"" + xwikiAttachment.getFilename() + "\"");

        return response;
    }
}
