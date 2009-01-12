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
package org.xwiki.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.ext.wadl.WadlResource;
import org.restlet.resource.Variant;
import org.xwiki.rest.representers.NullRepresenter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public class XWikiResource extends WadlResource
{
    /* This is injected by the component manager and contains all the registered representers */
    private Map<String, XWikiResourceRepresenter> descriptorToRepresenterMap;

    /*
     * This is injected by the component manager. Since resources are dynamically discovered and added, and since
     * Restlet doesn't provide a way for retrieving the URI template associated to a resource class, we used this
     * additional registry in order to keep track of it. This registry is necessary in order to discover which URI
     * template is associated to a resource when making links in representations.
     */
    protected XWikiResourceClassRegistry resourceClassRegistry;

    /* This map contains only the the representers for the current resource, indexed by media type */
    private Map<MediaType, XWikiResourceRepresenter> mediaTypeToRepresenterMap;

    /* This is configured in components.xml */
    private String uriPattern;

    protected XWikiContext xwikiContext;

    protected com.xpn.xwiki.XWiki xwiki;

    protected com.xpn.xwiki.api.XWiki xwikiApi;

    /**
     * A wrapper class for returning an XWiki document enriched with information about its status.
     */
    protected static class DocumentInfo
    {
        private Document document;

        private boolean created;

        public DocumentInfo(Document document, boolean created)
        {
            this.document = document;
            this.created = created;
        }

        public Document getDocument()
        {
            return document;
        }

        public boolean isCreated()
        {
            return created;
        }

    }

    @Override
    public void init(Context context, Request request, Response response)
    {
        super.init(context, request, response);

        mediaTypeToRepresenterMap = new HashMap<MediaType, XWikiResourceRepresenter>();
        getVariants().clear();
        for (String descriptor : descriptorToRepresenterMap.keySet()) {
            XWikiResourceRepresenter representer = descriptorToRepresenterMap.get(descriptor);

            if (descriptor.startsWith(this.getClass().getName())) {
                getLogger().log(
                    Level.INFO,
                    String.format("Registering '%s' representer for resource %s", representer.getMediaType(), this
                        .getClass().getName()));
                getVariants().add(new Variant(representer.getMediaType()));
                mediaTypeToRepresenterMap.put(representer.getMediaType(), representer);

                /*
                 * This is a style check in order to see if the media type declared in the descriptor is equal to the
                 * one exposed by the presenter. This check just issue a warning and can help the developer to maintain
                 * components.xml consistent with the implementation.
                 */
                if (descriptor.indexOf(':') != -1) {
                    String[] fields = descriptor.split(":", 2);
                    if (!fields[1].equals(representer.getMediaType().getName())) {
                        getLogger()
                            .log(
                                Level.WARNING,
                                String
                                    .format(
                                        "Representer %s exposes the '%s' media type but is declared with the '%s' media type. Please update components.xml in order to make it consistent with respect to the implementation.",
                                        representer.getClass().getName(), representer.getMediaType(), fields[1]));
                    }
                } else {
                    getLogger()
                        .log(
                            Level.WARNING,
                            String
                                .format(
                                    "Representer %s exposes the '%s' media type but doesn't declare any media type. Please update components.xml in order to make it consistent with respect to the implementation.",
                                    representer.getClass().getName(), representer.getMediaType()));
                }

            }
        }

        /*
         * Put a reference to the resource in the request attributes so that the cleanup filter will be able to retrieve
         * and release it
         */
        getLogger().log(Level.INFO, String.format("Instantiated and initialized resource %s", this));
        request.getAttributes().put(Constants.RESOURCE_COMPONENT, this);

        /* Initialize relevant XWiki variables */
        xwikiContext = (XWikiContext) context.getAttributes().get(Constants.XWIKI_CONTEXT);
        xwiki = (com.xpn.xwiki.XWiki) context.getAttributes().get(Constants.XWIKI);
        xwikiApi = (com.xpn.xwiki.api.XWiki) context.getAttributes().get(Constants.XWIKI_API);
    }

    protected XWikiResourceRepresenter getRepresenterFor(Variant variant)
    {
        XWikiResourceRepresenter representer = mediaTypeToRepresenterMap.get(variant.getMediaType());

        if (representer != null) {
            return representer;
        } else {
            /* Flow control should never reach this point. */
            getLogger()
                .log(
                    Level.WARNING,
                    String
                        .format(
                            "'%s' representer for resource %s doesn't exist. This should never happen. Returning a null representer.",
                            variant.getMediaType(), this.getClass().getName()));
            return new NullRepresenter();
        }
    }

    public String getUriPattern()
    {
        return uriPattern;
    }

    /**
     * <p>
     * Returns a document based on the request. A DocumentInfo object is returned in order to provide additional
     * information.
     * </p>
     * <p>
     * If failIfDoesntExist is true, null is returned whenever the requested document does not exist
     * </p>
     * <p>
     * If failIfDoesntExist is false then a DocumentInfo is returned. However the document field could be null if the
     * user doesn't have the rights to access the document
     * </p>
     * 
     * @param failIfDoesntExist
     * @return A DocumentInfo containing the document or null if the document doesn't exist or an exception was thrown.
     */
    public DocumentInfo getDocumentFromRequest(Request request, boolean failIfDoesntExist)
    {
        try {
            String wikiName = (String) request.getAttributes().get(Constants.WIKI_NAME_PARAMETER);
            String spaceName = (String) request.getAttributes().get(Constants.SPACE_NAME_PARAMETER);
            String pageName = (String) request.getAttributes().get(Constants.PAGE_NAME_PARAMETER);
            String language = (String) request.getAttributes().get(Constants.LANGUAGE_ID_PARAMETER);
            String version = (String) request.getAttributes().get(Constants.VERSION_PARAMETER);

            String pageFullName = Utils.getPrefixedPageName(wikiName, spaceName, pageName);

            boolean existed = xwikiApi.exists(pageFullName);

            if (failIfDoesntExist) {
                if (!existed) {
                    return null;
                }
            }

            Document doc = xwikiApi.getDocument(pageFullName);

            /* If doc is null, we don't have the rights to access the document */
            if (doc == null) {
                return new DocumentInfo(null, false);
            }

            if (language != null) {
                doc = doc.getTranslatedDocument(language);

                /*
                 * If the language of the translated document is not the one we requested, then the requested
                 * translation doesn't exist. new translated document by hand.
                 */
                if (!language.equals(doc.getLanguage())) {
                    /* If we are here the requested translation doesn't exist */
                    if (failIfDoesntExist) {
                        return null;
                    } else {
                        XWikiDocument xwikiDocument = new XWikiDocument(spaceName, pageName);
                        xwikiDocument.setLanguage(language);
                        doc = new Document(xwikiDocument, xwikiContext);

                        existed = false;
                    }
                }
            }

            /* Get a specific version if requested to */
            if (version != null) {
                doc = doc.getDocumentRevision(version);
            }

            return new DocumentInfo(doc, !existed);
        } catch (Exception e) {
            return null;
        }
    }

}
