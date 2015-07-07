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
package org.xwiki.rest.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.internal.NoOpQueryFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * A class containing utility methods used in the REST subsystem.
 * 
 * @version $Id$
 */
public class Utils
{
    /**
     * Get the page id given its components.
     * 
     * @param wikiName
     * @param spaceName
     * @param pageName
     * @return The page id.
     */
    public static String getPageId(String wikiName, List<String> spaceName, String pageName)
    {        
        XWikiDocument xwikiDocument = new XWikiDocument(new DocumentReference(wikiName, spaceName, pageName));

        Document document = new Document(xwikiDocument, null);

        return document.getPrefixedFullName();
    }
    
    /**
     * @param spaces the space hierarchy
     * @param wikiName the name of the wiki
     * @return the space reference
     */
    public static SpaceReference getSpaceReference(List<String> spaces, String wikiName)
    {
        EntityReference parentReference = new WikiReference(wikiName);
        SpaceReference spaceReference = null;

        for (String space : spaces) {
            spaceReference = new SpaceReference(space, parentReference);
            parentReference = spaceReference;
        }

        return spaceReference;
    }

    public static String getLocalSpaceId(List<String> spaces)
    {
        EntityReferenceSerializer<String> serializer =
                com.xpn.xwiki.web.Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        // The wiki name cannot be not empty in a space reference, but its value has no importance since the local
        // serializer does not use it
        return serializer.serialize(getSpaceReference(spaces, "whatever"));
    }

    /**
     * @param wikiName the name of the wiki that contains the space
     * @param spaces the spaces hierarchy
     * @return the space id
     * @throws org.xwiki.rest.XWikiRestException
     */
    public static String getSpaceId(String wikiName, List<String> spaces)
    {
        EntityReferenceSerializer<String> defaultEntityReferenceSerializer =
            com.xpn.xwiki.web.Utils.getComponent(EntityReferenceSerializer.TYPE_STRING);
        SpaceReference spaceReference = getSpaceReference(spaces, wikiName);
        return defaultEntityReferenceSerializer.serialize(spaceReference);
    }
    
    public static SpaceReference resolveLocalSpaceId(String spaceId, String wikiName)
    {
        EntityReferenceResolver<String> resolver =
                com.xpn.xwiki.web.Utils.getComponent(EntityReferenceResolver.TYPE_STRING);
        return new SpaceReference(resolver.resolve(spaceId, EntityType.SPACE, new WikiReference(wikiName)));
    }

    public static List<String> getSpacesFromSpaceId(String spaceId)
    {
        List<String> spaces = new ArrayList<>();
        EntityReference spaceReference =  resolveLocalSpaceId(spaceId, "whatever");
        
        while(spaceReference != null && EntityType.SPACE == spaceReference.getType()) {
            spaces.add(spaceReference.getName());
            spaceReference = spaceReference.getParent();
        }

        Collections.reverse(spaces);
        return spaces;
    }

    /**
     * Get the page full name given its components.
     * 
     * @param wikiName
     * @param spaces
     * @param pageName
     * @return The page full name.
     */
    public static String getPageFullName(String wikiName, List<String> spaces, String pageName)
    {
        XWikiDocument xwikiDocument = new XWikiDocument(new DocumentReference(wikiName, spaces, pageName));

        Document document = new Document(xwikiDocument, null);

        return document.getFullName();
    }

    /**
     * Get the object id given its components.
     * 
     * @param wikiName
     * @param spaceName
     * @param pageName
     * @param className
     * @param objectNumber
     * @return The object id.
     */
    public static String getObjectId(String wikiName, List<String> spaces, String pageName, String className,
        int objectNumber)
    {
        XWikiDocument xwikiDocument = new XWikiDocument(new DocumentReference(wikiName, spaces, pageName));

        Document document = new Document(xwikiDocument, null);

        return String.format("%s:%s[%d]", document.getPrefixedFullName(), className, objectNumber);
    }

    /**
     * Get the page id given its components.
     * 
     * @return The page id.
     */
    public static String getPageId(String wikiName, String pageFullName)
    {
        DocumentReferenceResolver<String> defaultDocumentReferenceResolver =
                com.xpn.xwiki.web.Utils.getComponent(DocumentReferenceResolver.TYPE_STRING);

        DocumentReference documentReference =
                defaultDocumentReferenceResolver.resolve(pageFullName, new WikiReference(wikiName));
        XWikiDocument xwikiDocument = new XWikiDocument(documentReference);

        Document document = new Document(xwikiDocument, null);

        return document.getPrefixedFullName();
    }

    /**
     * Get parent document for a given document.
     * 
     * @param doc document to get the parent from.
     * @param xwikiApi the xwiki api.
     * @return parent of the given document, null if none is specified.
     * @throws XWikiException if getting the parent document has failed.
     */
    public static Document getParentDocument(Document doc, com.xpn.xwiki.api.XWiki xwikiApi) throws XWikiException
    {
        if (StringUtils.isEmpty(doc.getParent())) {
            return null;
        }
        /*
         * This is ugly but we have to mimic the behavior of link generation: if the parent does not specify its space,
         * use the current document space.
         */
        String parentName = doc.getParent();
        if (!parentName.contains(".")) {
            parentName = doc.getSpace() + "." + parentName;
        }
        return xwikiApi.getDocument(parentName);
    }

