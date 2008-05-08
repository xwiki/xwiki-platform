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
package com.xpn.xwiki.plugin.invitationmanager.api;

import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Manages invitations and membership requests
 * 
 * @version $Id: $
 */
public interface InvitationManager
{
    public static String DEFAULT_RESOURCE_SPACE = "InvitationManagerResources";

    String DEFAULT_INVITATIONS_SPACE_SUFFIX = "_Invitations";

    /**
     * Requests a space membership in the name of the current logged-in user. Registers the request
     * in the database, and possibly notify space administrators of the request
     * 
     * @param space A space to join.
     * @param message A message to the administrators of the space, explaining the request
     * @param context A XWikiContext instance
     * @see MembershipRequest
     */
    void requestMembership(String space, String message, XWikiContext context)
        throws InvitationManagerException;

    /**
     * @see #requestMembership(String, String, XWikiContext)
     * @param role The role the requester would like to have in the space, provided he is accepted
     */
    void requestMembership(String space, String message, String role, XWikiContext context)
        throws InvitationManagerException;

    /**
     * @see #requestMembership(String, String, String, XWikiContext)
     * @param roles The list of roles the requester would like to have in the space, provided he is
     *            accepted
     */
    void requestMembership(String space, String message, List roles, XWikiContext context)
        throws InvitationManagerException;

    /**
     * @see #requestMembership(String, String, List, XWikiContext)
     * @param map A map of additional parameters for the membership request
     */
    void requestMembership(String space, String message, List roles, Map map, XWikiContext context)
        throws InvitationManagerException;

