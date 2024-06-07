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
package org.xwiki.rest.internal.resources;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;

/**
 * Base class for resources that represent a collection of pages.
 *
 * @version $Id$
 * @since 16.4.0RC1
 */
public abstract class AbstractPagesResource extends XWikiResource
{
    @Inject
    protected ModelFactory modelFactory;

    @Inject
    protected ContextualAuthorizationManager contextualAuthorizationManager;

    protected Pages getPages(List<DocumentReference> documentReferences, boolean withPrettyNames)
    {
        return this.objectFactory.createPages().withPageSummaries(documentReferences.stream()
            .filter(documentReference -> this.contextualAuthorizationManager.hasAccess(Right.VIEW, documentReference))
            .map(documentReference -> getPageSummary(documentReference, withPrettyNames)).filter(Objects::nonNull)
            .collect(Collectors.toList()));
    }

    protected PageSummary getPageSummary(DocumentReference documentReference, boolean withPrettyNames)
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            Document document = xcontext.getWiki().getDocument(documentReference, xcontext).newDocument(xcontext);
            return this.modelFactory.toRestPageSummary(this.uriInfo.getBaseUri(), document, withPrettyNames);
        } catch (Exception e) {
            getLogger().warn("Failed to get document [{}]. Root cause is [{}].", documentReference,
                ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }
}
