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

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.index.tree.PageHierarchy;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryException;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.resources.AbstractPagesResource;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.resources.wikis.WikiChildrenResource;
import org.xwiki.security.authorization.Right;

/**
 * The top level pages in the nested page hierarchy of a given wiki.
 *
 * @version $Id$
 * @since 16.4.0RC1
 */
@Component
@Named("org.xwiki.rest.internal.resources.wikis.WikiChildrenResourceImpl")
public class WikiChildrenResourceImpl extends AbstractPagesResource implements WikiChildrenResource
{
    @Inject
    @Named("nestedpages")
    private PageHierarchy nestedPageHierarchy;

    @Override
    public Pages getChildren(String wikiName, Integer offset, Integer limit, String search) throws XWikiRestException
    {
        WikiReference wikiReference = new WikiReference(wikiName);
        if (!this.contextualAuthorizationManager.hasAccess(Right.VIEW, wikiReference)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        try {
            return getPages(this.nestedPageHierarchy.getChildren(wikiReference).withOffset(offset).withLimit(limit)
                .matching(search).getDocumentReferences(), true);
        } catch (QueryException e) {
            throw new XWikiRestException("Failed to retrieve the top level pages.", e);
        }
    }
}
