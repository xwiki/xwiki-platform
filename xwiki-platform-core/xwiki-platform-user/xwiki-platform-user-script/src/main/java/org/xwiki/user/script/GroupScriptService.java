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
package org.xwiki.user.script;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;
import org.xwiki.user.group.WikiTarget;

/**
 * Groups related script API.
 * 
 * @version $Id$
 * @since 10.8RC1
 */
@Component
@Named(GroupScriptService.ROLEHINT)
@Singleton
@Unstable
public class GroupScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ROLEHINT = "user.group";

    @Inject
    private GroupManager groupManager;

    /**
     * Search groups the passed user or group is member of.
     * <p>
     * {code wikis} controls where to search for the groups and {@code recurse} only the direct group should be
     * returned or the whole hierarchy.
     * 
     * @param member the group member (user or group)
     * @param wikiTarget the wikis where to search. The following types are supported:
     *            <ul>
     *            <li>{@link org.xwiki.user.group.WikiTarget}</li>
     *            <li>{@link String}</li>
     *            <li>{@code Collection<String>}</li>
     *            <li>{@code org.xwiki.model.reference.WikiReference}</li>
     *            <li>{@code Collection<org.xwiki.model.reference.WikiReference>}</li>
     *            </ul>
     * @param recurse false if only the direct groups should be returned, true to take into account groups of groups
     * @return the groups the passed user or group is member of
     * @throws GroupException when failing to get groups
     */
    public Collection<DocumentReference> getGroups(DocumentReference member, Object wikiTarget, boolean recurse)
        throws GroupException
    {
        return this.groupManager.getGroups(member, wikiTarget, recurse);
    }

    /**
     * Search groups the passed user or group is member of in all wikis.
     * 
     * @param member the group member (user or group)
     * @return the groups the passed user or group is member of
     * @throws GroupException when failing to get groups
     */
    public Collection<DocumentReference> getGroupsFromAllWikis(DocumentReference member) throws GroupException
    {
        return getGroups(member, null, true);
    }

    /**
     * Search groups the passed user or group is member of in the passed reference wiki.
     * 
     * @param member the group member (user or group)
     * @return the groups the passed user or group is member of
     * @throws GroupException when failing to get groups
     */
    public Collection<DocumentReference> getGroupsFromMemberWiki(DocumentReference member) throws GroupException
    {
        return getGroups(member, WikiTarget.ENTITY, true);
    }

    /**
     * Search groups the passed user or group is member of in the passed reference wiki and the current wiki.
     * 
     * @param member the group member (user or group)
     * @return the groups the passed user or group is member of
     * @throws GroupException when failing to get groups
     */
    public Collection<DocumentReference> getGroupsFromMemberAndCurrentWiki(DocumentReference member)
        throws GroupException
    {
        return getGroups(member, WikiTarget.ENTITY_AND_CURRENT, true);
    }

    /**
     * Get the users and groups which belong to the passed group (including members of sub-groups).
     * <p>
     * It's possible control if only the direct members or all the members should be returned using {@code recurse}
     * parameter.
     * 
     * @param group the group for which to return the members
     * @param recurse false if only the direct members should be returned, true to take into account groups of groups
     * @return the members of the passed group
     * @throws GroupException when failing to get members
     */
    public Collection<DocumentReference> getMembers(DocumentReference group, boolean recurse) throws GroupException
    {
        return this.groupManager.getMembers(group, recurse);
    }

    /**
     * Get all the users and groups which belong to the passed group (including members of sub-groups).
     * 
     * @param group the group for which to return the members
     * @return the members of the passed group
     * @throws GroupException when failing to get members
     */
    public Collection<DocumentReference> getMembers(DocumentReference group) throws GroupException
    {
        return getMembers(group, true);
    }
}
