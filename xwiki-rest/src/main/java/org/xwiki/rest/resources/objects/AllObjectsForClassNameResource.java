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
package org.xwiki.rest.resources.objects;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.ObjectSummary;
import org.xwiki.rest.model.jaxb.Objects;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/classes/{className}/objects")
public class AllObjectsForClassNameResource extends XWikiResource
{
    @GET
    public Objects getObjects(@PathParam("wikiName") String wikiName, @PathParam("className") String className,
        @QueryParam("start") @DefaultValue("0") Integer start, @QueryParam("number") @DefaultValue("-1") Integer number)
        throws XWikiException, QueryException
    {
        String database = xwikiContext.getDatabase();

        Objects objects = new Objects();

        /* This try is just needed for executing the finally clause. Exceptions are actually re-thrown. */
        try {
            xwikiContext.setDatabase(wikiName);

            String query =
                "select doc, obj from BaseObject as obj, XWikiDocument as doc where obj.name=doc.fullName and obj.className=:className";

            QueryManager queryManager = (QueryManager) com.xpn.xwiki.web.Utils.getComponent(QueryManager.ROLE);

            List<Object> queryResult = null;
            queryResult =
                queryManager.createQuery(query, Query.XWQL).bindValue("className", className).setLimit(number)
                    .setOffset(start).execute();

            for (Object object : queryResult) {
                Object[] fields = (Object[]) object;

                XWikiDocument xwikiDocument = (XWikiDocument) fields[0];
                xwikiDocument.setDatabase(wikiName);
                Document doc = new Document(xwikiDocument, xwikiContext);
                BaseObject xwikiObject = (BaseObject) fields[1];

                ObjectSummary objectSummary =
                    DomainObjectFactory.createObjectSummary(objectFactory, uriInfo.getBaseUri(), xwikiContext, doc,
                        xwikiObject, false);

                objects.getObjectSummaries().add(objectSummary);
            }
        } finally {
            xwikiContext.setDatabase(database);
        }

        return objects;
    }

}
