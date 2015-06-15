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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.resources.wikis.WikisResource;
import org.xwiki.rest.resources.wikis.WikisSearchQueryResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserManager;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.wikis.WikisResourceImpl")
public class WikisResourceImpl extends XWikiResource implements WikisResource
{
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    private WikiUserManager wikiUserManager;

    @Inject
    private EntityReferenceValueProvider defaultEntityReferenceValueProvider;

    @Override
    public Wikis getWikis() throws XWikiRestException
    {
        try {
            Wikis wikis = objectFactory.createWikis();

            for (String wikiId : this.wikiDescriptorManager.getAllIds()) {
                // Allow listing a wiki if:
                // - the user has view access to it
                // - or the wiki accepts global users and is not an invitation-based wiki
                // - or the current user has a pending invitation to the wiki
                // Note 1: To check if a user has view access to a wiki we check if the user can access the home page
                //         of the "XWiki" space. We do this because this needs to be allowed in view for the wiki to
                //         work properly.
                // Note 2: it would be nicer to have an API for this.
                // Note 3: this strategy is copied from WikisLiveTableResults.xml
                EntityReference absoluteCommentReference = new EntityReference(
                    this.defaultEntityReferenceValueProvider.getDefaultValue(EntityType.DOCUMENT), EntityType.DOCUMENT,
                        new EntityReference("XWiki", EntityType.SPACE, new EntityReference(wikiId, EntityType.WIKI)));
                DocumentReference currentUserReference = getXWikiContext().getUserReference();
                if (this.authorizationManager.hasAccess(Right.VIEW, absoluteCommentReference)
                    || (this.wikiUserManager.getUserScope(wikiId) != UserScope.LOCAL_ONLY
                        && this.wikiUserManager.getMembershipType(wikiId) != MembershipType.INVITE)
                    || this.wikiUserManager.hasPendingInvitation(currentUserReference, wikiId))
                {
                    wikis.getWikis().add(DomainObjectFactory.createWiki(objectFactory, uriInfo.getBaseUri(), wikiId));
                }
            }

            String queryUri = Utils.createURI(uriInfo.getBaseUri(), WikisSearchQueryResource.class).toString();
            Link queryLink = objectFactory.createLink();
            queryLink.setHref(queryUri);
            queryLink.setRel(Relations.QUERY);
            wikis.getLinks().add(queryLink);

            return wikis;
        } catch (Exception e) {
            throw new XWikiRestException("Failed to get the list of wikis", e);
        }
    }
}
