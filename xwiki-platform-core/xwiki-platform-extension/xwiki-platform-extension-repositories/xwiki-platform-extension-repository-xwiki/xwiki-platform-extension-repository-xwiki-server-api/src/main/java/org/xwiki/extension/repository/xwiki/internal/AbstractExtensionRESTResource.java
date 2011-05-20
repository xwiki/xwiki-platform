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

package org.xwiki.extension.repository.xwiki.internal;

import java.util.List;
import java.util.Vector;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.extension.repository.xwiki.model.jaxb.Extension;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionDependency;
import org.xwiki.extension.repository.xwiki.model.jaxb.ObjectFactory;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Property;

/**
 * Base class for the annotation REST services, to implement common functionality to all annotation REST services.
 * 
 * @version $Id$
 * @since 3.1M2
 */
public abstract class AbstractExtensionRESTResource extends XWikiResource implements Initializable
{
    /**
     * The execution needed to get the annotation author from the context user.
     */
    @Inject
    protected Execution execution;

    /**
     * <p>
     * The object factory for model objects to be used when creating representations.
     * </p>
     */
    protected ObjectFactory objectFactory;

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        this.objectFactory = new ObjectFactory();
    }

    protected Document getExtensionDocument(String extensionId) throws XWikiException, QueryException
    {
        String query = "from doc.object(XWiki.ExtensionClass) as extension where extension.id = :extensionId";

        List<String> documentNames =
            this.queryManager.createQuery(query, Query.XWQL).bindValue("extensionId", extensionId).execute();

        if (documentNames.isEmpty()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return Utils.getXWikiApi(this.componentManager).getDocument(documentNames.get(0));
    }

    protected com.xpn.xwiki.api.Object getExtensionObject(Document extensionDocument)
    {
        return extensionDocument.getObject("XWiki.ExtensionClass");
    }

    protected com.xpn.xwiki.api.Object getExtensionObject(String extensionId, String extensionVersion)
        throws XWikiException, QueryException
    {
        return getExtensionObject(getExtensionDocument(extensionId));
    }

    protected com.xpn.xwiki.api.Object getExtensionVersionObject(Document extensionDocument, String version)
    {
        if (version == null) {
            Vector<com.xpn.xwiki.api.Object> objects = extensionDocument.getObjects("XWiki.ExtensionVersionClass");

            if (objects.isEmpty()) {
                return null;
            } else {
                return objects.lastElement();
            }
        }

        return extensionDocument.getObject("XWiki.ExtensionVersionClass", "version", version, false);
    }

    protected com.xpn.xwiki.api.Object getExtensionVersionObject(String extensionId, String version)
        throws XWikiException, QueryException
    {
        return getExtensionVersionObject(getExtensionDocument(extensionId), version);
    }

    protected Extension createExtension(Document extensionDocument, String version)
    {
        com.xpn.xwiki.api.Object extensionObject = getExtensionObject(extensionDocument);

        if (extensionObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        com.xpn.xwiki.api.Object extensionVersionObject = getExtensionVersionObject(extensionDocument, version);

        if (extensionVersionObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        Extension extension = this.objectFactory.createExtension();
        extension.setId((String) getValue(extensionObject, "id"));
        extension.setVersion((String) getValue(extensionVersionObject, "version"));
        extension.setType((String) getValue(extensionObject, "type"));

        extension.getAuthors().addAll((List<String>) getValue(extensionObject, "authors"));
        extension.setDescription((String) getValue(extensionObject, "description"));
        extension.setName((String) getValue(extensionObject, "name"));
        extension.setWebsite((String) getValue(extensionObject, "website"));

        for (com.xpn.xwiki.api.Object dependencyObject : extensionDocument.getObjects("XWiki.ExtensionDependencyClass",
            "extensionversion", version)) {
            ExtensionDependency dependency = new ExtensionDependency();
            dependency.setId((String) getValue(dependencyObject, "id"));
            dependency.setVersion((String) getValue(dependencyObject, "version"));

            extension.getDependencies().add(dependency);
        }

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
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }
}
