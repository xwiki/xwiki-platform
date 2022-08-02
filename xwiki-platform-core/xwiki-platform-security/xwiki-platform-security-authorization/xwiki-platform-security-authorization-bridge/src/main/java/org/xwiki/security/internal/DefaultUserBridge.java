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
package org.xwiki.security.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AuthorizationException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiGroupService;

/**
 * Temporary implementation of the (@link UserBridge} interface to access user information.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultUserBridge implements UserBridge
{
    /** Security reference factory. */
    @Inject
    private SecurityReferenceFactory factory;

    /** Execution object. */
    @Inject
    private Execution execution;

    /**
     * @return the current {@code XWikiContext}
     */
    private XWikiContext getXWikiContext()
    {
        return ((XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY));
    }

    @Override
    public Collection<GroupSecurityReference> getAllGroupsFor(UserSecurityReference user, WikiReference wikiReference)
        throws AuthorizationException
    {
        DocumentReference userRef = user.getOriginalReference();

        if (userRef == null) {
            // Public users (not logged in) may not appears in any group
            return Collections.emptyList();
        }

        Collection<DocumentReference> groupRefs = getGroupsReferencesFor(wikiReference, userRef);

        Collection<GroupSecurityReference> groups = new ArrayList<GroupSecurityReference>(groupRefs.size());
        for (DocumentReference groupRef : groupRefs) {
            GroupSecurityReference group = factory.newGroupReference(groupRef);
            groups.add(group);
        }
        return groups;
    }

    /**
     * Get all groups in a given wiki where a given user or group is a member of.
     *
     * @param wiki the wiki to search groups containing the user/group
     * @param userOrGroupDocumentReference the user/group document reference
     * @return the list of group where the user/group is a member
     * @throws AuthorizationException when an issue arise during retrieval.
     */
    private Collection<DocumentReference> getGroupsReferencesFor(WikiReference wiki,
        DocumentReference userOrGroupDocumentReference) throws AuthorizationException
    {
        XWikiContext xwikiContext = getXWikiContext();
        XWikiGroupService groupService;
        try {
            groupService = xwikiContext.getWiki().getGroupService(xwikiContext);
        } catch (Exception e) {
            throw new AuthorizationException("Failed to access the group service.",  e);
        }

        String currentWiki = xwikiContext.getWikiId();
        Collection<DocumentReference> groupReferences = new HashSet<>();
        try {
            xwikiContext.setWikiId(wiki.getName());            
            // We get the groups of the member via the group service but we make sure to not use the group service's
            // cache by calling the method with a limit and an offset.
            //
            // We do not use the group service's cache because it might not have been refreshed yet (for example, it can
            // happen when the security module is used inside a listener that reacts to the "SaveDocument" event just 
            // before the XWikiGroupService listener is called). Because of this race condition, it is not a good idea
            // to have a cache depending on an other cache.
            //
            // TODO: use a proper component to retrieve the groups of a member without any cache
            final int nb = 1000;
            int i = 0;
            while (groupReferences.addAll(groupService.getAllGroupsReferencesForMember(userOrGroupDocumentReference,
                nb, i * nb, xwikiContext))) {
                i++;
            }            
            return groupReferences;
        } catch (Exception e) {
            throw new AuthorizationException(String.format("Failed to get groups for user or group [%s] in wiki [%s]",
                userOrGroupDocumentReference, wiki), e);
        } finally {
            xwikiContext.setWikiId(currentWiki);
        }
    }
}
