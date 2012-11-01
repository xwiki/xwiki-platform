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
package org.xwiki.rest.internal.resources.pages;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Page;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.internal.resources.pages.PageTranslationResource")
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/translations/{language}")
public class PageTranslationResource extends ModifiablePageResource
{
    @GET
    public Page getPageTranslation(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("language") String language,
        @QueryParam("prettynames") @DefaultValue("0") Boolean withPrettyNames) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, language, null, true, false);

        Document doc = documentInfo.getDocument();

        return DomainObjectFactory.createPage(objectFactory, uriInfo.getBaseUri(), uriInfo.getAbsolutePath(), doc,
            false, Utils.getXWikiApi(componentManager), withPrettyNames);
    }

    @PUT
    public Response putPageTranslation(@PathParam("wikiName") String wikiName,
        @PathParam("spaceName") String spaceName, @PathParam("pageName") String pageName,
        @PathParam("language") String language, Page page) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, language, null, false, true);

        return putPage(documentInfo, page);
    }

    @DELETE
    public void deletePageTranslation(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("language") String language) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, language, null, true, false);

        deletePage(documentInfo);
    }

}