    /**
     * Returns all the membership requests that match the given prototype. Only the not null fields
     * of the request prototype are considered for matching.
     * 
     * @param prototype A membership request prototype to match
     * @param context A XWikiContext instance
     * @return A list of membership requests
     */
    List getMembershipRequests(MembershipRequest prototype, XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests(MembershipRequest, XWikiContext)
     */
    List getMembershipRequests(MembershipRequest prototype, int start, int count,
        XWikiContext context);

    /**
     * Returns the list of membership requests that have been sent to the specified space,
     * disregarding the status they have or the role they address.
     * 
     * @param space The space for which to retrieve the membership requests
     * @param context A XWikiContext instance
     * @return A list of membership requests
     */
    List getMembershipRequests(String space, XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests(String, XWikiContext)
     */
    List getMembershipRequests(String space, int start, int count, XWikiContext context);

    /**
     * Returns the list of membership requests that have been sent to the specified space and have
     * the specified status, disregarding the role they address.
     * 
     * @param space The space for which to retrieve the membership requests
     * @param status The status the requests must have
     * @param context A XWikiContext instance
     * @return A list of membership requests
     */
    List getMembershipRequests(String space, int status, XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests(String, int, XWikiContext)
     */
    List getMembershipRequests(String space, int status, int start, int count,
        XWikiContext context);

    /**
     * Returns the list of membership requests that have been sent to the specified space addressing
     * the specified role, disregarding the status they have.
     * 
     * @param space The space for which to retrieve the membership requests
     * @param role The role the requests must address
     * @param context A XWikiContext instance
     * @return A list of membership requests
     */
    List getMembershipRequests(String space, String role, XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests(String, String, XWikiContext)
     */
    List getMembershipRequests(String space, String role, int start, int count,
        XWikiContext context);

    /**
     * Returns the list of membership requests that have been sent to the specified space, having
     * the specified status and addressing the specified role.
     * 
     * @param space The space for which to retrieve the membership requests
     * @param status The status the requests must have
     * @param role The role the requests must address
     * @param context A XWikiContext instance
     * @return A list of membership requests
     */
    List getMembershipRequests(String space, int status, String role, XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests(String, int, String, XWikiContext)
     */
    List getMembershipRequests(String space, int status, String role, int start, int count,
        XWikiContext context);

    /**
     * Returns the membership requests of the currently logged-in user, disregarding their status
     * and the role they address.
     * 
     * @param context A XWikiContext instance
     * @return A list of membership requests
     */
    List getMembershipRequests(XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests(XWikiContext)
     */
    List getMembershipRequests(int start, int count, XWikiContext context);

    /**
     * Returns the membership requests of the currently logged-in user, having the specified status
     * but disregarding the role they address.
     * 
     * @param status The status the membership requests must have
     * @param context A XWikiContext instance
     * @return A list of membership requests
     */
    List getMembershipRequests(int status, XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests(int, XWikiContext)
     */
    List getMembershipRequests(int status, int start, int count, XWikiContext context);

    /**
     * Accept a pending membership request. Actually add the user to the space group, update the
     * membership request object, and notify the concerned user with an email based on the space
     * default template email for membership acceptance. The default mail could be stored has a
     * WebPreferences property, possibly empty (then, the wiki preference would be used). If the
     * context user is not an administrator of the space, does nothing and log a warning in the
     * context
     */
    boolean acceptMembership(String space, String userName, XWikiContext context)
        throws InvitationManagerException;

    /**
     * Accept a pending membership using a custom email template which can differ from the
     * space/wiki default one. If the context user is not an administrator of the space, does
     * nothing and log a warning in the context.
     * 
     * @see #acceptMembership(String, String, XWikiContext)
     */
    boolean acceptMembership(String space, String userName, String templateMail,
        XWikiContext context) throws InvitationManagerException;

    /**
     * Reject a pending membership request. Update the membership request object, and notify the
     * concerned user with an email based on the space default template for membership rejections.
     * The default mail could be stored has a XWikiPreferences property, possibly empty (then, the
     * wiki preference would be used). If the context user is not an administrator of the space,
     * does nothing and log a warning in the context.
     */
    void rejectMembership(String space, String userName, XWikiContext context)
        throws InvitationManagerException;

    /**
     * Reject a pending membership request using a custom email template, which can differ from the
     * space/wiki default one. If the context user is not an administrator of the space, does
     * nothing and log a warning in the context.
     * 
     * @see #rejectMembership(String, String, XWikiContext)
     */
    void rejectMembership(String space, String userName, String templateMail, XWikiContext context)
        throws InvitationManagerException;

    /**
     * The currently logged-in user cancels the pending membership request which he sent to join the
     * specified space. Only the user who sent the membership request can cancel it.
     * 
     * @param space The space for which to cancel the membership request
     * @param context A XWikiContext instance
     */
    void cancelMembershipRequest(String space, XWikiContext context)
        throws InvitationManagerException;

    /**
     * Invites a user to join the space. It can be a registered user, in which case the
     * <code>user</code> parameter must be a valid wikiname, or an unregistered user, in which
     * case the <code>user</code> parameter must be a valid e-mail address. If space is
     * <code>null</code> this is a global invitation to join the wiki. Creates an invitation
     * object, filled with the wikiname/e-mail of the invited user. Sends an invitation mail, using
     * the default template mail for invitations. The default mail to be used could be stored as a
     * XWikiPreferences property. If the context user is not an administrator of the space, does
     * nothing and log a warning in the context.
     * 
     * @param user Wikiname for registered users and e-mail for unregistered users
     * @param space The space to join. If space is <code>null</code> this is a global invitation
     *            to join the wiki
     * @param open <code>true</code> if the invitation is open. In this case the <code>user</code>
     *            should be a mailing list address
     * @param context A XWikiContext instance
     */
    void inviteUser(String user, String space, boolean open, XWikiContext context)
        throws InvitationManagerException;

    /**
     * @param role The role the user will have in the space, provided he accepts the invitation
     * @see #inviteUser(String, String, XWikiContext)
     */
    void inviteUser(String user, String space, boolean open, String role, XWikiContext context)
        throws InvitationManagerException;

    /**
     * @param roles The list of roles the user will have in the space, provided he accepts the
     *            invitation
     * @see #inviteUser(String, String, XWikiContext)
     */
    void inviteUser(String user, String space, boolean open, List roles, XWikiContext context)
        throws InvitationManagerException;

    /**
     * @param templateMail Custom e-mail template
     * @see #inviteUser(String, String, List, XWikiContext)
     */
    void inviteUser(String user, String space, boolean open, List roles, String templateMail,
        XWikiContext context) throws InvitationManagerException;

    /**
     * @param map A map of additional parameters for the invitation
     * @see #inviteUser(String, String, List, String, XWikiContext)
     */
    void inviteUser(String user, String space, boolean open, List roles, String templateMail,
        Map map, XWikiContext context) throws InvitationManagerException;

    /**
     * The current logged-in user verify the invitation to join the specified space.
     * 
     * @param space The space the user accepts to join
     * @param context A XWikiContext instance
     */
    boolean verifyInvitation(String space, XWikiContext context)
        throws InvitationManagerException;

    /**
     * The current logged-in user verifies the invitation to join the specified space, using an
     * email address and a code.
     * 
     * @param space The space the user accepts to join
     * @param email The e-mail where the invitation was sent
     * @param code The code of the invitation, when it was sent to a single person (e.g. the e-mail
     *            address to which the invitation was sent is not a mailing list)
     * @param context A XWikiContext instance
     */
    boolean verifyInvitation(String space, String email, String code, XWikiContext context)
        throws InvitationManagerException;

    /**
     * The current logged-in user accepts the invitation to join the specified space.
     * 
     * @param space The space the user accepts to join
     * @param context A XWikiContext instance
     */
    boolean acceptInvitation(String space, XWikiContext context)
        throws InvitationManagerException;

    /**
     * The current logged-in user accepts the invitation to join the specified space, using an email
     * address and a code. This can throw an error if the user is already member.
     * 
     * @param space The space the user accepts to join
     * @param email The e-mail where the invitation was sent
     * @param code The code of the invitation, when it was sent to a single person (e.g. the e-mail
     *            address to which the invitation was sent is not a mailing list)
     * @param context A XWikiContext instance
     */
    boolean acceptInvitation(String space, String email, String code, XWikiContext context)
        throws InvitationManagerException;

    /**
     * The currently logged-in user rejects the invitation to join the specified space
     * 
     * @param space The space the user refuses to join
     * @param context A XWikiContext instance
     */
    void rejectInvitation(String space, XWikiContext context) throws InvitationManagerException;

    /**
     * Rejects the invitation to join the specified space, which was sent to the specified e-mail
     * address.
     * 
     * @param space The space the user refuses to join
     * @param email The e-mail address where the invitation was sent
     * @param code The code of the invitation
     * @param context A XWikiContext instance
     */
    void rejectInvitation(String space, String email, String code, XWikiContext context)
        throws InvitationManagerException;

    /**
     * Cancels the pending invitation which was sent to the specified user to join the specified
     * space. Only the administrator of that space can cancel the invitation.
     * 
     * @param user Wikiname for a registered user or e-mail address for a unregistered user
     * @param space The space for which to cancel the invitation
     * @param context A XWikiContext instance
     */
    void cancelInvitation(String user, String space, XWikiContext context)
        throws InvitationManagerException;

    /**
     * Returns all the invitation that match the given prototype.
     * 
     * @param prototype The invitation prototype to match
     * @param context A XWikiContext instance
     * @return A list of invitations
     */
    List getInvitations(Invitation prototype, XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(Invitation, XWikiContext)
     */
    List getInvitations(Invitation prototype, int start, int count, XWikiContext context);

    /**
     * Returns all the invitations to join the specified space that have been sent by space members,
     * disregarding their status.
     * 
     * @param space The space for which to retrieve the invitations
     * @param context A XWikiContext instance
     * @return A list of invitations
     */
    List getInvitations(String space, XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(String, XWikiContext)
     */
    List getInvitations(String space, int start, int count, XWikiContext context);

    /**
     * Returns all the invitations to join the specified space that have the given status.
     * 
     * @param space The space for which to retrieve the invitations
     * @param status The status the invitations must have
     * @param context A XWikiContext instance
     * @return A list of invitations
     */
    List getInvitations(String space, int status, XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(String, int, XWikiContext)
     */
    List getInvitations(String space, int status, int start, int count, XWikiContext context);

    /**
     * Returns all the invitations to join the specified space that address the given role.
     * 
     * @param space The space for which to retrieve the invitations
     * @param role The role the invitations must address
     * @param context A XWikiContext instance
     * @return A list of invitations
     */
    List getInvitations(String space, String role, XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(String, String, XWikiContext)
     */
    List getInvitations(String space, String role, int start, int count, XWikiContext context);

    /**
     * Returns all the invitations to join the specified space that have the specified status and
     * address the specified role.
     * 
     * @param space The space for which to retrieve the invitations
     * @param status The status the invitations must have
     * @param role The role the invitations must address
     * @param context A XWikiContext instance
     * @return A list of invitations
     */
    List getInvitations(String space, int status, String role, XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(String, int, String, XWikiContext)
     */
    List getInvitations(String space, int status, String role, int start, int count,
        XWikiContext context);

    /**
     * Returns all the invitations received by the currently logged-in user, disregarding their
     * status.
     * 
     * @param context A XWikiContext instance
     * @return A list of invitations
     */
    List getInvitations(XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(String, XWikiContext)
     */
    List getInvitations(int start, int count, XWikiContext context);

    /**
     * Returns all the invitations received by the currently logged-in user which have the specified
     * status.
     * 
     * @param status The status the invitations must have
     * @param context A XWikiContext instance
     * @return A list of invitations
     */
    List getInvitations(int status, XWikiContext context);

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(String, int, XWikiContext)
     */
    List getInvitations(int status, int start, int count, XWikiContext context);

    /**
     * Creates the classes used by the invitation manager when necessary
     */
    void initClasses(XWikiContext context) throws XWikiException;
}
