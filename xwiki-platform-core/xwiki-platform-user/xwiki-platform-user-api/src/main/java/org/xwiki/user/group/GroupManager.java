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
package org.xwiki.user.group;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.stability.Unstable;

/**
 * Fast access to group membership.
 * 
 * @version $Id$
 * @since 10.8RC1
 */
@Role
public interface GroupManager
{
    /**
     * Search groups the passed user or group is member of.
     * <p>
     * {code wikis} controls where to search for the groups and {@code recurse} only the direct group should be returned
     * or the whole hierarchy.
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
    Collection<DocumentReference> getGroups(DocumentReference member, Object wikiTarget, boolean recurse)
        throws GroupException;

    /**
     * Retrieve the users and groups which are the members of the passed group.
     * 
     * @param group the group for which to return the members
     * @param recurse false if only the direct members should be returned, true to take into account groups of groups
     * @return the members of the passed group
     * @throws GroupException when failing to get members
     */
    Collection<DocumentReference> getMembers(DocumentReference group, boolean recurse) throws GroupException;

    /**
     * @param wikiReference the wiki for which to get the groups
     * @return the groups found in the passed wiki
     * @throws GroupException when failing to get groups
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    default Set<DocumentReference> getGroups(WikiReference wikiReference) throws GroupException
    {
        return Set.of();
    }

    /**
     * @param reference the reference of the document to check
     * @return true of the passed document is a group, false otherwise
     * @throws GroupException when failing to check if the document is a group
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    default boolean isGroup(DocumentReference reference) throws GroupException
    {
        // Not always true, but a reasonable default implementation from API point of view
        return !getMembers(reference, false).isEmpty();
    }

    /**
     * Indicate if the passed member (user or group) is a member of the passed group.
     * <p>
     * The membership is resolved from the groups of the member (see
     * {@link #getGroups(DocumentReference, Object, boolean)}), which differs from
     * {@code getMembers(group, recurse).contains(member)} in two ways:
     * <ul>
     * <li>when {@code recurse} is true, the intermediate groups are only searched in the wiki of the member and in the
     * wiki of the group (which covers a global user or group added to a subwiki group): a membership going through a
     * group located in any other wiki is not taken into account</li>
     * <li>the implicit memberships known by the implementation are taken into account, for example every user except
     * the guest user being a member of {@code XWikiAllGroup} when {@code xwiki.authentication.group.allgroupimplicit}
     * is enabled</li>
     * </ul>
     * The locale of the passed references, if any, is ignored, and a group is never a member of itself.
     *
     * @param member the group member (user or group); {@code null} (the guest user) is never a member
     * @param group the group to check; {@code null} has no members
     * @param recurse false to only check direct membership, true to also take into account groups of groups
     * @return {@code true} if the passed member is a member of the passed group
     * @throws GroupException when failing to get the groups of the member
     * @since 18.10.0RC1
     */
    @Unstable
    default boolean isMember(DocumentReference member, DocumentReference group, boolean recurse)
        throws GroupException
    {
        // The guest user (null member) is never a member of any group, a null group has no members, and a group is
        // never a member of itself, even when it lists itself as a member.
        if (member == null || group == null || member.withoutLocale().equals(group.withoutLocale())) {
            return false;
        }

        // Start from the member's groups rather than the group's members: a user usually belongs to a few groups
        // while a group can have thousands of members. A direct membership is stored in the group's document, so only
        // the group's wiki holds it, while a recursive check also needs the member's wiki, where the intermediate
        // groups of the member are usually located.
        WikiReference groupWiki = group.getWikiReference();
        Object wikiTarget = recurse && !member.getWikiReference().equals(groupWiki)
            ? List.of(member.getWikiReference(), groupWiki) : groupWiki;

        return getGroups(member.withoutLocale(), wikiTarget, recurse).contains(group.withoutLocale());
    }
}
