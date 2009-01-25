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

import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.History;
import org.xwiki.rest.model.HistorySummary;

/**
 * Resource for the page history. This base resource is able to retrieve history for pages and translations.
 * 
 * @version $Id$
 */
public class BasePageHistoryResource extends XWikiResource
{
    /**
     * Get the page history for a page.
     */
    @Override
    public Representation represent(Variant variant)
    {
        String wikiName = (String) getRequest().getAttributes().get(Constants.WIKI_NAME_PARAMETER);
        String spaceName = (String) getRequest().getAttributes().get(Constants.SPACE_NAME_PARAMETER);
        String pageName = (String) getRequest().getAttributes().get(Constants.PAGE_NAME_PARAMETER);
        String languageId = (String) getRequest().getAttributes().get(Constants.LANGUAGE_ID_PARAMETER);
        Form queryForm = getRequest().getResourceRef().getQueryAsForm();
        String pageFullName = String.format("%s.%s", spaceName, pageName);

        String order = "desc"; // fromLatest ? "desc" : "asc";
        String query =
            String
                .format(
                    "select doc.fullName, rcs.id, rcs.date, rcs.author from XWikiRCSNodeInfo as rcs, XWikiDocument as doc where rcs.id.docId=doc.id and doc.fullName=:pageFullName and doc.language=:languageId order by rcs.date %s, rcs.id.version1 %s, rcs.id.version2 %s",
                    order, order, order);

        QueryManager queryManager = (QueryManager) com.xpn.xwiki.web.Utils.getComponent(QueryManager.ROLE);

        List<Object> queryResult = null;

        try {
            queryResult =
                queryManager.createQuery(query, Query.XWQL).bindValue("pageFullName", pageFullName).bindValue(
                    "languageId", languageId != null ? languageId : "").setLimit(
                    Utils.parseInt(queryForm.getFirstValue(Constants.NUMBER_PARAMETER), -1)).setOffset(
                    Utils.parseInt(queryForm.getFirstValue(Constants.START_PARAMETER), 0)).execute();
        } catch (QueryException e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            return null;
        }

        History history = new History();
        for (Object object : queryResult) {
            Object[] fields = (Object[]) object;
            HistorySummary historySummary =
                DomainObjectFactory.createHistorySummary(getRequest(), resourceClassRegistry, wikiName, languageId, fields);
            history.addHistorySummary(historySummary);
        }

        return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), history);
    }
}