    /**
     * Retrieve the XWiki context from the current execution context.
     * 
     * @param componentManager The component manager to be used to retrieve the execution context.
     * @return The XWiki context.
     * @throws RuntimeException If there was an error retrieving the context.
     */
    public static XWikiContext getXWikiContext(ComponentManager componentManager)
    {
        Execution execution;
        XWikiContext xwikiContext;
        try {
            execution = componentManager.getInstance(Execution.class);
            xwikiContext = (XWikiContext) execution.getContext().getProperty("xwikicontext");
            return xwikiContext;
        } catch (Exception e) {
            throw new RuntimeException("Unable to get XWiki context", e);
        }
    }

    /**
     * Retrieve the XWiki private API object.
     * 
     * @param componentManager The component manager to be used to retrieve the execution context.
     * @return The XWiki private API object.
     */
    public static com.xpn.xwiki.XWiki getXWiki(ComponentManager componentManager)
    {
        return getXWikiContext(componentManager).getWiki();
    }

    /**
     * Retrieve the XWiki public API object.
     * 
     * @param componentManager The component manager to be used to retrieve the execution context.
     * @return The XWiki public API object.
     * @throws RuntimeException If there was an error while initializing the XWiki public API object.
     */
    public static com.xpn.xwiki.api.XWiki getXWikiApi(ComponentManager componentManager)
    {
        return new com.xpn.xwiki.api.XWiki(getXWiki(componentManager), getXWikiContext(componentManager));
    }

    /**
     * Retrieve the XWiki user associated to the current XWiki context.
     * 
     * @param componentManager The component manager to be used to retrieve the execution context.
     * @return The user associated to the current XWiki context.
     */
    public static String getXWikiUser(ComponentManager componentManager)
    {
        XWikiUser user = getXWikiContext(componentManager).getXWikiUser();
        if (user == null) {
            return "XWiki.Guest";
        }

        return user.getUser();
    }

    /**
     * Retrieve the XWiki private API object.
     * 
     * @param componentManager The component manager to be used to retrieve the execution context.
     * @return The XWiki private API object.
     */
    public static String getAuthorName(DocumentReference authorReference, ComponentManager componentManager)
    {
        return getXWikiContext(componentManager).getWiki().getPlainUserName(authorReference,
            getXWikiContext(componentManager));
    }

    /**
     * Retrieve the BaseObject from the Document.
     * 
     * @param doc Public API document
     * @param className Classname
     * @param objectNumber Object Number
     * @return The BaseObject field
     * @throws XWikiException
     */
    public static BaseObject getBaseObject(Document doc, String className, int objectNumber,
        ComponentManager componentManager) throws XWikiException
    {
        XWikiDocument xwikiDocument =
            Utils.getXWiki(componentManager).getDocument(doc.getPrefixedFullName(),
                Utils.getXWikiContext(componentManager));

        return xwikiDocument.getObject(className, objectNumber);
    }

    /**
     * Creates an URI to access the specified resource. The given path elements are encoded before being inserted into
     * the resource path.
     * <p>
     * NOTE: We added this method because {@link UriBuilder#build(Object...)} doesn't encode all special characters. See
     * https://github.com/restlet/restlet-framework-java/issues/601 .
     * 
     * @param baseURI the base URI
     * @param resourceClass the resource class, used to get the URI path
     * @param pathElements the path elements to insert in the resource path
     * @return an URI that can be used to access the specified resource
     */
    public static URI createURI(URI baseURI, java.lang.Class< ? > resourceClass, java.lang.Object... pathElements)
    {
        Object[] encodedPathElements = new String[pathElements.length];
        for (int i = 0; i < pathElements.length; i++) {
            if (pathElements[i] != null) {
                try {
                    encodedPathElements[i] = URIUtil.encodePath(pathElements[i].toString());
                } catch (URIException e) {
                    throw new RuntimeException("Failed to encode path element: " + pathElements[i], e);
                }
            } else {
                encodedPathElements[i] = null;
            }
        }
        return UriBuilder.fromUri(baseURI).path(resourceClass).buildFromEncoded(encodedPathElements);
    }

    /**
     * @param componentManager the component manager to be used to retrieve the hidden query filter
     * @return the hidden query filter or a NoOp filter if the hidden filter isn't found
     */
    public static QueryFilter getHiddenQueryFilter(ComponentManager componentManager)
    {
        QueryFilter filter;
        try {
            filter = componentManager.getInstance(QueryFilter.class, "hidden");
        } catch (ComponentLookupException e) {
            // They hidden filter is not available at runtime, don't use it
            filter = new NoOpQueryFilter();
        }
        return filter;
    }
}
