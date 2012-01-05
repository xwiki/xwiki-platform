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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AuthorizationException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiGroupService;

/**
 * Temporary implementation of the (@link UserBridge} interface to access user information.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultUserBridge implements UserBridge
{
    /** Security reference factory. */
    @Inject
    private SecurityReferenceFactory factory;

    /** Entity reference serializer. */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /** Document reference resolver for users and groups. */
    @Inject
    @Named("user")
    private DocumentReferenceResolver<String> resolver;

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
        Collection<String> groupNames;
        try {
            XWikiContext xwikiContext = getXWikiContext();
            XWikiGroupService groupService = xwikiContext.getWiki().getGroupService(xwikiContext);
            String userName = serializer.serialize(user);
            groupNames = groupService.getAllGroupsNamesForMember(userName, Integer.MAX_VALUE, 0, xwikiContext);
        } catch (XWikiException e) {
            throw new AuthorizationException("Failed to generate the group names.",  e);
        }
        /*
        * The groups inherit the wiki of the user, unless the wiki
        * name is explicitly given.
        */
        WikiReference wikiReference = user.getOriginalReference().getWikiReference();
        Collection<GroupSecurityReference> groups = new ArrayList<GroupSecurityReference>(groupNames.size());
        for (String groupName : groupNames) {
            GroupSecurityReference group = factory.newGroupReference(resolver.resolve(groupName, wikiReference));
            groups.add(group);
        }
        return groups;
    }
}
