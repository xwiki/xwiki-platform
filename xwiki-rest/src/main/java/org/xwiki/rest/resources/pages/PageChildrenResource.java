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

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Pages;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.resources.pages.PageChildrenResource")
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/children")
public class PageChildrenResource extends XWikiResource
{
    @GET
    public Pages getPageChildren(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @QueryParam("start") @DefaultValue("0") Integer start,
        @QueryParam("number") @DefaultValue("-1") Integer number) throws XWikiException, QueryException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        Pages pages = objectFactory.createPages();

        /* Use an explicit query to improve performance */
        String queryString =
            "select distinct doc.fullName from XWikiDocument as doc where doc.parent = :parent order by doc.fullName asc";
        List<String> childPageFullNames =
            queryManager.createQuery(queryString, Query.XWQL).bindValue("parent", doc.getFullName()).setOffset(start)
                .setLimit(number).execute();

        for (String childPageFullName : childPageFullNames) {
            String pageId = Utils.getPageId(wikiName, childPageFullName);

            if (!xwikiApi.exists(pageId)) {
                logger.warning(String.format(
                    "[Page '%s' appears to be in space '%s' but no information is available.]", pageName, spaceName));
            } else {
                Document childDoc = xwikiApi.getDocument(pageId);

                /* We only add pages we have the right to access */
                if (childDoc != null) {
                    pages.getPageSummaries().add(
                        DomainObjectFactory.createPageSummary(objectFactory, uriInfo.getBaseUri(), childDoc, xwikiApi));
                }
            }
        }

        return pages;
    }

}
