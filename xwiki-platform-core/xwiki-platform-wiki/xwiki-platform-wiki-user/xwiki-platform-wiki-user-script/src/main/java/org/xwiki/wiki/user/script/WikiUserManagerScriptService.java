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
package org.xwiki.wiki.user.script;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.MemberCandidacy;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.WikiUserManagerException;

import com.xpn.xwiki.XWikiContext;

/**
 * Script service to manage how the user are supported in a wiki.
 *
 * @since 5.3M2
 * @version $Id$
 */
@Component
@Named("wiki.user")
@Singleton
@Unstable
public class WikiUserManagerScriptService implements ScriptService
{
    @Inject
    private WikiUserManager wikiUserManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * @return the user scope
     */
    public UserScope getUserScope()
    {
        return getUserScope(wikiDescriptorManager.getCurrentWikiId());
    }

    /**
     * @param wikiId Id of the wiki to test
     * @return the user scope
     */
    public UserScope getUserScope(String wikiId)
    {
        try {
            return wikiUserManager.getUserScope(wikiId);
        } catch (WikiUserManagerException e) {
            return null;
        }
    }

    /**
     * @param wikiId Id of the wiki to change
     * @param scope scope to set
     * @return true if it succeed
     */
    public boolean setUserScope(String wikiId, String scope)
    {
        XWikiContext xcontext = xcontextProvider.get();
        boolean success = true;
        try {
            // Check if the current script has the programing rights
            authorizationManager.checkAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
                   xcontext.getDoc().getDocumentReference());
            // Check the right
            authorizationManager.checkAccess(Right.ADMIN, xcontext.getUserReference(), new WikiReference(wikiId));
            // Do the job
            wikiUserManager.setUserScope(wikiId, UserScope.valueOf(scope.toUpperCase()));
        } catch (WikiUserManagerException e) {
            success = false;
        } catch (AccessDeniedException e) {
            success = false;
        }
        return success;
    }

    /**
     * @return the membership type of the current wiki
     */
    public MembershipType getMembershipType()
    {
        return getMembershipType(wikiDescriptorManager.getCurrentWikiId());
    }

    /**
     * @param wikiId Id of the wiki to test
     * @return the membership type of the specified wiki
     */
    public MembershipType getMembershipType(String wikiId)
    {
        try {
            return wikiUserManager.getMembershipType(wikiId);
        } catch (WikiUserManagerException e) {
            return null;
        }
    }

    /**
     * @param wikiId Id of the wiki to change
     * @param type the membership type to set
     * @return true if it succeed
     */
    public boolean setMembershipType(String wikiId, String type)
    {
        XWikiContext xcontext = xcontextProvider.get();
        boolean success = true;
        try {
            // Check if the current script has the programing rights
            authorizationManager.checkAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
                    xcontext.getDoc().getDocumentReference());
            // Check the right
            authorizationManager.checkAccess(Right.ADMIN, xcontext.getUserReference(), new WikiReference(wikiId));
            // Do the job
            wikiUserManager.setMembershipType(wikiId, MembershipType.valueOf(type.toUpperCase()));
        } catch (WikiUserManagerException e) {
            success = false;
        } catch (AccessDeniedException e) {
            success = false;
        }
        return success;
    }

    /**
     * @param wikiId if the the wiki
     * @return the list of all the members (global users) or null if something failed.
     */
    public Collection<String> getMembers(String wikiId)
    {
        try {
            return wikiUserManager.getMembers(wikiId);
        } catch (WikiUserManagerException e) {
            return null;
        }
    }

    /**
     * To know if a user is a member of a wiki.
     *
     * @param userId Id of the user
     * @param wikiId Id of the wiki
     * @return if the user is a member of the specified wiki or null if some problems occur
     */
    public Boolean isMember(String userId, String wikiId)
    {
        try {
            return wikiUserManager.isMember(userId, wikiId);
        } catch (WikiUserManagerException e) {
            // Todo
            return null;
        }
    }

    /**
     * Add a user as a member.
     *
     * @param userId UserID to add
     * @param wikiId Id of the wiki
     * @return true if it succeed
     */
    public boolean addMember(String userId, String wikiId)
    {
        XWikiContext xcontext = xcontextProvider.get();
        try {
            // Check if the current script has the programing rights
            authorizationManager.checkAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
                    xcontext.getDoc().getDocumentReference());
            // Check the right
            authorizationManager.checkAccess(Right.ADMIN, xcontext.getUserReference(), new WikiReference(wikiId));
            // Add the member
            wikiUserManager.addMember(userId, wikiId);
        } catch (AccessDeniedException e) {
            // TODO
            return false;
        } catch (WikiUserManagerException e) {
            // TODO
            return false;
        }

        return true;
    }

    /**
     * Add a list of users as a members.
     *
     * @param userIds List of userID to add
     * @param wikiId Id of the wiki
     * @return true if it succeed
     */
    public boolean addMembers(Collection<String> userIds, String wikiId)
    {
        XWikiContext xcontext = xcontextProvider.get();
        try {
            // Check if the current script has the programing rights
            authorizationManager.checkAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
                    xcontext.getDoc().getDocumentReference());
            // Check the right
            authorizationManager.checkAccess(Right.ADMIN, xcontext.getUserReference(), new WikiReference(wikiId));
            // Add the member
            wikiUserManager.addMembers(userIds, wikiId);
        } catch (AccessDeniedException e) {
            // TODO
            return false;
        } catch (WikiUserManagerException e) {
            // TODO
            return false;
        }

        return true;
    }

    /**
     * Remove a member.
     *
     * @param userId UserID to remove
     * @param wikiId Id the the wiki
     * @return true if it succeed
     */
    public boolean removeMember(String userId, String wikiId)
    {
        XWikiContext xcontext = xcontextProvider.get();
        try {
            // Check if the current script has the programing rights
            authorizationManager.checkAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
                    xcontext.getDoc().getDocumentReference());
            // Check the right
            authorizationManager.checkAccess(Right.ADMIN, xcontext.getUserReference(), new WikiReference(wikiId));
            // Add the member
            wikiUserManager.removeMember(userId, wikiId);
        } catch (AccessDeniedException e) {
            // TODO
            return false;
        } catch (WikiUserManagerException e) {
            // TODO
            return false;
        }

        return true;
    }

    private boolean canSeeCandidacy(MemberCandidacy candidacy)
    {
        XWikiContext xcontext = xcontextProvider.get();

        // If the user is concerned by the candidacy
        String currentUser = entityReferenceSerializer.serialize(xcontext.getUserReference());
        if (candidacy.getUserId().equals(currentUser)) {
            // Hide the admin private comment
            candidacy.setAdminPrivateComment(null);
            return true;
        }

        // Check if the user is an admin
        return authorizationManager.hasAccess(Right.ADMIN, xcontext.getUserReference(),
                new WikiReference(candidacy.getWikiId()));
    }

    /**
     * Get the specified candidacy.
     *
     * @param wikiId Id of the wiki concerned by the candidacy
     * @param candidacyId id of the candidacy
     * @return the candidacy or null if problems occur
     */
    public MemberCandidacy getCandidacy(String wikiId, int candidacyId)
    {
        // Get the candidacy
        MemberCandidacy candidacy = null;
        try {
            candidacy = wikiUserManager.getCandidacy(wikiId, candidacyId);
            // Check the rights
            if (!canSeeCandidacy(candidacy)) {
                // TODO;
                candidacy = null;
            }
        } catch (WikiUserManagerException e) {
            //TODO
        }

        return candidacy;
    }

    /**
     * Filter from a list of candidacies those that the current user has the right to see.
     * @param candidacies list to filter
     * @return the filtered list
     */
    private Collection<MemberCandidacy> filterAuthorizedCandidacies(Collection<MemberCandidacy> candidacies)
    {
        Collection<MemberCandidacy> authorizedCandidacies = new ArrayList<MemberCandidacy>();

        for (MemberCandidacy candidacy : candidacies) {
            if (canSeeCandidacy(candidacy)) {
                authorizedCandidacies.add(candidacy);
            }
        }

        return authorizedCandidacies;
    }

    /**
     * Get all the invitations to join a wiki.
     *
     * @param wikiId id of the wiki to join
     * @return a list of invitations to join this wiki or null if some problems occur
     */
    public Collection<MemberCandidacy> getAllInvitations(String wikiId)
    {
        try {
            Collection<MemberCandidacy> candidacies = wikiUserManager.getAllInvitations(wikiId);
            return filterAuthorizedCandidacies(candidacies);
        } catch (WikiUserManagerException e) {
            // TODO
            return null;
        }
    }

    /**
     * Get all the join requests for a wiki that the current user has the right to see.
     *
     * @param wikiId id of the wiki to join
     * @return a list of join request for this wiki or null if some problems occur
     */
    public Collection<MemberCandidacy> getAllRequests(String wikiId)
    {
        try {
            Collection<MemberCandidacy> candidacies = wikiUserManager.getAllRequests(wikiId);
            return filterAuthorizedCandidacies(candidacies);
        } catch (WikiUserManagerException e) {
            // TODO
            return null;
        }
    }

    /**
     * Join a wiki.
     *
     * @param userId userId to add to the wiki
     * @param wikiId id of the wiki
     * @return true if it succeed
     */
    public boolean join(String userId, String wikiId)
    {
        // Check if the current user is userId
        XWikiContext xcontext = xcontextProvider.get();
        String currentUser = entityReferenceSerializer.serialize(xcontext.getUserReference());
        if (!userId.equals(currentUser)) {
            //TODO
            return false;
        }

        try {
            wikiUserManager.join(userId, wikiId);
        } catch (WikiUserManagerException e) {
            // TODO
            return false;
        }
        return true;
    }

    /**
     * Leave a wiki.
     *
     * @param userId userId to remove from the wiki
     * @param wikiId id of the wiki
     * @return true if it succeed
     */
    public boolean leave(String userId, String wikiId)
    {
        // Check if the current user is userId
        XWikiContext xcontext = xcontextProvider.get();
        String currentUser = entityReferenceSerializer.serialize(xcontext.getUserReference());
        if (!userId.equals(currentUser)) {
            //TODO
            return false;
        }

        // Leave the wiki
        try {
            wikiUserManager.leave(userId, wikiId);
        } catch (WikiUserManagerException e) {
            // TODO
            return false;
        }

        return true;
    }

    /**
     * Perform a request to join a wiki.
     *
     * @param userId UserID of the requester
     * @param wikiId Id of the wiki to join
     * @param message Message that motivates the request
     * @return the generated candidacy
     */
    public MemberCandidacy askToJoin(String userId, String wikiId, String message)
    {
        try {
            return wikiUserManager.askToJoin(userId, wikiId, message);
        } catch (WikiUserManagerException e) {
            // TODO
            return null;
        }
    }

    /**
     * Accept the request to join the wiki.
     *
     * @param request request to accept
     * @param message message about the acceptance
     * @param privateComment private comment that only the administrator can see
     * @return true if it succeed
     */
    public boolean acceptRequest(MemberCandidacy request, String message, String privateComment)
    {
        try {
            // Check if the current user is userId and if the current script has the programing rights
            XWikiContext xcontext = xcontextProvider.get();
            authorizationManager.checkAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
                    xcontext.getDoc().getDocumentReference());
            authorizationManager.checkAccess(Right.ADMIN,
                    xcontext.getUserReference(), new WikiReference(request.getWikiId()));
            // Do the job
            wikiUserManager.acceptRequest(request, message, privateComment);
            return true;
        } catch (WikiUserManagerException e) {
            // TODO
        } catch (AccessDeniedException e) {
            // TODO
        }

        return false;
    }

    /**
     * Refuse the request to join the wiki.
     *
     * @param request request to refuse
     * @param message message about the refusal
     * @param privateComment private comment that only the administrator can see
     * @return true if it succeed
     */
    public boolean refuseRequest(MemberCandidacy request, String message, String privateComment)
    {
        try {
            // Check if the current user is userId and if the current script has the programing rights
            XWikiContext xcontext = xcontextProvider.get();
            authorizationManager.checkAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
                    xcontext.getDoc().getDocumentReference());
            authorizationManager.checkAccess(Right.ADMIN,
                    xcontext.getUserReference(), new WikiReference(request.getWikiId()));
            // Do the job
            wikiUserManager.refuseRequest(request, message, privateComment);
            return true;
        } catch (WikiUserManagerException e) {
            // TODO
        } catch (AccessDeniedException e) {
            // TODO
        }

        return false;
    }

    /**
     * Cancel a candidacy.
     *
     * @param candidacy Candidacy to cancel
     * @return true if it succeed
     */
    public boolean cancelCandidacy(MemberCandidacy candidacy)
    {
        try {
            // Check if the current user is userId and if the current script has the programing rights
            XWikiContext xcontext = xcontextProvider.get();
            authorizationManager.checkAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
                    xcontext.getDoc().getDocumentReference());

            String currentUser = entityReferenceSerializer.serialize(xcontext.getUserReference());
            if (!candidacy.getUserId().equals(currentUser)) {
                authorizationManager.checkAccess(Right.ADMIN,
                    xcontext.getUserReference(), new WikiReference(candidacy.getWikiId()));
            }
            // Do the job
            wikiUserManager.cancelCandidacy(candidacy);
            return true;
        } catch (WikiUserManagerException e) {
            // TODO
        } catch (AccessDeniedException e) {
            // TODO
        }

        return false;
    }

    /**
     * Invite a global user to a wiki.
     *
     * @param userId Id of the user to add
     * @param wikiId Id of the wiki to join
     * @param message MemberCandidacy message
     * @return The generated invitation or null if problems occur
     */
    public MemberCandidacy invite(String userId, String wikiId, String message)
    {
        // Invite
        try {
            // Check if the current user is userId and if the current script has the programing rights
            XWikiContext xcontext = xcontextProvider.get();
            authorizationManager.checkAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
                    xcontext.getDoc().getDocumentReference());
            authorizationManager.checkAccess(Right.ADMIN, xcontext.getUserReference(), new WikiReference(wikiId));
            // Do the job
            return wikiUserManager.invite(userId, wikiId, message);
        } catch (WikiUserManagerException e) {
            // TODO
        } catch (AccessDeniedException e) {
            // TODO
        }
        return null;
    }

    /**
     * Accept the invitation to join a wiki.
     *
     * @param invitation invitation to accept
     * @param message message that goes along the acceptance
     * @return true if it succeed
     */
    public boolean acceptInvitation(MemberCandidacy invitation, String message)
    {
        // Check if the current script has the programing rights
        XWikiContext xcontext = xcontextProvider.get();
        if (!authorizationManager.hasAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
            xcontext.getDoc().getDocumentReference())) {
            return false;
        }

        // Check right
        if (!canSeeCandidacy(invitation)) {
            // TODO
            return false;
        }

        // Accept invitation
        try {
            wikiUserManager.acceptInvitation(invitation, message);
        } catch (WikiUserManagerException e) {
            // TODO
            return false;
        }

        return true;
    }

    /**
     * Refuse the invitation to join a wiki.
     *
     * @param invitation invitation to refuse
     * @param message message that goes along the refusal
     * @return true if it succeed
     */
    public boolean refuseInvitation(MemberCandidacy invitation, String message)
    {
        // Check if the current script has the programing rights
        XWikiContext xcontext = xcontextProvider.get();
        if (!authorizationManager.hasAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
            xcontext.getDoc().getDocumentReference())) {
            return false;
        }

        // Check right
        if (!canSeeCandidacy(invitation)) {
            // TODO
            return false;
        }

        // Accept invitation
        try {
            wikiUserManager.refuseInvitation(invitation, message);
        } catch (WikiUserManagerException e) {
            // TODO
            return false;
        }

        return true;
    }
}
