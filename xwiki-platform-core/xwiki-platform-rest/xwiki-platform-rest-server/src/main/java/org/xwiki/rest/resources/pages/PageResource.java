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
package org.xwiki.rest.resources.pages;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.HttpStatus;
import org.xwiki.component.annotation.Component;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Utils;
import org.xwiki.rest.model.jaxb.Page;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.resources.pages.PageResource")
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}")
public class PageResource extends ModifiablePageResource
{
    @GET
    public Page getPage(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        return DomainObjectFactory.createPage(objectFactory, uriInfo.getBaseUri(), uriInfo.getAbsolutePath(), doc,
            false, Utils.getXWikiApi(componentManager));
    }

    @PUT
    public Response putPage(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, Page page) throws XWikiException
    {
        if (uriInfo.getQueryParameters().containsKey("copyfrom")
            || uriInfo.getQueryParameters().containsKey("movefrom")) {
            String sourcePageId = null;
            if (uriInfo.getQueryParameters().containsKey("copyfrom")) {
                sourcePageId = uriInfo.getQueryParameters().getFirst("copyfrom");
            }

            if (uriInfo.getQueryParameters().containsKey("movefrom")) {
                sourcePageId = uriInfo.getQueryParameters().getFirst("movefrom");
            }

            if (sourcePageId != null) {
                Document sourceDoc = Utils.getXWikiApi(componentManager).getDocument(sourcePageId);

                DocumentInfo targetDocumentInfo =
                    getDocumentInfo(wikiName, spaceName, pageName, null, null, false, true);

                boolean existed =
                    Utils.getXWikiApi(componentManager).exists(targetDocumentInfo.getDocument().getFullName());
                if (existed) {
                    return Response.status(HttpStatus.SC_CONFLICT).build();
                }

                if (uriInfo.getQueryParameters().containsKey("movefrom")) {
                    // invoke XWikiDocument.rename(newDocumentReference, context)
                    XWikiDocument targetDoc = targetDocumentInfo.getDocument().getDocument();

                    sourceDoc.rename(targetDoc.getDocumentReference());

                    page =
                        DomainObjectFactory.createPage(objectFactory, uriInfo.getBaseUri(), uriInfo.getAbsolutePath(),
                            sourceDoc, false, Utils.getXWikiApi(componentManager));

                    if (targetDocumentInfo.isCreated()) {
                        return Response.created(uriInfo.getAbsolutePath()).entity(page).build();
                    }
                }

                if (uriInfo.getQueryParameters().containsKey("movefrom")) {
                    // invoke XWikiDocument.copyDocument(targetDocumentReference, context)
                    XWikiDocument targetDoc =
                        sourceDoc.getDocument().copyDocument(targetDocumentInfo.getDocument().getDocumentReference(),
                            Utils.getXWikiContext(componentManager));
                    Document doc = targetDoc.newDocument(Utils.getXWikiContext(componentManager));
                    doc.save();

                    page =
                        DomainObjectFactory.createPage(objectFactory, uriInfo.getBaseUri(), uriInfo.getAbsolutePath(),
                            doc, false, Utils.getXWikiApi(componentManager));

                    if (targetDocumentInfo.isCreated()) {
                        return Response.created(uriInfo.getAbsolutePath()).entity(page).build();
                    }
                }
            }
        }

        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, false, true);

        return putPage(documentInfo, page);
    }

    @DELETE
    public void deletePage(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, false, true);

        deletePage(documentInfo);
    }
}
