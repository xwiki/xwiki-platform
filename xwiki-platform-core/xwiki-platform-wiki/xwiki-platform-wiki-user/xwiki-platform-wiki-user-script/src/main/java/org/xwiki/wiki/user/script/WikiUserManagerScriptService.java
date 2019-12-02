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
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
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
public class WikiUserManagerScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String WIKIUSERERROR_KEY = "scriptservice.wiki.user.error";

    @Inject
    private WikiUserManager wikiUserManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;
    
    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(WIKIUSERERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setLastError(Exception e)
    {
        this.execution.getContext().setProperty(WIKIUSERERROR_KEY, e);
    }

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
            setLastError(e);
            return null;
        }
    }

    /**
     * Check that all required permissions are respected by both the script and the user.
     *
     * @param wikiId the id of the wiki concerned by the operation
     *
     * @throws AccessDeniedException if the permissions are not respected
     */
    private void checkRights(String wikiId) throws AccessDeniedException
    {
        checkRights(wikiId, null);
    }

    /**
     * Check that all required permissions are respected by both the script and the user.
     *
     * @param wikiId the id of the wiki concerned by the operation
     * @param user the user concerned by the operation
     *
     * @throws AccessDeniedException if the permissions are not respected
     */
    private void checkRights(String wikiId, DocumentReference user) throws AccessDeniedException
    {
        XWikiContext context = xcontextProvider.get();

        // Does the script author have the admin right?
        //
        // The goal is to avoid that a non-granted user writes a script, which could be executed by an administrator,
        // which uses this script service to perform "nasty" operations, like being invited to a sub-wiki.
        //
        // By the past, we checked for the programing right, but it was too restrictive, as it make impossible to
        // a user without programing rights to create a wiki and then invite some peoples in it.
        authorizationManager.checkAccess(Right.ADMIN, context.getDoc().getAuthorReference(),
                context.getDoc().getDocumentReference());

        // Is the user concerned by the operation?
        if (user != null && user.equals(context.getUserReference())) {
            // If the user is concerned, then she has the right to perform this operation.
            return;
        }

        // Does the current user have the admin right?
        authorizationManager.checkAccess(Right.ADMIN, context.getUserReference(), new WikiReference(wikiId));
    }

    /**
     * Check that all required permissions are respected by both the script and the user concerned by a candidacy.
     *
     * @param candidacy the candidacy concerned by the operation
     *
     * @throws AccessDeniedException if the permissions are not respected
     */
    private void checkRights(MemberCandidacy candidacy) throws AccessDeniedException
    {
        checkRights(candidacy.getWikiId(), documentReferenceResolver.resolve(candidacy.getUserId()));
    }
            

    /**
     * @param wikiId Id of the wiki to change
     * @param scope scope to set
     * @return true if it succeed
     */
    public boolean setUserScope(String wikiId, String scope)
    {
        try {
            checkRights(wikiId);
            wikiUserManager.setUserScope(wikiId, UserScope.valueOf(scope.toUpperCase()));
        } catch (WikiUserManagerException | AccessDeniedException | IllegalArgumentException e) {
            setLastError(e);
            return false;
        }
        
        return true;
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
            setLastError(e);
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
        try {
            checkRights(wikiId);
            wikiUserManager.setMembershipType(wikiId, MembershipType.valueOf(type.toUpperCase()));
        } catch (WikiUserManagerException | AccessDeniedException | IllegalArgumentException e) {
            setLastError(e);
            return false;
        }
        
        return true;
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
            setLastError(e);
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
            setLastError(e);
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
        try {
            checkRights(wikiId);
            wikiUserManager.addMember(userId, wikiId);
        } catch (AccessDeniedException | WikiUserManagerException e) {
            setLastError(e);
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
        try {
            checkRights(wikiId);
            wikiUserManager.addMembers(userIds, wikiId);
        } catch (AccessDeniedException | WikiUserManagerException e) {
            setLastError(e);
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
        try {
            checkRights(wikiId);
            wikiUserManager.removeMember(userId, wikiId);
        } catch (AccessDeniedException | WikiUserManagerException e) {
            setLastError(e);
            return false;
        }

        return true;
    }

    private boolean canSeeCandidacy(MemberCandidacy candidacy)
    {
        XWikiContext context = xcontextProvider.get();

        // Test if the user is concerned by the candidacy...
        DocumentReference candidacyUser = documentReferenceResolver.resolve(candidacy.getUserId());
        DocumentReference userReference = context.getUserReference();

        // userReference can be null in case of guest user
        if (userReference != null && userReference.equals(candidacyUser)) {
            // Hide the admin private comment
            candidacy.setAdminPrivateComment(null);
            return true;
        }

        // Otherwise the user must be an admin.
        return authorizationManager.hasAccess(Right.ADMIN, userReference, new WikiReference(candidacy.getWikiId()));
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
                setLastError(new WikiUserManagerScriptServiceException("You are not allowed to see this candidacy."));
                candidacy = null;
            }
        } catch (WikiUserManagerException e) {
            setLastError(e);
        }

        return candidacy;
    }

    /**
     * Filter from a list of candidacies those that the current user has the right to see.
     *  
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
            setLastError(e);
            return null;
        }
    }

    /**
     * @param user DocumentReference to the user to test
     * @param wikiId id of the wiki to test
     * @return either or not the user has a pending invitation to join the wiki, null if some problems occur
     */
    public Boolean hasPendingInvitation(DocumentReference user, String wikiId)
    {
        // Guest users never have pending invitations!
        if (user == null) {
            return false;
        }

        try {
            checkRights(wikiId, user);
            return wikiUserManager.hasPendingInvitation(user, wikiId);
        } catch (AccessDeniedException | WikiUserManagerException e) {
            setLastError(e);
        }

        return null;
    }

    /**
     * @param user DocumentReference to the user to test
     * @param wikiId id of the wiki to test
     * @return either or not the user has a pending request to join the wiki, null if some problems occur
     */
    public Boolean hasPendingRequest(DocumentReference user, String wikiId)
    {
        // Guest users never have pending requests!
        if (user == null) {
            return false;
        }

        try {
            checkRights(wikiId, user);
            return wikiUserManager.hasPendingRequest(user, wikiId);
        } catch (AccessDeniedException | WikiUserManagerException e) {
            setLastError(e);
        }

        return null;
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
            setLastError(e);
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
        XWikiContext context = xcontextProvider.get();
        DocumentReference candidacyUser = documentReferenceResolver.resolve(userId);
        if (!context.getUserReference().equals(candidacyUser)) {
            setLastError(new WikiUserManagerException(String.format("User [%s] cannot call "
                    + "$services.wiki.user.join() with an other userId.", context.getUserReference())));
            return false;
        }

        try {
            wikiUserManager.join(userId, wikiId);
        } catch (WikiUserManagerException e) {
            setLastError(e);
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
        XWikiContext context = xcontextProvider.get();
        DocumentReference candidacyUser = documentReferenceResolver.resolve(userId);
        if (!context.getUserReference().equals(candidacyUser)) {
            setLastError(new WikiUserManagerException(String.format("User [%s] cannot call $services.wiki.user.leave()"
                    + " with an other userId.", context.getUserReference())));
            return false;
        }

        // Leave the wiki
        try {
            wikiUserManager.leave(userId, wikiId);
        } catch (WikiUserManagerException e) {
            setLastError(e);
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
            checkRights(wikiId, documentReferenceResolver.resolve(userId));
            return wikiUserManager.askToJoin(userId, wikiId, message);
        } catch (AccessDeniedException | WikiUserManagerException e) {
            setLastError(e);
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
            checkRights(request);
            wikiUserManager.acceptRequest(request, message, privateComment);
            return true;
        } catch (AccessDeniedException | WikiUserManagerException e) {
            setLastError(e);
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
            checkRights(request);
            wikiUserManager.refuseRequest(request, message, privateComment);
            return true;
        } catch (AccessDeniedException | WikiUserManagerException e) {
            setLastError(e);
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
            checkRights(candidacy);
            wikiUserManager.cancelCandidacy(candidacy);
            return true;
        } catch (AccessDeniedException | WikiUserManagerException e) {
            setLastError(e);
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
        try {
            checkRights(wikiId);
            return wikiUserManager.invite(userId, wikiId, message);
        } catch (AccessDeniedException | WikiUserManagerException e) {
            setLastError(e);
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
        try {
            checkRights(invitation);
            wikiUserManager.acceptInvitation(invitation, message);
            return true;
        } catch (AccessDeniedException | WikiUserManagerException e) {
            setLastError(e);
        }
        
        return false;
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
        try {
            checkRights(invitation);
            wikiUserManager.refuseInvitation(invitation, message);
            return true;
        } catch (AccessDeniedException | WikiUserManagerException e) {
            setLastError(e);
        }

        return false;
    }
}
