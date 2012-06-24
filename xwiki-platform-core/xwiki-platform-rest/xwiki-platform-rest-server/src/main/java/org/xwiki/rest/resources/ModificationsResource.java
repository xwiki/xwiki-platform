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
package org.xwiki.rest.resources;

import java.sql.Timestamp;
import java.util.Date;
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
import org.xwiki.rest.model.jaxb.History;
import org.xwiki.rest.model.jaxb.HistorySummary;

import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.resources.ModificationsResource")
@Path("/wikis/{wikiName}/modifications")
public class ModificationsResource extends XWikiResource
{
    @GET
    public History getModifications(@PathParam("wikiName") String wikiName,
        @QueryParam("start") @DefaultValue("0") Integer start,
        @QueryParam("number") @DefaultValue("25") Integer number,
        @QueryParam("order") @DefaultValue("desc") String order, @QueryParam("date") @DefaultValue("0") Long ts)
        throws QueryException
    {
        String database = Utils.getXWikiContext(componentManager).getDatabase();

        History history = new History();

        /* This try is just needed for executing the finally clause. Exceptions are actually re-thrown. */
        try {
            Utils.getXWikiContext(componentManager).setDatabase(wikiName);

            String query =
                String
                    .format(
                        "select doc.space, doc.name, doc.language, rcs.id, rcs.date, rcs.author from XWikiRCSNodeInfo as rcs, XWikiDocument as doc where rcs.id.docId=doc.id and rcs.date > :date order by rcs.date %s, rcs.id.version1 %s, rcs.id.version2 %s",
                        order, order, order);

            List<Object> queryResult = null;
            queryResult =
                queryManager.createQuery(query, Query.XWQL).bindValue("date", new Date(ts)).setLimit(number).setOffset(
                    start).execute();

            for (Object object : queryResult) {
                Object[] fields = (Object[]) object;

                String spaceName = (String) fields[0];
                String pageName = (String) fields[1];
                String language = (String) fields[2];
                if (language.equals("")) {
                    language = null;
                }
                XWikiRCSNodeId nodeId = (XWikiRCSNodeId) fields[3];
                Timestamp timestamp = (Timestamp) fields[4];
                Date modified = new Date(timestamp.getTime());
                String modifier = (String) fields[5];

                HistorySummary historySummary =
                    DomainObjectFactory.createHistorySummary(objectFactory, uriInfo.getBaseUri(), wikiName, spaceName,
                        pageName, language, nodeId.getVersion(), modifier, modified);

                history.getHistorySummaries().add(historySummary);
            }
        } finally {
            Utils.getXWikiContext(componentManager).setDatabase(database);
        }

        return history;
    }
}
