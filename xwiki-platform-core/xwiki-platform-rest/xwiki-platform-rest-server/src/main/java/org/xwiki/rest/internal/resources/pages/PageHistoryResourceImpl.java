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

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.History;
import org.xwiki.rest.model.jaxb.HistorySummary;
import org.xwiki.rest.resources.pages.PageHistoryResource;

import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.pages.PageHistoryResourceImpl")
public class PageHistoryResourceImpl extends XWikiResource implements PageHistoryResource
{
    @Override
    public History getPageHistory(String wikiName, String spaceName, String pageName, Integer start, Integer number,
            String order, Boolean withPrettyNames) throws XWikiRestException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();

        History history = new History();

        try {
            Utils.getXWikiContext(componentManager).setWikiId(wikiName);

            // Note that the query is made to work with Oracle which treats empty strings as null.
            String query = String.format("select doc.space, doc.name, rcs.id, rcs.date, rcs.author, rcs.comment"
                + " from XWikiRCSNodeInfo as rcs, XWikiDocument as doc where rcs.id.docId = doc.id and"
                + " doc.space = :space and doc.name = :name and (doc.language = '' or doc.language is null)"
                + " order by rcs.date %s, rcs.id.version1 %s, rcs.id.version2 %s", order, order, order);

            List<Object> queryResult = null;
            queryResult = queryManager.createQuery(query, Query.XWQL).bindValue("space", spaceName).bindValue("name",
                    pageName).setLimit(number).setOffset(start).execute();

            for (Object object : queryResult) {
                Object[] fields = (Object[]) object;

                XWikiRCSNodeId nodeId = (XWikiRCSNodeId) fields[2];
                Timestamp timestamp = (Timestamp) fields[3];
                Date modified = new Date(timestamp.getTime());
                String modifier = (String) fields[4];
                String comment = (String) fields[5];

                HistorySummary historySummary = DomainObjectFactory.createHistorySummary(objectFactory,
                        uriInfo.getBaseUri(), wikiName, spaceName, pageName, null, nodeId.getVersion(), modifier,
                        modified, comment, Utils.getXWikiApi(componentManager), withPrettyNames);

                history.getHistorySummaries().add(historySummary);
            }
        } catch (QueryException e) {
            throw new XWikiRestException(e);
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }

        return history;
    }
}
