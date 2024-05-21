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

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.index.tree.PageHierarchy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.resources.AbstractPagesResource;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.resources.pages.PageChildrenResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.pages.PageChildrenResourceImpl")
public class PageChildrenResourceImpl extends AbstractPagesResource implements PageChildrenResource
{
    @Inject
    @Named("nestedpages")
    private PageHierarchy nestedPageHierarchy;

    @Inject
    private QueryFilter documentFilter;

    @Override
    public Pages getPageChildren(String wikiName, String spaceName, String pageName, Integer start, Integer number,
        Boolean withPrettyNames, String hierarchy, String search) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

            if ("nestedpages".equals(hierarchy)) {
                return getPages(this.nestedPageHierarchy.getChildren(documentInfo.getDocument().getDocumentReference())
                    .withOffset(start).withLimit(number).matching(search).getDocumentReferences(), withPrettyNames);
            } else {
                // We fall-back (default) to the parent-child hierarchy for backwards compatibility.
                return getPages(getPageChildrenForParentChildHierarchy(documentInfo, start, number), withPrettyNames);
            }
        } catch (XWikiException | QueryException e) {
            throw new XWikiRestException(e);
        }
    }

    private List<DocumentReference> getPageChildrenForParentChildHierarchy(DocumentInfo parentInfo, int start,
        int number) throws QueryException
    {
        Document parentDocument = parentInfo.getDocument();

        // Use an explicit query to improve performance.
        String queryString = "select distinct doc.fullName from XWikiDocument as doc "
            + "where doc.parent = :parent order by doc.fullName asc";
        return this.queryManager.createQuery(queryString, Query.XWQL).bindValue("parent", parentDocument.getFullName())
            .setOffset(start).setLimit(number).addFilter(this.documentFilter).execute();
    }
}
