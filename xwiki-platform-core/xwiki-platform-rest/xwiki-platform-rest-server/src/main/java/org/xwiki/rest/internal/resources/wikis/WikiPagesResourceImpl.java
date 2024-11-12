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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.WikiReference;
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
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.wikis.WikiPagesResourceImpl")
public class WikiPagesResourceImpl extends XWikiResource implements WikiPagesResource
{
    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Override
    public Pages getPages(String wikiName, Integer start, String name, String space, String author, Integer number)
            throws XWikiRestException
    {
        XWikiContext context = Utils.getXWikiContext(componentManager);
        WikiReference wikiReference = context.getWikiReference();
        context.setWikiReference(new WikiReference(wikiName));

        Pages pages = objectFactory.createPages();
        try {
            Map<String, String> filters = new HashMap<>();
            if (!StringUtils.isEmpty(name)) {
                filters.put("name", name);
            }
            if (!StringUtils.isEmpty(space)) {
                filters.put("space", Utils.getLocalSpaceId(parseSpaceSegments(space)));
            }
            if (!StringUtils.isEmpty(author)) {
                filters.put("author", author);
            }

            /* Build the query */
            StringBuilder stringBuilder = new StringBuilder("select doc from XWikiDocument as doc");

            if (!filters.isEmpty()) {
                stringBuilder.append(" where (");

                int i = 0;
                for (String param : filters.keySet()) {
                    if (param.equals("name")) {
                        stringBuilder.append("upper(doc.fullName) like :name ");
                    }
                    if (param.equals("space")) {
                        stringBuilder.append("upper(doc.space) like :space ");
                    }
                    if (param.equals("author")) {
                        stringBuilder.append("upper(doc.contentAuthor) like :author ");
                    }
                    i++;

                    if (i < filters.keySet().size()) {
                        stringBuilder.append("and ");
                    }
                }

                stringBuilder.append(")");
            }

            String queryString = stringBuilder.toString();

            /* Execute the query by filling the parameters */
            List<Object> queryResult = null;
            Query query = queryManager.createQuery(queryString, Query.XWQL)
                .setWiki(wikiName)
                .setLimit(number)
                .setOffset(start);

            for (Map.Entry<String, String> filterEntry : filters.entrySet()) {
                query.bindValue(filterEntry.getKey(), String.format("%%%s%%", filterEntry.getValue().toUpperCase()));
            }

            queryResult = query.execute();
            XWikiURLFactory urlFactory = context.getURLFactory();

            /* Get the results and populate the returned representation */
            for (Object object : queryResult) {
                XWikiDocument xwikiDocument = (XWikiDocument) object;

                Document doc = new Document(xwikiDocument, context);
                if (this.contextualAuthorizationManager.hasAccess(Right.VIEW, doc.getDocumentReference())) {
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

                    URL absoluteUrl = urlFactory.createExternalURL(
                        doc.getSpace(), doc.getDocumentReference().getName(), "view", null, null,
                        context);
                    pageSummary.setXwikiAbsoluteUrl(absoluteUrl.toString());
                    pageSummary.setXwikiRelativeUrl(urlFactory.getURL(
                        absoluteUrl, context));

                    String pageUri = Utils
                        .createURI(uriInfo.getBaseUri(), PageResource.class, doc.getWiki(),
                            Utils.getSpacesURLElements(doc.getDocumentReference()),
                            doc.getDocumentReference().getName())
                        .toString();
                    Link pageLink = objectFactory.createLink();
                    pageLink.setHref(pageUri);
                    pageLink.setRel(Relations.PAGE);
                    pageSummary.getLinks().add(pageLink);

                    pages.getPageSummaries().add(pageSummary);
                }
            }
        } catch (QueryException e) {
            throw new XWikiRestException(e);
        } finally {
            context.setWikiReference(wikiReference);
        }

        return pages;
    }
}
