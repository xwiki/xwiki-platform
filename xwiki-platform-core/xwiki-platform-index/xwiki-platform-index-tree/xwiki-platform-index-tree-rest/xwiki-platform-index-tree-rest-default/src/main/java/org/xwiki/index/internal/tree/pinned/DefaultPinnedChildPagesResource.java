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

import org.xwiki.component.annotation.Component;
import org.xwiki.index.tree.internal.nestedpages.pinned.PinnedChildPagesManager;
import org.xwiki.index.tree.pinned.PinnedChildPagesResource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Default implementation of {@link PinnedChildPagesResource}.
 *
 * @version $Id$
 * @since 17.2.0RC1
 * @since 16.10.4
 * @since 16.4.7
 */
@Component
@Named("org.xwiki.index.internal.tree.pinned.DefaultPinnedChildPagesResource")
public class DefaultPinnedChildPagesResource extends XWikiResource
    implements PinnedChildPagesResource, XWikiRestComponent
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
    public List<String> getPinnedChildPages(String wikiName, String spaceName) throws XWikiRestException
    {
        List<String> spaces = parseSpaceSegments(spaceName.substring("/spaces/".length()));
        DocumentReference homeReference = new DocumentReference(wikiName, spaces,
            this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName());
        List<DocumentReference> pinnedChildPages = this.pinnedChildPagesManager.getPinnedChildPages(homeReference);
        return pinnedChildPages.stream().map(this.entityReferenceSerializer::serialize).toList();
    }

    @Override
    public Response setPinnedChildPages(String wikiName, String spaceName, List<String> pinnedChildPages)
        throws XWikiRestException
    {
        List<String> spaces = parseSpaceSegments(spaceName.substring("/spaces/".length()));
        DocumentReference homeReference = new DocumentReference(wikiName, spaces,
            this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName());
        if (!this.authorizationManager.hasAccess(Right.EDIT, homeReference)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        List<DocumentReference> pinnedChildPagesRef =
            pinnedChildPages.stream().map(documentReferenceResolver::resolve).toList();

        for (DocumentReference childPage : pinnedChildPagesRef) {
            if (!childPage.hasParent(homeReference.getLastSpaceReference())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
        this.pinnedChildPagesManager.setPinnedChildPages(homeReference.getLastSpaceReference(), pinnedChildPagesRef);
        return Response.accepted().build();
    }
}
