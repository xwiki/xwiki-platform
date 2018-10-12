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
package org.xwiki.rest.internal.resources.wikis;

import java.net.URL;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.wikis.WikiPagesResource;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.wikis.WikiPagesResourceImpl")
public class WikiPagesResourceImpl extends XWikiResource implements WikiPagesResource
{
    @Override
    public Pages getPages(String wikiName, Integer start, String name, String space, String author, Integer number)
            throws XWikiRestException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();

        Pages pages = objectFactory.createPages();

        /* This try is just needed for executing the finally clause. */
        try {
            Map<String, String> filters = new HashMap<String, String>();
            if (!name.equals("")) {
                filters.put("name", name);
            }
            if (!space.equals("")) {
                filters.put("space", Utils.getLocalSpaceId(parseSpaceSegments(space)));
            }
            if (!author.equals("")) {
                filters.put("author", author);
            }

            /* Build the query */
            Formatter f = new Formatter();
            f.format("select doc from XWikiDocument as doc");

            if (filters.keySet().size() > 0) {
                f.format(" where (");

                int i = 0;
                for (String param : filters.keySet()) {
                    if (param.equals("name")) {
                        f.format(" upper(doc.fullName) like :name ");
                    }

                    if (param.equals("space")) {
                        f.format(" upper(doc.space) like :space ");
                    }

                    if (param.equals("author")) {
                        f.format(" upper(doc.contentAuthor) like :author ");
                    }

                    i++;

                    if (i < filters.keySet().size()) {
                        f.format(" and ");
                    }
                }

                f.format(")");
            }

            String queryString = f.toString();

            /* Execute the query by filling the parameters */
            List<Object> queryResult = null;
            try {
                Query query = queryManager.createQuery(queryString, Query.XWQL).setLimit(number).setOffset(start);
                for (String param : filters.keySet()) {
                    query.bindValue(param, String.format("%%%s%%", filters.get(param).toUpperCase()));
                }

                queryResult = query.execute();
            } catch (QueryException e) {
                throw new XWikiRestException(e);
            }

            /* Get the results and populate the returned representation */
            for (Object object : queryResult) {
                XWikiDocument xwikiDocument = (XWikiDocument) object;
                xwikiDocument.setDatabase(wikiName);

                Document doc = new Document(xwikiDocument, Utils.getXWikiContext(componentManager));

                /*
                 * We manufacture page summaries in place because we don't have all the data for calling the
                 * DomainObjectFactory method (doing so would require to retrieve an actual Document)
                 */
                PageSummary pageSummary = objectFactory.createPageSummary();
                pageSummary.setId(doc.getPrefixedFullName());
                pageSummary.setFullName(doc.getFullName());
                pageSummary.setWiki(wikiName);
                pageSummary.setSpace(doc.getSpace());
                pageSummary.setName(doc.getDocumentReference().getName());
                pageSummary.setTitle(doc.getTitle());
                pageSummary.setParent(doc.getParent());

                URL absoluteUrl = Utils.getXWikiContext(componentManager).getURLFactory().createExternalURL(
                    doc.getSpace(), doc.getDocumentReference().getName(), "view", null, null,
                    Utils.getXWikiContext(componentManager));
                pageSummary.setXwikiAbsoluteUrl(absoluteUrl.toString());
                pageSummary.setXwikiRelativeUrl(Utils.getXWikiContext(componentManager).getURLFactory().getURL(
                    absoluteUrl, Utils.getXWikiContext(componentManager)));

                String pageUri = Utils.createURI(uriInfo.getBaseUri(), PageResource.class, doc.getWiki(),
                    Utils.getSpacesFromSpaceId(doc.getSpace()), doc.getDocumentReference().getName()).toString();
                Link pageLink = objectFactory.createLink();
                pageLink.setHref(pageUri);
                pageLink.setRel(Relations.PAGE);
                pageSummary.getLinks().add(pageLink);

                pages.getPageSummaries().add(pageSummary);
            }
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }

        return pages;
    }
}
