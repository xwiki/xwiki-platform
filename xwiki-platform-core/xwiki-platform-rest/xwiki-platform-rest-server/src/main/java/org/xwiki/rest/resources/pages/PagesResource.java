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
import java.util.regex.Pattern;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Pages;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.resources.pages.PagesResource")
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages")
public class PagesResource extends XWikiResource
{
    @GET
    public Pages getPages(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @QueryParam("start") @DefaultValue("0") Integer start,
        @QueryParam("number") @DefaultValue("-1") Integer number, @QueryParam("parentId") String parentFilterExpression)
        throws XWikiException, QueryException, ComponentLookupException
    {
        String database = Utils.getXWikiContext(componentManager).getDatabase();

        Pages pages = objectFactory.createPages();

        /* This try is just needed for executing the finally clause. Exceptions are actually re-thrown. */
        try {
            Utils.getXWikiContext(componentManager).setDatabase(wikiName);

            /* Use an explicit query to improve performance */
            List<String> pageNames =
                queryManager.getNamedQuery("getSpaceDocsName")
                    .addFilter(componentManager.<QueryFilter>getInstance(QueryFilter.class, "hidden"))
                    .bindValue("space", spaceName).setOffset(start).setLimit(number).execute();

            Pattern parentFilter = null;
            if (parentFilterExpression != null) {
                if (parentFilterExpression.equals("null")) {
                    parentFilter = Pattern.compile("");
                } else {
                    parentFilter = Pattern.compile(parentFilterExpression);
                }
            }

            for (String pageName : pageNames) {
                String pageFullName = Utils.getPageId(wikiName, spaceName, pageName);

                if (!Utils.getXWikiApi(componentManager).exists(pageFullName)) {
                    logger.warning(String
                        .format("[Page '%s' appears to be in space '%s' but no information is available.]", pageName,
                            spaceName));
                } else {
                    Document doc = Utils.getXWikiApi(componentManager).getDocument(pageFullName);

                    /* We only add pages we have the right to access */
                    if (doc != null) {
                        boolean add = true;

                        Document parent = Utils.getParentDocument(doc, Utils.getXWikiApi(componentManager));

                        if (parentFilter != null) {
                            String parentId = "";
                            if (parent != null && !parent.isNew()) {
                                parentId = parent.getPrefixedFullName();
                            }
                            add = parentFilter.matcher(parentId).matches();
                        }

                        if (add) {
                            pages.getPageSummaries().add(
                                DomainObjectFactory.createPageSummary(objectFactory, uriInfo.getBaseUri(), doc, Utils
                                    .getXWikiApi(componentManager)));
                        }
                    }
                }
            }
        } finally {
            Utils.getXWikiContext(componentManager).setDatabase(database);
        }

        return pages;
    }
}
