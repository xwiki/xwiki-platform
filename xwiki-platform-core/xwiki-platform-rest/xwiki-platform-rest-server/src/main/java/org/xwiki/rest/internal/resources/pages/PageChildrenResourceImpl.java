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

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.resources.pages.PageChildrenResource;

import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.pages.PageChildrenResourceImpl")
public class PageChildrenResourceImpl extends XWikiResource implements PageChildrenResource
{
    @Override
    public Pages getPageChildren(String wikiName, String spaceName, String pageName, Integer start, Integer number,
        Boolean withPrettyNames, String hierarchy, String search) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

            Document doc = documentInfo.getDocument();

            Pages pages = objectFactory.createPages();

            /* Use an explicit query to improve performance */
            String queryString = "select distinct doc.fullName from XWikiDocument as doc "
                + "where doc.parent = :parent order by doc.fullName asc";
            List<String> childPageFullNames = queryManager.createQuery(queryString, Query.XWQL)
                .bindValue("parent", doc.getFullName()).setOffset(start).setLimit(number).execute();

            for (String childPageFullName : childPageFullNames) {
                String pageId = Utils.getPageId(wikiName, childPageFullName);

                if (!Utils.getXWikiApi(componentManager).exists(pageId)) {
                    getLogger().warn("Child page [{}] of [{}] is missing.", childPageFullName, doc.getFullName());
                } else {
                    Document childDoc = Utils.getXWikiApi(componentManager).getDocument(pageId);

                    /* We only add pages we have the right to access */
                    if (childDoc != null) {
                        pages.getPageSummaries().add(DomainObjectFactory.createPageSummary(objectFactory,
                            uriInfo.getBaseUri(), childDoc, Utils.getXWikiApi(componentManager), withPrettyNames));
                    }
                }
            }

            return pages;
        } catch (Exception e) {
            throw new XWikiRestException(e);
        }
    }
}
