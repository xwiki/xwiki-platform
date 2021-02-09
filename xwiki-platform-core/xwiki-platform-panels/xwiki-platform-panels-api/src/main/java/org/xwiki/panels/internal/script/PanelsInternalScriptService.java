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
package org.xwiki.panels.internal.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * This script service provides useful operation for the panel pages.
 *
 * @version $Id$
 * @since 13.1RC1
 * @since 12.10.4
 * @since 12.6.8
 */
@Component
@Named("panels")
@Singleton
public class PanelsInternalScriptService implements ScriptService
{
    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("hidden/document")
    private QueryFilter hiddenDocumentQueryFilter;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Logger logger;

    /**
     * Returns the paginated list of orphaned pages, as well as the offset for the next call to this method, and if
     * their is more elements to retrive.
     *
     * @param limit the number of elements to return
     * @param offset the offset to use of the pagination
     * @param homepage the current wiki home page
     * @return an object holding the pagination state as well as the paginated list of orphaned pages
     */
    public OrphanedPagesItem listOrphaned(int limit, int offset, String homepage)
    {
        List<String> ret = new ArrayList<>();
        int newOffset = offset;
        try {
            boolean hasMore;
            List<String> orphanedPages = queryOrphanedPages(limit, homepage, newOffset);
            do {
                for (String orphanedPage : orphanedPages) {
                    // Update the offset to return an updated offset to the caller
                    newOffset = newOffset + 1;
                    if (this.authorizationManager
                        .hasAccess(Right.VIEW, this.documentAccessBridge.getCurrentUserReference(),
                            this.documentReferenceResolver.resolve(orphanedPage)))
                    {
                        ret.add(orphanedPage);
                    }
                    if (ret.size() >= limit) {
                        break;
                    }
                }
                if (ret.size() >= limit) {
                    hasMore = true;
                } else {
                    orphanedPages = queryOrphanedPages(limit, homepage, newOffset);
                    hasMore = !orphanedPages.isEmpty();
                }
            } while (hasMore && ret.size() < limit);

            return new OrphanedPagesItem(ret, newOffset, hasMore);
        } catch (QueryException e) {
            this.logger.warn(
                "Failed to retrieve the list of orphaned pages with limit [{}], offset [{}], and homepage [{}]. "
                    + "Cause: [{}]",
                limit, offset, homepage, getRootCauseMessage(e));
            return new OrphanedPagesItem(Collections.emptyList(), newOffset, false);
        }
    }

    private List<String> queryOrphanedPages(int limit, String homepage, int newOffset) throws QueryException
    {
        return this.queryManager.createQuery(
            "where doc.parent is null "
                + "or doc.parent='' "
                + "and doc.fullName <> :homepage "
                + "order by doc.contentUpdateDate desc", Query.XWQL)
            .setLimit(limit)
            .setOffset(newOffset)
            .addFilter(this.hiddenDocumentQueryFilter)
            .bindValue("homepage", homepage)
            .execute();
    }
}
