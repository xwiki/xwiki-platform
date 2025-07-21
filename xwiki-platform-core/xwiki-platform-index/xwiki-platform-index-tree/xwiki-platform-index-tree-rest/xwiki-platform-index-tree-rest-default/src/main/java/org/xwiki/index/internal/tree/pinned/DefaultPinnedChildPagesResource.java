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
package org.xwiki.index.internal.tree.pinned;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.index.tree.internal.nestedpages.pinned.PinnedChildPagesManager;
import org.xwiki.index.tree.pinned.PinnedChildPagesResource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Default implementation of {@link PinnedChildPagesResource}.
 *
 * @version $Id$
 * @since 17.2.0RC1
 * @since 16.10.5
 * @since 16.4.7
 */
@Component
@Named("org.xwiki.index.internal.tree.pinned.DefaultPinnedChildPagesResource")
public class DefaultPinnedChildPagesResource extends XWikiResource implements PinnedChildPagesResource
{
    @Inject
    private PinnedChildPagesManager pinnedChildPagesManager;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Override
    public List<String> parseSpaceSegments(String spaceName) throws XWikiRestException
    {
        return super.parseSpaceSegments(spaceName.substring("/spaces/".length()));
    }

    @Override
    public List<String> getPinnedChildPages(String wikiName, String spaceName) throws XWikiRestException
    {
        EntityReference homeReference;
        if (!StringUtils.isEmpty(spaceName)) {
            List<String> spaces = parseSpaceSegments(spaceName);
            homeReference = new DocumentReference(wikiName, spaces,
                this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName());
        } else {
            homeReference = new WikiReference(wikiName);
        }
        List<DocumentReference> pinnedChildPages = this.pinnedChildPagesManager.getPinnedChildPages(homeReference);
        return pinnedChildPages.stream().map(this.entityReferenceSerializer::serialize).toList();
    }

    @Override
    public Response setPinnedChildPages(String wikiName, String spaceName, List<String> pinnedChildPages)
        throws XWikiRestException
    {
        EntityReference homeReference;
        if (!StringUtils.isEmpty(spaceName)) {
            List<String> spaces = parseSpaceSegments(spaceName);
            homeReference = new DocumentReference(wikiName, spaces,
                this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName());
            if (!this.authorizationManager.hasAccess(Right.EDIT, homeReference)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        } else {
            homeReference = new WikiReference(wikiName);
            if (!this.authorizationManager.hasAccess(Right.ADMIN, homeReference)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }

        List<DocumentReference> pinnedChildPagesRef =
            pinnedChildPages.stream().map(documentReferenceResolver::resolve).toList();

        this.pinnedChildPagesManager.setPinnedChildPages(homeReference, pinnedChildPagesRef);
        return Response.accepted().build();
    }
}
