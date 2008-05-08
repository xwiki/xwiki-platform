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
package com.xpn.xwiki.plugin.invitationmanager.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.plugin.invitationmanager.api.Invitation;
import com.xpn.xwiki.plugin.invitationmanager.api.InvitationManager;
import com.xpn.xwiki.plugin.invitationmanager.api.InvitationManagerException;
import com.xpn.xwiki.plugin.invitationmanager.api.MembershipRequest;

/**
 * API for {@link InvitationManagerPlugin}
 * 
 * @version $Id: $
 */
public class InvitationManagerPluginApi extends PluginApi
{
    public InvitationManagerPluginApi(InvitationManagerPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    protected InvitationManager getInvitationManager()
    {
        return ((InvitationManagerPlugin) getProtectedPlugin()).getInvitationManager();
    }

    /**
     * Requests a space membership in the name of the current logged-in user.
     * 
     * @param space A space to join.
     * @param message A message to the administrators of the space, explaining the request
     */
    public void requestMembership(String space, String message) throws InvitationManagerException
    {
        getInvitationManager().requestMembership(space, message, context);
    }

    /**
     * @see #requestMembership(String, String)
     * @param role The role the requester would like to have in the space, provided he is accepted
     */
    public void requestMembership(String space, String message, String role)
        throws InvitationManagerException
    {
        getInvitationManager().requestMembership(space, message, role, context);
    }

    /**
     * @see #requestMembership(String, String, String)
     * @param roles The list of roles the requester would like to have in the space, provided he is
     *            accepted
     */
    public void requestMembership(String space, String message, List roles)
        throws InvitationManagerException
    {
        getInvitationManager().requestMembership(space, message, roles, context);
    }

    /**
     * @see #requestMembership(String, String, List)
     * @param map A map of additional parameters for the membership request
     */
    public void requestMembership(String space, String message, List roles, Map map)
        throws InvitationManagerException
    {
        getInvitationManager().requestMembership(space, message, roles, map, context);
    }

    /**
     * Returns all the membership requests that match the given prototype. Only the not null fields
     * of the request prototype are considered for matching.
     * 
     * @param prototype A membership request prototype to match
     * @return A list of membership requests
     */
    public List getMembershipRequests(MembershipRequest prototype)
    {
        return getMembershipRequests(prototype, 0, 0);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests(MembershipRequest)
     */
    public List getMembershipRequests(MembershipRequest prototype, int start, int count)
    {
        if (hasProgrammingRights()) {
            return getInvitationManager().getMembershipRequests(prototype, start, count, context);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Returns the list of membership requests that have been sent to the specified space,
     * disregarding the status they have or the role they address.
     * 
     * @param space The space for which to retrieve the membership requests
     * @return A list of membership requests
     */
    public List getMembershipRequests(String space)
    {
        return getInvitationManager().getMembershipRequests(space, context);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests(String)
     */
    public List getMembershipRequests(String space, int start, int count)
    {
        return getInvitationManager().getMembershipRequests(space, start, count, context);
    }

    /**
     * Returns the list of membership requests that have been sent to the specified space and have
     * the specified status, disregarding the role they address.
     * 
     * @param space The space for which to retrieve the membership requests
     * @param status The status the requests must have
     * @return A list of membership requests
     */
    public List getMembershipRequests(String space, int status)
    {
        return getInvitationManager().getMembershipRequests(space, status, context);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests(String, int)
     */
    public List getMembershipRequests(String space, int status, int start, int count)
    {
        return getInvitationManager().getMembershipRequests(space, status, start, count, context);
    }

    /**
     * Returns the list of membership requests that have been sent to the specified space addressing
     * the specified role, disregarding the status they have.
     * 
     * @param space The space for which to retrieve the membership requests
     * @param role The role the requests must address
     * @return A list of membership requests
     */
    public List getMembershipRequests(String space, String role)
    {
        return getInvitationManager().getMembershipRequests(space, role, context);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests(String, String)
     */
    public List getMembershipRequests(String space, String role, int start, int count)
    {
        return getInvitationManager().getMembershipRequests(space, role, start, count, context);
    }

    /**
     * Returns the list of membership requests that have been sent to the specified space, having
     * the specified status and addressing the specified role.
     * 
     * @param space The space for which to retrieve the membership requests
     * @param status The status the requests must have
     * @param role The role the requests must address
     * @return A list of membership requests
     */
    public List getMembershipRequests(String space, int status, String role)
    {
        return getInvitationManager().getMembershipRequests(space, status, role, context);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests(String, int, String)
     */
    public List getMembershipRequests(String space, int status, String role, int start, int count)
    {
        return getInvitationManager().getMembershipRequests(space, status, role, start, count,
            context);
    }

    /**
     * Returns the membership requests of the currently logged-in user, disregarding their status
     * and the role they address.
     * 
     * @return A list of membership requests
     */
    public List getMembershipRequests()
    {
        return getInvitationManager().getMembershipRequests(context);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests()
     */
    public List getMembershipRequests(int start, int count)
    {
        return getInvitationManager().getMembershipRequests(start, count, context);
    }

    /**
     * Returns the membership requests of the currently logged-in user, having the specified status
     * but disregarding the role they address.
     * 
     * @param status The status the membership requests must have
     * @return A list of membership requests
     */
    public List getMembershipRequests(int status)
    {
        return getInvitationManager().getMembershipRequests(status, context);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getMembershipRequests(int)
     */
    public List getMembershipRequests(int status, int start, int count)
    {
        return getInvitationManager().getMembershipRequests(status, start, count, context);
    }

    /**
     * Accept a pending membership request. Actually add the user to the space group, update the
     * membership request object, and notify the concerned user with an email based on the space
     * default template email for membership acceptance. If the context user is not an administrator
     * of the space, does nothing and log a warning in the context
     */
    public boolean acceptMembership(String space, String userName)
        throws InvitationManagerException
    {
        if (hasProgrammingRights()) {
            return getInvitationManager().acceptMembership(space, userName, context);
        }
        return false;
    }

    /**
     * Accept a pending membership using a custom email template which can differ from the
     * space/wiki default one. If the context user is not an administrator of the space, does
     * nothing and log a warning in the context.
     * 
     * @see #acceptMembership(String, String)
     */
    public boolean acceptMembership(String space, String userName, String templateMail)
        throws InvitationManagerException
    {
        if (hasProgrammingRights()) {
            getInvitationManager().acceptMembership(space, userName, templateMail, context);
        }
        return false;
    }

    /**
     * Reject a pending membership request. Update the membership request object, and notify the
     * concerned user with an email based on the space default template for membership rejections.
     * If the context user is not an administrator of the space, does nothing and log a warning in
     * the context.
     */
    public boolean rejectMembership(String space, String userName)
    {
        context.remove("InvitationManagerException");
        if (hasProgrammingRights()) {
            try {
                getInvitationManager().rejectMembership(space, userName, context);
                return true;
            } catch (InvitationManagerException e) {
                context.put("InvitationManagerException", e);
                return false;
            }
        }
        return false;
    }

    /**
     * Reject a pending membership request using a custom email template, which can differ from the
     * space/wiki default one. If the context user is not an administrator of the space, does
     * nothing and log a warning in the context.
     * 
     * @see #rejectMembership(String, String)
     */
    public boolean rejectMembership(String space, String userName, String templateMail)
    {
        context.remove("InvitationManagerException");
        if (hasProgrammingRights()) {
            try {
                getInvitationManager().rejectMembership(space, userName, templateMail, context);
                return true;
            } catch (InvitationManagerException e) {
                context.put("InvitationManagerException", e);
                return false;
            }
        }
        return false;
    }

    /**
     * The currently logged-in user cancels the pending membership request which he sent to join the
     * specified space. Only the user who sent the membership request can cancel it.
     * 
     * @param space The space for which to cancel the membership request
     */
    public void cancelMembershipRequest(String space) throws InvitationManagerException
    {
        getInvitationManager().cancelMembershipRequest(space, context);
    }

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
     */
    public boolean inviteUser(String user, String space, boolean open)
    {
        context.remove("InvitationManagerException");
        if (hasProgrammingRights()) {
            try {
                getInvitationManager().inviteUser(user, space, open, context);
                return true;
            } catch (InvitationManagerException e) {
                context.put("InvitationManagerException", e);
                return false;
            }
        }
        return false;
    }

    /**
     * @param role The role the user will have in the space, provided he accepts the invitation
     * @see #inviteUser(String, String)
     */
    public boolean inviteUser(String user, String space, boolean open, String role)
    {
        context.remove("InvitationManagerException");
        if (hasProgrammingRights()) {
            try {
                getInvitationManager().inviteUser(user, space, open, role, context);
                return true;
            } catch (InvitationManagerException e) {
                context.put("InvitationManagerException", e);
                return false;
            }
        }
        return false;
    }

    /**
     * @param roles The list of roles the user will have in the space, provided he accepts the
     *            invitation
     * @see #inviteUser(String, String)
     */
    public boolean inviteUser(String user, String space, boolean open, List roles)
    {
        context.remove("InvitationManagerException");
        if (hasProgrammingRights()) {
            try {
                getInvitationManager().inviteUser(user, space, open, roles, context);
                return true;
            } catch (InvitationManagerException e) {
                context.put("InvitationManagerException", e);
                return false;
            }
        }
        return false;
    }

    /**
     * @param templateMail Custom e-mail template
     * @see #inviteUser(String, String, List)
     */
    public boolean inviteUser(String user, String space, boolean open, List roles,
        String templateMail)
    {
        context.remove("InvitationManagerException");
        if (hasProgrammingRights()) {
            try {
                getInvitationManager()
                    .inviteUser(user, space, open, roles, templateMail, context);
                return true;
            } catch (InvitationManagerException e) {
                context.put("InvitationManagerException", e);
                return false;
            }
        }
        return false;
    }

    /**
     * @param map A map of additional parameters for the invitation
     * @see #inviteUser(String, String, List, String)
     */
    public boolean inviteUser(String user, String space, boolean open, List roles,
        String templateMail, Map map)
    {
        context.remove("InvitationManagerException");
        if (hasProgrammingRights()) {
            try {
                getInvitationManager().inviteUser(user, space, open, roles, templateMail, map,
                    context);
                return true;
            } catch (InvitationManagerException e) {
                context.put("InvitationManagerException", e);
                return false;
            }
        }
        return false;
    }

    /**
     * The current logged-in user verifies the invitation to join the specified space.
     * 
     * @param space The space the user accepts to join
     */
    public boolean verifyInvitation(String space) throws InvitationManagerException
    {
        return getInvitationManager().verifyInvitation(space, context);
    }

    /**
     * The current logged-in user verifies the invitation to join the specified space, using an
     * email address and a code.
     * 
     * @param space The space the user accepts to join
     * @param email The e-mail where the invitation was sent
     * @param code The code of the invitation, when it was sent to a single person (e.g. the e-mail
     *            address to which the invitation was sent is not a mailing list)
     */
    public boolean verifyInvitation(String space, String email, String code)
        throws InvitationManagerException
    {
        return getInvitationManager().verifyInvitation(space, email, code, context);
    }

    /**
     * The current logged-in user accepts the invitation to join the specified space.
     * 
     * @param space The space the user accepts to join
     */
    public boolean acceptInvitation(String space) throws InvitationManagerException
    {
        return getInvitationManager().acceptInvitation(space, context);
    }

    /**
     * The current logged-in user accepts the invitation to join the specified space, using an email
     * address and a code. This can throw an error if the user is already member.
     * 
     * @param space The space the user accepts to join
     * @param email The e-mail where the invitation was sent
     * @param code The code of the invitation, when it was sent to a single person (e.g. the e-mail
     *            address to which the invitation was sent is not a mailing list)
     */
    public boolean acceptInvitation(String space, String email, String code)
        throws InvitationManagerException
    {
        return getInvitationManager().acceptInvitation(space, email, code, context);
    }

    /**
     * The currently logged-in user rejects the invitation to join the specified space
     * 
     * @param space The space the user refuses to join
     */
    public void rejectInvitation(String space) throws InvitationManagerException
    {
        getInvitationManager().rejectInvitation(space, context);
    }

    /**
     * Rejects the invitation to join the specified space, which was sent to the specified e-mail
     * address.
     * 
     * @param space The space the user refuses to join
     * @param email The e-mail address where the invitation was sent
     * @param code The code of the invitation
     */
    public void rejectInvitation(String space, String email, String code)
        throws InvitationManagerException
    {
        getInvitationManager().rejectInvitation(space, email, code, context);
    }

    /**
     * Cancels the pending invitation which was sent to the specified user to join the specified
     * space. Only the administrator of that space can cancel the invitation.
     * 
     * @param user Wikiname for a registered user or e-mail address for a unregistered user
     * @param space The space for which to cancel the invitation
     */
    public boolean cancelInvitation(String user, String space)
    {
        context.remove("InvitationManagerException");
        if (hasProgrammingRights()) {
            try {
                getInvitationManager().cancelInvitation(user, space, context);
                return true;
            } catch (InvitationManagerException e) {
                context.put("InvitationManagerException", e);
                return false;
            }
        }
        return false;
    }

    /**
     * Returns all the invitation that match the given prototype.
     * 
     * @param prototype The invitation prototype to match
     * @return A list of invitations
     */
    public List getInvitations(Invitation prototype)
    {
        return getInvitations(prototype, 0, 0);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(Invitation)
     */
    public List getInvitations(Invitation prototype, int start, int count)
    {
        if (hasProgrammingRights()) {
            return getInvitationManager().getInvitations(prototype, start, count, context);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Returns all the invitations to join the specified space that have been sent by space members,
     * disregarding their status.
     * 
     * @param space The space for which to retrieve the invitations
     * @return A list of invitations
     */
    public List getInvitations(String space)
    {
        return getInvitationManager().getInvitations(space, context);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(String)
     */
    public List getInvitations(String space, int start, int count)
    {
        return getInvitationManager().getInvitations(space, start, count, context);
    }

    /**
     * Returns all the invitations to join the specified space that have the given status.
     * 
     * @param space The space for which to retrieve the invitations
     * @param status The status the invitations must have
     * @return A list of invitations
     */
    public List getInvitations(String space, int status)
    {
        return getInvitationManager().getInvitations(space, status, context);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(String, int)
     */
    public List getInvitations(String space, int status, int start, int count)
    {
        return getInvitationManager().getInvitations(space, status, start, count, context);
    }

    /**
     * Returns all the invitations to join the specified space that address the given role.
     * 
     * @param space The space for which to retrieve the invitations
     * @param role The role the invitations must address
     * @return A list of invitations
     */
    public List getInvitations(String space, String role)
    {
        return getInvitationManager().getInvitations(space, role, context);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(String, String)
     */
    public List getInvitations(String space, String role, int start, int count)
    {
        return getInvitationManager().getInvitations(space, role, start, count, context);
    }

    /**
     * Returns all the invitations to join the specified space that have the specified status and
     * address the specified role.
     * 
     * @param space The space for which to retrieve the invitations
     * @param status The status the invitations must have
     * @param role The role the invitations must address
     * @return A list of invitations
     */
    public List getInvitations(String space, int status, String role)
    {
        return getInvitationManager().getInvitations(space, status, role, context);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(String, int, String)
     */
    public List getInvitations(String space, int status, String role, int start, int count)
    {
        return getInvitationManager().getInvitations(space, status, role, start, count, context);
    }

    /**
     * Returns all the invitations received by the currently logged-in user, disregarding their
     * status.
     * 
     * @return A list of invitations
     */
    public List getInvitations()
    {
        return getInvitationManager().getInvitations(context);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(String)
     */
    public List getInvitations(int start, int count)
    {
        return getInvitationManager().getInvitations(start, count, context);
    }

    /**
     * Returns all the invitations received by the currently logged-in user which have the specified
     * status.
     * 
     * @param status The status the invitations must have
     * @return A list of invitations
     */
    public List getInvitations(int status)
    {
        return getInvitationManager().getInvitations(status, context);
    }

    /**
     * @param start The index of the first item to return
     * @param count The maximum number of items to return
     * @see #getInvitations(String, int)
     */
    public List getInvitations(int status, int start, int count)
    {
        return getInvitationManager().getInvitations(status, start, count, context);
    }
}
