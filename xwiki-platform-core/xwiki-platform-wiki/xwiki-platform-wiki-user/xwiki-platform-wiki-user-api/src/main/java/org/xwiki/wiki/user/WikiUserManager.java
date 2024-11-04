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
import org.xwiki.model.reference.DocumentReference;

/**
 * Component that manage how users can participate on a wiki (local users, members, invitation, etc...).
 *
 * @since 5.3M2
 * @version $Id$
 */
@Role
public interface WikiUserManager
{
    /**
     * @param wikiId Id of the wiki to test
     * @return the user scope of the wiki
     * @throws WikiUserManagerException if problems occur
     */
    UserScope getUserScope(String wikiId) throws WikiUserManagerException;

    /**
     * Set the user scope of the wiki.
     *
     * @param wikiId Id of the wiki to change
     * @param scope the scope to set
     * @throws WikiUserManagerException if problems occur
     */
    void setUserScope(String wikiId, UserScope scope) throws WikiUserManagerException;

    /**
     * @param wikiId if of the wiki to test
     * @return the membership type of the specified wiki
     * @throws WikiUserManagerException if problems occur
     */
    MembershipType getMembershipType(String wikiId) throws WikiUserManagerException;

    /**
     * Set the membership type of the wiki and save the configuration.
     *
     * @param wikiId Id of the wiki to change
     * @param type the membershyp type to set
     * @throws WikiUserManagerException if problems occur
     */
    void setMembershipType(String wikiId, MembershipType type) throws WikiUserManagerException;

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
     * To know if a user is a member of a wiki.
     *
     * @param userId Id of the user
     * @param wikiId Id of the wiki
     * @return if the user is a member of the specified wiki
     * @throws WikiUserManagerException if problems occur
     */
    boolean isMember(String userId, String wikiId) throws WikiUserManagerException;

    /**
     * Add a user as a member.
     *
     * @param userId UserID to add
     * @param wikiId Id of the wiki
     * @throws WikiUserManagerException if problems occur
     */
    void addMember(String userId, String wikiId) throws WikiUserManagerException;

    /**
     * Add a list of users as a member.
     *
     * @param userIds List of userID to add
     * @param wikiId Id of the wiki
     * @throws WikiUserManagerException if problems occur
     */
    void addMembers(Collection<String> userIds, String wikiId) throws WikiUserManagerException;

    /**
     * Remove a member.
     *
     * @param userId UserID to remove
     * @param wikiId Id the the wiki
     * @throws WikiUserManagerException if problems occur
     */
    void removeMember(String userId, String wikiId) throws WikiUserManagerException;

    /**
     * Remove a list of members.
     *
     * @param userIds List of UserID to remove
     * @param wikiId Id the the wiki
     * @throws WikiUserManagerException if problems occur
     */
    void removeMembers(Collection<String> userIds, String wikiId) throws WikiUserManagerException;

    /**
     * Get all the invitations to join a wiki.
     *
     * @param wikiId id of the wiki to join
     * @return a list of invitations to join this wiki
     * @throws WikiUserManagerException if problems occur
     */
    Collection<MemberCandidacy> getAllInvitations(String wikiId) throws WikiUserManagerException;

    /**
     * Get all the join requests for a wiki.
     *
     * @param wikiId id of the wiki to join
     * @return a list of join request for this wiki
     * @throws WikiUserManagerException if problems occur
     */
    Collection<MemberCandidacy> getAllRequests(String wikiId) throws WikiUserManagerException;

    /**
     * @param user DocumentReference to the user to test
     * @param wikiId id of the wiki to test
     * @return either or not the user has a pending invitation to join the wiki
     * @throws WikiUserManagerException if problems occur
     */
    boolean hasPendingInvitation(DocumentReference user, String wikiId) throws WikiUserManagerException;

