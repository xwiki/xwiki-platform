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
package org.xwiki.wiki.user;

import java.util.Collection;

import org.xwiki.component.annotation.Role;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Component that manage how users can participate on a wiki (local users, members, invitation, etc...).
 * @since 5.3M2
 * @version $Id$
 */
@Role
public interface WikiUserManager
{
    /**
     * @param wikiId Id of the wiki to test
     * @return if local users are enabled on the specified wiki
     */
    boolean hasLocalUsersEnabled(String wikiId) throws WikiManagerException;

    /**
     * @param wikiId Id of the wiki to change
     * @param enable enable or not the local users on the wiki
     */
    void enableLocalUsers(String wikiId, boolean enable) throws WikiManagerException;

    /**
     * @param wikiId if of the wiki to test
     * @return the membership type of the specified wiki
     */
    MembershipType getMembershipType(String wikiId) throws WikiManagerException;

    /**
     * @param wikiId Id of the wiki to change
     * @param type the membershyp type to set
     */
    void setMembershypType(String wikiId, MembershipType type) throws WikiManagerException;

    /**
     * @param wikiId id of the wiki
     * @return the list of all the local users
     * @throws WikiUserManagerException if problems occur
     */
    Collection<String> getLocalUsers(String wikiId) throws WikiUserManagerException;

    /**
     * @param wikiId if the the wiki
     * @return the list of all the members (global users).
     * @throws WikiUserManagerException if problems occur
     */
    Collection<String> getMembers(String wikiId) throws WikiUserManagerException;

    /**
     * Add a user as a member.
     * @param userId UserID to add
     * @param wikiId Id of the wiki
     * @throws WikiUserManagerException if problems occur
     */
    void addMember(String userId, String wikiId) throws WikiUserManagerException;

    /**
     * Remove a member.
     * @param userId UserID to remove
     * @param wikiId Id the the wiki
     * @throws WikiUserManagerException if problems occur
     */
    void removeMember(String userId, String wikiId) throws WikiUserManagerException;

    /**
     * Perform a request to join a wiki.
     * @param userId UserID of the requester
     * @param wikiId Id of the wiki to join
     * @param message Message that motivates the request
     * @return the generated candidacy
     * @throws WikiUserManagerException if problems occur
     */
    MemberCandidacy askToJoin(String userId, String wikiId, String message) throws WikiUserManagerException;

    /**
     * Accept the request to join the wiki.
     * @param request request to accept
     * @param message message about the acceptance
     * @param privateComment private comment that only the administrator can see
     * @throws WikiUserManagerException if problem occurs
     */
    void acceptRequest(MemberCandidacy request, String message, String privateComment)
            throws WikiUserManagerException;

    /**
     * Refuse the request to join the wiki.
     * @param request request to refuse
     * @param message message about the refusal
     * @param privateComment private comment that only the administrator can see
     * @throws WikiUserManagerException if problem occurs
     */
    void refuseRequest(MemberCandidacy request, String message, String privateComment)
            throws WikiUserManagerException;

    /**
     * Invite a global user to a wiki.
     * @param userId Id of the user to add
     * @param wikiId Id of the wiki to join
     * @param message MemberCandidacy message
     * @return The generated invitation
     * @throws WikiUserManagerException if problems occur
     */
    MemberCandidacy invite(String userId, String wikiId, String message) throws WikiUserManagerException;

    /**
     * Accept the invitation to join a wiki.
     * @param invitation invitation to accept
     * @param message message that goes along the acceptance
     * @throws WikiUserManagerException if problems occur
     */
    void acceptInvitation(MemberCandidacy invitation, String message) throws WikiUserManagerException;

    /**
     * Refuse the invitation to join a wiki.
     * @param invitation invitation to refuse
     * @param message message that goes along the refusal
     * @throws WikiUserManagerException if problems occur
     */
    void refuseInvitation(MemberCandidacy invitation, String message) throws WikiUserManagerException;

}
