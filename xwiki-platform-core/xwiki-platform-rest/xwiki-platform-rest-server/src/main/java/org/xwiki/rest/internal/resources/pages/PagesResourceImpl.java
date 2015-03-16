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

import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.resources.pages.PagesResource;

import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.pages.PagesResourceImpl")
public class PagesResourceImpl extends XWikiResource implements PagesResource
{
    @Override
    public Pages getPages(String wikiName, String spaceName, Integer start, Integer number,
            String parentFilterExpression, String order, Boolean withPrettyNames)
            throws XWikiRestException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();

        Pages pages = objectFactory.createPages();

        try {
            Utils.getXWikiContext(componentManager).setWikiId(wikiName);

            Query query = ("date".equals(order)) ? queryManager.createQuery(
                    "select doc.name from Document doc where doc.space=:space and language='' order by doc.date desc",
                    "xwql") : queryManager.getNamedQuery("getSpaceDocsName");

            /* Use an explicit query to improve performance */
            List<String> pageNames =
                    query.addFilter(componentManager.<QueryFilter>getInstance(QueryFilter.class, "hidden"))
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
                            .format("[Page '%s' appears to be in space '%s' but no information is available.]",
                                    pageName,
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
                            pages.getPageSummaries().add(DomainObjectFactory.createPageSummary(objectFactory,
                                    uriInfo.getBaseUri(), doc, Utils.getXWikiApi(componentManager), withPrettyNames));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new XWikiRestException(e);
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }

        return pages;
    }
}