    /**
     * @param user DocumentReference to the user to test
     * @param wikiId id of the wiki to test
     * @return either or not the user has a pending request to join the wiki
     * @throws WikiUserManagerException if problems occur
     */
    boolean hasPendingRequest(DocumentReference user, String wikiId) throws WikiUserManagerException;

    /**
     * Get the specified member candidacy.
     *
     * @param wikiId Od of the request concerned by the candidacy
     * @param candidacyId Id of the candidacy to get
     * @return the candidacy
     * @throws WikiUserManagerException if problems occur
     */
    MemberCandidacy getCandidacy(String wikiId, int candidacyId) throws WikiUserManagerException;

    /**
     * Perform a request to join a wiki.
     *
     * @param userId UserID of the requester
     * @param wikiId Id of the wiki to join
     * @param message Message that motivates the request
     * @return the generated candidacy
     * @throws WikiUserManagerException if problems occur
     */
    // TODO: this API should also trigger a dedicated event for admins to be aware about the request.
    MemberCandidacy askToJoin(String userId, String wikiId, String message) throws WikiUserManagerException;

    /**
     * Join a wiki.
     *
     * @param userId userId to add to the wiki
     * @param wikiId id of the wiki
     * @throws WikiUserManagerException if problems occur
     */
    // TODO: this API should also trigger a dedicated event for admins to be aware about the request.
    void join(String userId, String wikiId) throws WikiUserManagerException;

    /**
     * Leave a wiki.
     *
     * @param userId userId to remove from the wiki
     * @param wikiId id of the wiki
     * @throws WikiUserManagerException if problems occur
     */
    // TODO: this API should also trigger a dedicated event for admins to be aware about the request.
    void leave(String userId, String wikiId) throws WikiUserManagerException;

    /**
     * Accept the request to join the wiki.
     *
     * @param request request to accept
     * @param message message about the acceptance
     * @param privateComment private comment that only the administrator can see
     * @throws WikiUserManagerException if problems occur
     */
    // TODO: this API should also trigger a dedicated event for admins to be aware about the request.
    void acceptRequest(MemberCandidacy request, String message, String privateComment)
        throws WikiUserManagerException;

    /**
     * Refuse the request to join the wiki.
     *
     * @param request request to refuse
     * @param message message about the refusal
     * @param privateComment private comment that only the administrator can see
     * @throws WikiUserManagerException if problems occur
     */
    // TODO: this API should also trigger a dedicated event for admins to be aware about the request.
    void refuseRequest(MemberCandidacy request, String message, String privateComment)
        throws WikiUserManagerException;

    /**
     * Invite a global user to a wiki.
     *
     * @param userId Id of the user to add
     * @param wikiId Id of the wiki to join
     * @param message MemberCandidacy message
     * @return The generated invitation
     * @throws WikiUserManagerException if problems occur
     */
    // TODO: this API should also trigger a dedicated event for admins to be aware about the request.
    MemberCandidacy invite(String userId, String wikiId, String message) throws WikiUserManagerException;

    /**
     * Accept the invitation to join a wiki.
     *
     * @param invitation invitation to accept
     * @param message message that goes along the acceptance
     * @throws WikiUserManagerException if problems occur
     */
    // TODO: this API should also trigger a dedicated event for admins to be aware about the request.
    void acceptInvitation(MemberCandidacy invitation, String message) throws WikiUserManagerException;

    /**
     * Refuse the invitation to join a wiki.
     *
     * @param invitation invitation to refuse
     * @param message message that goes along the refusal
     * @throws WikiUserManagerException if problems occur
     */
    // TODO: this API should also trigger a dedicated event for admins to be aware about the request.
    void refuseInvitation(MemberCandidacy invitation, String message) throws WikiUserManagerException;

    /**
     * Cancel a candidacy.
     *
     * @param candidacy Candidacy to cancel
     * @throws WikiUserManagerException if problems occur
     */
    // TODO: this API should also trigger a dedicated event for admins to be aware about the request.
    void cancelCandidacy(MemberCandidacy candidacy) throws WikiUserManagerException;
}
