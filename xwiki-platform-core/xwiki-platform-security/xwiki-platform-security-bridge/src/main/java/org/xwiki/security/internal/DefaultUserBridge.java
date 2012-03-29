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
import java.util.Formatter;

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
    private XWikiContext getXWikiContext() {
        return ((XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY));
    }

    @Override
    public Collection<GroupSecurityReference> getAllGroupsFor(UserSecurityReference user) throws AuthorizationException
    {
        Collection<DocumentReference> groupRefs = getGroupsReferencesFor(user.getOriginalReference().getWikiReference(),
            user.getOriginalReference());

        Collection<GroupSecurityReference> groups = new ArrayList<GroupSecurityReference>(groupRefs.size());
        for (DocumentReference groupRef : groupRefs) {
            GroupSecurityReference group = factory.newGroupReference(groupRef);
            groups.add(group);
        }
        return groups;
    }

    /**
     * Get all groups in a given wiki where a given user or group is a member of.
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

        String currentWiki = xwikiContext.getDatabase();
        try {
            xwikiContext.setDatabase(wiki.getName());
            return groupService.getAllGroupsReferencesForMember(userOrGroupDocumentReference, 0, 0, xwikiContext);
        } catch (Exception e) {
            throw new AuthorizationException(new Formatter()
                                             .format("Failed to get groups for user or group [%s] in wiki [%s]",
                                                     userOrGroupDocumentReference,
                                                     wiki)
                                             .toString(), e);
        } finally {
            xwikiContext.setDatabase(currentWiki);
        }
    }
}
