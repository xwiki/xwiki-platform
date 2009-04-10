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
package com.xpn.xwiki.plugin.invitationmanager.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.invitationmanager.api.Invitation;
import com.xpn.xwiki.plugin.invitationmanager.api.InvitationManager;
import com.xpn.xwiki.plugin.invitationmanager.api.InvitationManagerException;
import com.xpn.xwiki.plugin.invitationmanager.api.JoinRequest;
import com.xpn.xwiki.plugin.invitationmanager.api.JoinRequestStatus;
import com.xpn.xwiki.plugin.invitationmanager.api.MembershipRequest;
import com.xpn.xwiki.plugin.mailsender.Mail;
import com.xpn.xwiki.plugin.mailsender.MailSenderPlugin;
import com.xpn.xwiki.plugin.spacemanager.api.Space;
import com.xpn.xwiki.plugin.spacemanager.api.SpaceManager;
import com.xpn.xwiki.plugin.spacemanager.api.SpaceManagers;
import com.xpn.xwiki.plugin.spacemanager.api.SpaceUserProfile;
import com.xpn.xwiki.plugin.spacemanager.impl.SpaceUserProfileImpl;
import com.xpn.xwiki.render.XWikiVelocityRenderer;

/**
 * The default implementation for {@link InvitationManager}
 * 
 * @version $Id: $
 */
@SuppressWarnings("unchecked")
public class InvitationManagerImpl implements InvitationManager
{
    public static interface JoinRequestAction
    {
        String CREATE = "Create";

        String SEND = "Send";

        String ACCEPT = "Accept";

        String REJECT = "Reject";
    }

    public static final String USER_PROFILES_DEFAULT = "1";

    public static final String SPACE_VELOCITY_KEY = "space";

    public static final String INVITATION_VELOCITY_KEY = "invitation";

    public static final String MEMBERSHIP_REQUEST_VELOCITY_KEY = "membershiprequest";

    public static final String INVITATION_CLASS_NAME = "XWiki.InvitationClass";

    public static final String MEMBERSHIP_REQUEST_CLASS_NAME = "XWiki.MembershipRequestClass";

    private boolean mailNotification = true;

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#acceptInvitation(String, String, String, XWikiContext)
     */
    public boolean acceptInvitation(String space, String email, String code, XWikiContext context)
        throws InvitationManagerException
    {
        try {
            Invitation invitation = getInvitation(space, email, context);
            if (code.equals(invitation.getCode())
                && invitation.getStatus() == JoinRequestStatus.SENT) {
                if (!invitation.isOpen()) {
                    invitation.setStatus(JoinRequestStatus.ACCEPTED);
                    invitation.setResponseDate(new Date());
                    invitation.setInvitee(context.getUser());
                    invitation.saveWithProgrammingRights();
                }

                if (isWithUserProfiles(context)) {
                    // create and save a new space user profile based on the HTTP request
                    createSpaceUserProfile(space, context);
                }

                // update the list of space members and their roles
                addMember(space, context.getUser(), invitation.getRoles(), context);

                // create a custom invitation for the currently logged-in user
                customizeInvitation(invitation, JoinRequestStatus.ACCEPTED, context);
                return true;
            } else {
                return false;
            }
        } catch (XWikiException e) {
            throw new InvitationManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#acceptInvitation(String, String, String, XWikiContext)
     */
    public boolean verifyInvitation(String space, String email, String code, XWikiContext context)
        throws InvitationManagerException
    {
        try {
            Invitation invitation = getInvitation(space, email, context);
            if (code.equals(invitation.getCode())
                && invitation.getStatus() == JoinRequestStatus.SENT) {
                return true;
            } else {
                return false;
            }
        } catch (XWikiException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#acceptInvitation(String, XWikiContext)
     */
    public boolean acceptInvitation(String space, XWikiContext context)
        throws InvitationManagerException
    {
        try {
            Invitation invitation = getInvitation(space, context.getUser(), context);

            // if the invitation does not exist we need to return false
            if (invitation.isNew())
                return false;

            int status = invitation.getStatus();
            if (status == JoinRequestStatus.SENT || status == JoinRequestStatus.REFUSED) {
                // update the invitation object
                invitation.setResponseDate(new Date());
                invitation.setStatus(JoinRequestStatus.ACCEPTED);

                if (isWithUserProfiles(context)) {
                    // create and save a new space user profile based on the HTTP request
                    createSpaceUserProfile(space, context);
                }

                // update the list of members and their roles
                addMember(space, context.getUser(), invitation.getRoles(), context);
                // save the invitation status
                invitation.saveWithProgrammingRights();
                return true;
            } else {
                return false;
            }
        } catch (XWikiException e) {
            throw new InvitationManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#acceptInvitation(String, String, String, XWikiContext)
     */
    public boolean verifyInvitation(String space, XWikiContext context)
        throws InvitationManagerException
    {
        try {
            Invitation invitation = getInvitation(space, context.getUser(), context);
            if ((invitation != null) && (invitation.getStatus() == JoinRequestStatus.SENT))
                return true;
            else
                return false;
        } catch (XWikiException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#acceptMembership(String, String, String, XWikiContext)
     */
    public boolean acceptMembership(String space, String userName, String templateMail,
        XWikiContext context) throws InvitationManagerException
    {
        try {
            MembershipRequest request = getMembershipRequest(space, userName, context);

            // if the invitation does not exist we need to return false
            if (request.isNew())
                return false;

            int status = request.getStatus();
            if (status == JoinRequestStatus.SENT || status == JoinRequestStatus.REFUSED) {
                // update the membership request object
                request.setResponseDate(new Date());
                request.setResponder(context.getUser());
                request.setStatus(JoinRequestStatus.ACCEPTED);
                // send notification mail
                sendMail(JoinRequestAction.ACCEPT, request, templateMail, context);

                // create and save a new space user profile based on the membership request
                createSpaceUserProfile(space, request, context);

                // update the list of members and their roles
                addMember(space, userName, request.getRoles(), context);

                // save after adding user and sending email
                request.saveWithProgrammingRights();
                return true;
            } else {
                return false;
            }
        } catch (XWikiException e) {
            throw new InvitationManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#acceptMembership(String, String, XWikiContext)
     */
    public boolean acceptMembership(String space, String userName, XWikiContext context)
        throws InvitationManagerException
    {
        String templateMail =
            getDefaultTemplateMailDocumentName(space, "Request", JoinRequestAction.ACCEPT,
                context);
        return acceptMembership(space, userName, templateMail, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#cancelInvitation(String, String, XWikiContext)
     */
    public void cancelInvitation(String userNameOrMail, String space, XWikiContext context)
        throws InvitationManagerException
    {
        try {
            Invitation invitation = getInvitation(space, userNameOrMail, context);
            if (invitation.getStatus() == JoinRequestStatus.SENT) {
                invitation.setStatus(JoinRequestStatus.CANCELLED);
                invitation.saveWithProgrammingRights();
            }
        } catch (XWikiException e) {
            throw new InvitationManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#cancelMembershipRequest(String, XWikiContext)
     */
    public void cancelMembershipRequest(String space, XWikiContext context)
        throws InvitationManagerException
    {
        try {
            MembershipRequest request = getMembershipRequest(space, context.getUser(), context);
            if (request.getStatus() == JoinRequestStatus.SENT) {
                request.setStatus(JoinRequestStatus.CANCELLED);
                request.saveWithProgrammingRights();
            }
        } catch (XWikiException e) {
            throw new InvitationManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(int, int, int, XWikiContext)
     */
    public List getInvitations(int status, int start, int count, XWikiContext context)
    {
        try {
            Invitation prototype = createInvitation(context.getUser(), null, context);
            prototype.setStatus(status);
            return getInvitations(prototype, start, count, context);
        } catch (XWikiException e) {
            return Collections.EMPTY_LIST;
        }
    }

    public void initClasses(XWikiContext context) throws XWikiException
    {
        getInvitationClass(context);
        getMembershipRequestClass(context);
    }

    /**
     * @param context Xwiki context
     * @return Returns the Invitation Class as defined by the extension
     * @throws XWikiException
     */
    protected BaseClass getInvitationClass(XWikiContext context) throws XWikiException
    {
        String className = getInvitationClassName();
        String classContent = "1 XWiki Invitation Class";

        return getJoinRequestClass(className, classContent, context);
    }

    /**
     * @param context Xwiki context
     * @return Returns the Membership Class as defined by the extension
     * @throws XWikiException
     */
    protected BaseClass getMembershipRequestClass(XWikiContext context) throws XWikiException
    {
        String className = getMembershipRequestClassName();
        String classContent = "1 XWiki Membership Request Class";

        return getJoinRequestClass(className, classContent, context);
    }

    private BaseClass getJoinRequestClass(String className, String classContent,
        XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument(className, context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setFullName(className);
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        bclass.setName(className);

        needsUpdate |=
            bclass.addDateField(JoinRequestImpl.JoinRequestFields.REQUEST_DATE, "Request Date");
        needsUpdate |=
            bclass.addDateField(JoinRequestImpl.JoinRequestFields.RESPONSE_DATE, "Response Date");
        needsUpdate |=
            bclass.addTextAreaField(JoinRequestImpl.JoinRequestFields.TEXT, "Text", 80, 10);
        needsUpdate |=
            bclass.addTextAreaField(JoinRequestImpl.JoinRequestFields.MAP, "Map", 80, 10);
        needsUpdate |= bclass.addTextField(JoinRequestImpl.JoinRequestFields.SPACE, "Space", 32);
        needsUpdate |=
            bclass.addStaticListField(JoinRequestImpl.JoinRequestFields.STATUS, "Status", 1,
                false, "0=none|1=created|2=sent|3=accepted|4=refused|5=cancelled", "select");
        needsUpdate |=
            bclass.addDBListField(JoinRequestImpl.JoinRequestFields.ROLES, "Roles", 5, true, "");

        if (className.equals(getInvitationClassName())) {
            needsUpdate |= bclass.addTextField(InvitationImpl.InvitationFields.CODE, "Code", 40);
            needsUpdate |=
                bclass.addTextField(InvitationImpl.InvitationFields.INVITEE, "Invitee", 40);
            needsUpdate |=
                bclass.addTextField(InvitationImpl.InvitationFields.INVITEE, "Inviter", 40);
            needsUpdate |=
                bclass.addBooleanField(InvitationImpl.InvitationFields.OPEN, "Open", "yesno");
        } else if (className.equals(getMembershipRequestClassName())) {
            needsUpdate |=
                bclass.addTextField(MembershipRequestImpl.MembershipRequestFields.REQUESTER,
                    "Requester", 40);
            needsUpdate |=
                bclass.addTextField(MembershipRequestImpl.MembershipRequestFields.RESPONDER,
                    "Responder", 40);
        }

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent(classContent);
            doc.setSyntaxId(XWikiDocument.XWIKI10_SYNTAXID);
        }

        if (needsUpdate)
            xwiki.saveDocument(doc, context);
        return bclass;
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(int, int, XWikiContext)
     */
    public List getInvitations(int start, int count, XWikiContext context)
    {
        return getInvitations(JoinRequestStatus.ANY, start, count, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(int, XWikiContext)
     */
    public List getInvitations(int status, XWikiContext context)
    {
        return getInvitations(status, 0, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(Invitation, int, int, XWikiContext)
     */
    public List getInvitations(Invitation prototype, int start, int count, XWikiContext context)
    {
        try {
            String className = getInvitationClassName();
            StringBuffer fromClause =
                new StringBuffer("from XWikiDocument as doc, BaseObject as obj");
            StringBuffer whereClause =
                new StringBuffer("where doc.fullName=obj.name and obj.className = '" + className
                    + "'");

            String space = prototype.getSpace();
            String invitee = prototype.getInvitee();
            if (!"".equals(space) && !"".equals(invitee)) {
                Invitation invitation = getInvitation(space, invitee, context);
                // find a better way of testing if the invitation is new
                if (((Document) invitation).isNew()) {
                    return Collections.EMPTY_LIST;
                }
                List list = new ArrayList();
                list.add(invitation);
                return list;
            } else if (!"".equals(space)) {
                fromClause.append(", StringProperty as space");
                whereClause
                    .append(" and obj.id=space.id.id and space.id.name='"
                        + InvitationImpl.InvitationFields.SPACE + "' and space.value='" + space
                        + "'");
            } else if (!"".equals(invitee)) {
                fromClause.append(" StringProperty as invitee");
                whereClause.append(" and obj.id=invitee.id.id and invitee.id.name='"
                    + InvitationImpl.InvitationFields.INVITEE + "' and invitee.value='" + invitee
                    + "'");
            }

            int status = prototype.getStatus();
            if (status != JoinRequestStatus.ANY) {
                fromClause.append(", StringProperty as status");
                whereClause.append(" and obj.id=status.id.id and status.id.name='"
                    + InvitationImpl.InvitationFields.STATUS + "' and status.value='" + status
                    + "'");
            }

            List roles = prototype.getRoles();
            if (roles != null && roles.size() > 0) {
                String role = (String) prototype.getRoles().get(0);
                fromClause.append(", DBStringListProperty as roles join roles.list as role");
                whereClause.append(" and obj.id=roles.id.id and roles.id.name='"
                    + InvitationImpl.InvitationFields.ROLES + "' and instr(role.id, '" + role
                    + "')>0");
            }

            String inviter = prototype.getInviter();
            if (!"".equals(inviter)) {
                fromClause.append(" StringProperty as inviter");
                whereClause.append(" and obj.id=inviter.id.id and inviter.id.name='"
                    + InvitationImpl.InvitationFields.INVITER + "' and inviter.value='" + inviter
                    + "'");
            }

            String sql =
                "select distinct doc.fullName " + fromClause.toString() + " "
                    + whereClause.toString();

            return wrapInvitations(context.getWiki().getStore()
                .search(sql, count, start, context), context);
        } catch (XWikiException e) {
            e.printStackTrace();
            return Collections.EMPTY_LIST;
        }
    }

    private List wrapInvitations(List list, XWikiContext context)
    {
        ArrayList result = new ArrayList();
        if (list == null)
            return null;
        for (int i = 0; i < list.size(); i++) {
            String inviteDocName = (String) list.get(i);
            try {
                result.add(new InvitationImpl(inviteDocName, this, context));
            } catch (XWikiException e) {
                // let's not a single invitation break our requests
                e.printStackTrace();
            }
        }
        return result;
    }

    private List wrapMembershipRequests(List list, XWikiContext context)
    {
        ArrayList result = new ArrayList();
        if (list == null)
            return null;
        for (int i = 0; i < list.size(); i++) {
            String inviteDocName = (String) list.get(i);
            try {
                result.add(new MembershipRequestImpl(inviteDocName, this, context));
            } catch (XWikiException e) {
                // let's not a single invitation break our requests
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(Invitation, XWikiContext)
     */
    public List getInvitations(Invitation prototype, XWikiContext context)
    {
        return getInvitations(prototype, 0, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(String, int, int, int, XWikiContext)
     */
    public List getInvitations(String space, int status, int start, int count,
        XWikiContext context)
    {
        return getInvitations(space, status, null, start, count, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(String, int, int, XWikiContext)
     */
    public List getInvitations(String space, int start, int count, XWikiContext context)
    {
        return getInvitations(space, JoinRequestStatus.ANY, start, count, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(String, int, String, int, int, XWikiContext)
     */
    public List getInvitations(String space, int status, String role, int start, int count,
        XWikiContext context)
    {
        try {
            Invitation prototype = createInvitation(null, space, context);
            if ((role != null) && !"".equals(role)) {
                List roles = new ArrayList();
                roles.add(role);
                prototype.setRoles(roles);
            }
            prototype.setStatus(status);
            return getInvitations(prototype, start, count, context);
        } catch (XWikiException e) {
            e.printStackTrace();
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(String, int, String, XWikiContext)
     */
    public List getInvitations(String space, int status, String role, XWikiContext context)
    {
        return getInvitations(space, status, role, 0, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(String, int, XWikiContext)
     */
    public List getInvitations(String space, int status, XWikiContext context)
    {
        return getInvitations(space, status, 0, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(String, String, int, int, XWikiContext)
     */
    public List getInvitations(String space, String role, int start, int count,
        XWikiContext context)
    {
        return getInvitations(space, 0, role, start, count, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(String, String, XWikiContext)
     */
    public List getInvitations(String space, String role, XWikiContext context)
    {
        return getInvitations(space, role, 0, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(String, XWikiContext)
     */
    public List getInvitations(String space, XWikiContext context)
    {
        return getInvitations(space, 0, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getInvitations(XWikiContext)
     */
    public List getInvitations(XWikiContext context)
    {
        return getInvitations(0, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(int, int, int, XWikiContext)
     */
    public List getMembershipRequests(int status, int start, int count, XWikiContext context)
    {
        try {
            MembershipRequest prototype =
                createMembershipRequest(context.getUser(), null, context);
            prototype.setStatus(status);
            return getMembershipRequests(prototype, start, count, context);
        } catch (XWikiException e) {
            e.printStackTrace();
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(int, int, XWikiContext)
     */
    public List getMembershipRequests(int start, int count, XWikiContext context)
    {
        return getMembershipRequests(JoinRequestStatus.ANY, start, count, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(int, XWikiContext)
     */
    public List getMembershipRequests(int status, XWikiContext context)
    {
        return getMembershipRequests(status, 0, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(MembershipRequest, int, int, XWikiContext)
     */
    public List getMembershipRequests(MembershipRequest prototype, int start, int count,
        XWikiContext context)
    {
        try {
            String className = getMembershipRequestClassName();
            StringBuffer fromClause =
                new StringBuffer("from XWikiDocument as doc, BaseObject as obj");
            StringBuffer whereClause =
                new StringBuffer("where doc.fullName=obj.name and obj.className = '" + className
                    + "'");

            String space = prototype.getSpace();
            String requester = prototype.getRequester();
            if (!"".equals(space) && !"".equals(requester)) {
                MembershipRequest request = getMembershipRequest(space, requester, context);
                // find a better way of testing if the request is new
                if (((Document) request).isNew()) {
                    return Collections.EMPTY_LIST;
                }
                List list = new ArrayList();
                list.add(request);
                return list;
            } else if (!"".equals(space)) {
                fromClause.append(", StringProperty as space");
                whereClause.append(" and obj.id=space.id.id and space.id.name='"
                    + MembershipRequestImpl.MembershipRequestFields.SPACE + "' and space.value='"
                    + space + "'");
            } else if (!"".equals(requester)) {
                fromClause.append(" StringProperty as requester");
                whereClause.append(" and obj.id=requester.id.id and requester.id.name='"
                    + MembershipRequestImpl.MembershipRequestFields.REQUESTER
                    + "' and requester.value='" + requester + "'");
            }

            int status = prototype.getStatus();
            if (status != JoinRequestStatus.ANY) {
                fromClause.append(", StringProperty as status");
                whereClause.append(" and obj.id=status.id.id and status.id.name='"
                    + MembershipRequestImpl.MembershipRequestFields.STATUS
                    + "' and status.value='" + status + "'");
            }

            List roles = prototype.getRoles();
            if (roles != null && roles.size() > 0) {
                String role = (String) prototype.getRoles().get(0);
                fromClause.append(", DBStringListProperty as roles join roles.list as role");
                whereClause.append(" and obj.id=roles.id.id and roles.id.name='"
                    + MembershipRequestImpl.MembershipRequestFields.ROLES
                    + "' and instr(role.id, '" + role + "')>0");
            }

            String responder = prototype.getResponder();
            if (!"".equals(responder)) {
                fromClause.append(" StringProperty as responder");
                whereClause.append(" and obj.id=responder.id.id and responder.id.name='"
                    + MembershipRequestImpl.MembershipRequestFields.RESPONDER
                    + "' and responder.value='" + responder + "'");
            }

            String sql =
                "select distinct doc.fullName " + fromClause.toString() + " "
                    + whereClause.toString();
            return wrapMembershipRequests(context.getWiki().getStore().search(sql, count, start,
                context), context);
        } catch (XWikiException e) {
            e.printStackTrace();
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(MembershipRequest, XWikiContext)
     */
    public List getMembershipRequests(MembershipRequest prototype, XWikiContext context)
    {
        return getMembershipRequests(prototype, 0, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(String, int, int, int, XWikiContext)
     */
    public List getMembershipRequests(String space, int status, int start, int count,
        XWikiContext context)
    {
        return getMembershipRequests(space, status, null, start, count, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(String, int, int, XWikiContext)
     */
    public List getMembershipRequests(String space, int start, int count, XWikiContext context)
    {
        return getMembershipRequests(space, JoinRequestStatus.ANY, start, count, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(String, int, String, int, int, XWikiContext)
     */
    public List getMembershipRequests(String space, int status, String role, int start,
        int count, XWikiContext context)
    {
        try {
            MembershipRequest prototype = createMembershipRequest(null, space, context);
            if ((role != null) && !"".equals(role)) {
                List roles = new ArrayList();
                roles.add(role);
                prototype.setRoles(roles);
            }
            prototype.setStatus(status);
            return getMembershipRequests(prototype, start, count, context);
        } catch (XWikiException e) {
            e.printStackTrace();
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(String, int, String, XWikiContext)
     */
    public List getMembershipRequests(String space, int status, String role, XWikiContext context)
    {
        return getMembershipRequests(space, status, role, 0, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(String, int, XWikiContext)
     */
    public List getMembershipRequests(String space, int status, XWikiContext context)
    {
        return getMembershipRequests(space, status, 0, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(String, String, int, int, XWikiContext)
     */
    public List getMembershipRequests(String space, String role, int start, int count,
        XWikiContext context)
    {
        return getMembershipRequests(space, 0, role, start, count, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(String, String, XWikiContext)
     */
    public List getMembershipRequests(String space, String role, XWikiContext context)
    {
        return getMembershipRequests(space, role, 0, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(String, XWikiContext)
     */
    public List getMembershipRequests(String space, XWikiContext context)
    {
        return getMembershipRequests(space, 0, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#getMembershipRequests(XWikiContext)
     */
    public List getMembershipRequests(XWikiContext context)
    {
        return getMembershipRequests(0, 0, context);
    }

    private void addToAlreadyMember(String user, XWikiContext context)
    {
        List list = (List) context.get("im_alreadymember");
        if (list == null) {
            list = new ArrayList();
            context.put("im_alreadymember", list);
        }
        list.add(user);
    }

    private void addToAlreadyInvited(String user, XWikiContext context)
    {
        List list = (List) context.get("im_alreadyinvited");
        if (list == null) {
            list = new ArrayList();
            context.put("im_alreadyinvited", list);
        }
        list.add(user);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.invitationmanager.api.InvitationManager#inviteUser(String, String,
     *      boolean, List, String, Map, XWikiContext)
     */
    public void inviteUser(String wikiNameOrMailAddress, String space, boolean open, List roles,
        String templateMail, Map map, XWikiContext context) throws InvitationManagerException
    {
        try {
            if (wikiNameOrMailAddress == null || wikiNameOrMailAddress.trim().length() == 0) {
                throw new InvitationManagerException(InvitationManagerException.MODULE_PLUGIN_INVITATIONMANAGER,
                    InvitationManagerException.ERROR_INVITATION_INVITEE_MISSING,
                    "Invitee missing");
            }

            wikiNameOrMailAddress = wikiNameOrMailAddress.trim();
            String registeredUser = getRegisteredUser(wikiNameOrMailAddress, context);
            String invitee;
            if (registeredUser == null) {
                // it should be an e-mail address
                if (!isEmailAddress(wikiNameOrMailAddress)) {
                    throw new InvitationManagerException(InvitationManagerException.MODULE_PLUGIN_INVITATIONMANAGER,
                        InvitationManagerException.ERROR_INVITATION_INVITEE_EMAIL_INVALID,
                        "Invalid invitee e-mail address");
                }
                // hide the e-mail address (only for invitation document name)
                invitee = encodeEmailAddress(wikiNameOrMailAddress);
            } else {
                invitee = registeredUser;
                if (isMember(space, invitee, context)) {
                    addToAlreadyMember(invitee, context);
                    throw new InvitationManagerException(InvitationManagerException.MODULE_PLUGIN_INVITATIONMANAGER,
                        InvitationManagerException.ERROR_INVITATION_ALREADY_MEMBER,
                        "Already member");
                }
            }

            // create the invitation object
            Invitation invitation = createInvitation(invitee, space, context);

            // if we get here it means the invitee is not a member of the space
            if (!invitation.isNew()) {
                int status = invitation.getStatus();
                // maybe it's an old invitation
                if (JoinRequestStatus.CREATED == status || JoinRequestStatus.SENT == status) {
                    // is's a new one
                    addToAlreadyInvited(invitee, context);
                    throw new InvitationManagerException(InvitationManagerException.MODULE_PLUGIN_INVITATIONMANAGER,
                        InvitationManagerException.ERROR_INVITATION_ALREADY_EXISTS,
                        "Already invited");
                } else {
                    // is's an old one
                    // in this case we overwrite the invitation
                }
            }

            invitation.setInviter(context.getUser());
            invitation.setMap(map);
            invitation.setOpen(open);
            invitation.setRequestDate(new Date());
            invitation.setRoles(roles);
            invitation.setStatus(JoinRequestStatus.SENT);
            if (registeredUser == null) {
                invitation.setCode(generateInvitationCode());
                // make the e-mail address available in the invitee field
                invitation.setInvitee(wikiNameOrMailAddress);
            } else {
                invitation.setInvitee(registeredUser);
            }

            // send a notification mail
            if (templateMail == null) {
                templateMail =
                    getDefaultTemplateMailDocumentName(space, "Invitation",
                        JoinRequestAction.SEND, context);
            }
            sendMail(JoinRequestAction.SEND, invitation, templateMail, context);

            // save invitation after
            invitation.saveWithProgrammingRights();
        } catch (InvitationManagerException e) {
            throw e;
        } catch (XWikiException e) {
            throw new InvitationManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.invitationmanager.api.InvitationManager#inviteUser(String, String,
     *      boolean, List, String, XWikiContext)
     */
    public void inviteUser(String user, String space, boolean open, List roles,
        String templateMail, XWikiContext context) throws InvitationManagerException
    {
        inviteUser(user, space, open, roles, templateMail, Collections.EMPTY_MAP, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.invitationmanager.api.InvitationManager#inviteUser(String, String,
     *      boolean, List, XWikiContext)
     */
    public void inviteUser(String user, String space, boolean open, List roles,
        XWikiContext context) throws InvitationManagerException
    {
        String templateMail =
            getDefaultTemplateMailDocumentName(space, "Invitation", JoinRequestAction.SEND,
                context);
        inviteUser(user, space, open, roles, templateMail, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.invitationmanager.api.InvitationManager#inviteUser(String, String,
     *      boolean, String, XWikiContext)
     */
    public void inviteUser(String user, String space, boolean open, String role,
        XWikiContext context) throws InvitationManagerException
    {
        List roles = new ArrayList();
        roles.add(role);
        inviteUser(user, space, open, roles, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.invitationmanager.api.InvitationManager#inviteUser(String, String,
     *      boolean, XWikiContext)
     */
    public void inviteUser(String user, String space, boolean open, XWikiContext context)
        throws InvitationManagerException
    {
        inviteUser(user, space, open, Collections.EMPTY_LIST, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#rejectInvitation(String, String, String, XWikiContext)
     */
    public void rejectInvitation(String space, String email, String code, XWikiContext context)
        throws InvitationManagerException
    {
        try {
            Invitation invitation = getInvitation(space, email, context);
            if (code.equals(invitation.getCode())
                && invitation.getStatus() == JoinRequestStatus.SENT) {
                if (!invitation.isOpen()) {
                    invitation.setStatus(JoinRequestStatus.REFUSED);
                    invitation.setResponseDate(new Date());
                    invitation.setInvitee(context.getUser());
                    invitation.saveWithProgrammingRights();
                }
                // create a custom invitation for the currently logged-in user
                customizeInvitation(invitation, JoinRequestStatus.REFUSED, context);
            }
        } catch (XWikiException e) {
            throw new InvitationManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#rejectInvitation(String, XWikiContext)
     */
    public void rejectInvitation(String space, XWikiContext context)
        throws InvitationManagerException
    {
        try {
            Invitation invitation = getInvitation(space, context.getUser(), context);
            if (invitation.getStatus() == JoinRequestStatus.SENT) {
                invitation.setStatus(JoinRequestStatus.REFUSED);
                invitation.setResponseDate(new Date());
                invitation.saveWithProgrammingRights();
            }
        } catch (XWikiException e) {
            throw new InvitationManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#rejectMembership(String, String, String, XWikiContext)
     */
    public void rejectMembership(String space, String userName, String templateMail,
        XWikiContext context) throws InvitationManagerException
    {
        try {
            MembershipRequest request = getMembershipRequest(space, userName, context);
            if (request.getStatus() == JoinRequestStatus.SENT) {
                request.setStatus(JoinRequestStatus.REFUSED);
                request.setResponseDate(new Date());
                request.setResponder(context.getUser());
                sendMail(JoinRequestAction.REJECT, request, templateMail, context);
                request.saveWithProgrammingRights();
            }
        } catch (XWikiException e) {
            throw new InvitationManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#rejectMembership(String, String, XWikiContext)
     */
    public void rejectMembership(String space, String userName, XWikiContext context)
        throws InvitationManagerException
    {
        String templateMail =
            getDefaultTemplateMailDocumentName(space, "Request", JoinRequestAction.REJECT,
                context);
        rejectMembership(space, userName, templateMail, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#requestMembership(String, String, List, Map, XWikiContext)
     */
    public void requestMembership(String space, String message, List roles, Map map,
        XWikiContext context) throws InvitationManagerException
    {
        try {
            MembershipRequest request =
                createMembershipRequest(context.getUser(), space, context);
            request.setMap(map);
            request.setRequestDate(new Date());
            request.setRoles(roles);
            request.setStatus(JoinRequestStatus.SENT);
            request.setText(message);
            // e-mail notification is sent to user's e-mail address informing them of their "Pending
            // approval" status
            String requestSentMailTemplate =
                getDefaultTemplateMailDocumentName(space, "Request", JoinRequestAction.CREATE,
                    context);
            sendMail(JoinRequestAction.CREATE, request, requestSentMailTemplate, context);
            // send "Membership Request awaiting approval" notification via e-mail to the space
            // administrator(s)
            String requestPendingMailTemplate =
                getDefaultTemplateMailDocumentName(space, "Request", JoinRequestAction.SEND,
                    context);
            sendMail(JoinRequestAction.SEND, request, requestPendingMailTemplate, context);
            request.saveWithProgrammingRights();
        } catch (XWikiException e) {
            throw new InvitationManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#requestMembership(String, String, List, XWikiContext)
     */
    public void requestMembership(String space, String message, List roles, XWikiContext context)
        throws InvitationManagerException
    {
        requestMembership(space, message, roles, Collections.EMPTY_MAP, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#requestMembership(String, String, String, XWikiContext)
     */
    public void requestMembership(String space, String message, String role, XWikiContext context)
        throws InvitationManagerException
    {
        List roles = new ArrayList();
        roles.add(role);
        requestMembership(space, message, roles, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InvitationManager#requestMembership(String, String, XWikiContext)
     */
    public void requestMembership(String space, String message, XWikiContext context)
        throws InvitationManagerException
    {
        requestMembership(space, message, Collections.EMPTY_LIST, context);
    }

    /**
     * @return the name of the document (wiki page) that is the default mail template to be used for
     *         notifying the execution of the specified action on a request of class
     *         <code>joinRequestClass</code> related to the given space.
     */
    private String getDefaultTemplateMailDocumentName(String space, String type, String action,
        XWikiContext context)
    {
        String docName = space + "." + "MailTemplate" + action + type;
        try {
            if (context.getWiki().getDocument(docName, context).isNew()) {
                docName = null;
            }
        } catch (XWikiException e) {
            docName = null;
        }
        if (docName == null) {
            docName = getDefaultResourceSpace(context) + "." + "MailTemplate" + action + type;
        }

        return docName;
    }

    private String getDefaultResourceSpace(XWikiContext context)
    {
        return context.getWiki().Param("xwiki.invitationmanager.resourcespace",
            InvitationManager.DEFAULT_RESOURCE_SPACE);
    }

    private String getDefaultInvitationsSpaceSuffix(XWikiContext context)
    {
        return context.getWiki().Param("xwiki.invitationmanager.invitationspacesuffix",
            InvitationManager.DEFAULT_INVITATIONS_SPACE_SUFFIX);
    }

    /**
     * @return the name of the document (wiki page) to be used for storing a request of class
     *         <code>joinRequestClass</code> that would allow the specified user to join the
     *         specified space.
     */
    public String getJoinRequestDocumentName(String type, String space, String user,
        XWikiContext context)
    {
        if (space == null) {
            space = System.currentTimeMillis() + "";
        }
        if (user == null) {
            user = System.currentTimeMillis() + "";
        }

        user = clearUserName(user);

        return getInvitationSpaceName(space, context) + "." + type + "_" + user;
    }

    private String clearUserName(String user)
    {
        if (isEmailAddress(user))
            return user.replaceAll("\\.", "_").replaceAll("\\@", "_at_");
        else
            return user.replaceAll("XWiki\\.", "");
    }

    private String getInvitationSpaceName(String space, XWikiContext context)
    {
        return space + getDefaultInvitationsSpaceSuffix(context);
    }

    /**
     * @return the xwiki class associated with invitations
     */
    public String getInvitationClassName()
    {
        return INVITATION_CLASS_NAME;
    }

    /**
     * @return the xwiki class associated with invitations
     */
    public String getMembershipRequestClassName()
    {
        return MEMBERSHIP_REQUEST_CLASS_NAME;
    }

    /**
     * @return the stored invitation uniquely identified by the given space and invitee
     */
    private Invitation getInvitation(String space, String invitee, XWikiContext context)
        throws XWikiException
    {
        Invitation invitation = null;
        if (space == null || invitee == null) {
            // skip to the end
        } else if (isEmailAddress(invitee)) {
            String email = invitee.trim();
            String encodedEmail = encodeEmailAddress(email);
            String user = getRegisteredUser(invitee, context);
            invitation = new InvitationImpl(encodedEmail, space, false, this, context);
            if (invitation.isNew() && user != null) {
                invitation = new InvitationImpl(user, space, false, this, context);
            }
            String realInvitee = invitation.getInvitee();
            if (!email.equals(realInvitee) && !user.equals(realInvitee)) {
                invitation = null;
            }
        } else {
            String user = getRegisteredUser(invitee, context);
            if (user != null) {
                String email = "";
                String encodedEmail = encodeEmailAddress(email);
                invitation = new InvitationImpl(user, space, false, this, context);
                if (invitation.isNew()) {
                    invitation = new InvitationImpl(encodedEmail, space, false, this, context);
                }
                String realInvitee = invitation.getInvitee();
                if (!email.equals(realInvitee) && !user.equals(realInvitee)) {
                    invitation = null;
                }
            }
        }
        if (invitation == null) {
            invitation = new InvitationImpl(null, null, false, this, context);
        }
        return invitation;
    }

    /**
     * @return a newly created invitation to be sent to the <code>invitee</code> in order to join
     *         the <code>space</code>
     */
    private Invitation createInvitation(String invitee, String space, XWikiContext context)
        throws XWikiException
    {
        return new InvitationImpl(invitee, space, true, this, context);
    }

    /**
     * Creates a custom invitation for the currently logged-in user, from the given one, and then
     * saves it.
     */
    private void customizeInvitation(Invitation invitation, int status, XWikiContext context)
        throws XWikiException
    {
        Invitation customInvitation =
            createInvitation(context.getUser(), invitation.getSpace(), context);
        customInvitation.setInviter(invitation.getInviter());
        customInvitation.setMap(invitation.getMap());
        customInvitation.setRequestDate(invitation.getRequestDate());
        customInvitation.setResponseDate(new Date());
        customInvitation.setRoles(invitation.getRoles());
        customInvitation.setStatus(status);
        customInvitation.setOpen(false);
        customInvitation.setText(invitation.getText());
        customInvitation.saveWithProgrammingRights();
    }

    /**
     * @return the stored membership request uniquely identified by the given space and requester
     */
    private MembershipRequest getMembershipRequest(String space, String requester,
        XWikiContext context) throws XWikiException
    {
        return new MembershipRequestImpl(requester, space, false, this, context);
    }

    /**
     * @return a newly created membership request to be sent by the <code>requester</code> in
     *         order to join the <code>space</code>
     */
    private MembershipRequest createMembershipRequest(String requester, String space,
        XWikiContext context) throws XWikiException
    {
        return new MembershipRequestImpl(requester, space, true, this, context);
    }

    /**
     * @return the encoded email address to be used when naming the document (wiki page) storing the
     *         invitation to the unregistered user with the given email address
     */
    private String encodeEmailAddress(String emailAddress)
    {
        return emailAddress.hashCode() + "";
    }

    /**
     * @return a new random code for an invitation
     */
    private String generateInvitationCode()
    {
        return RandomStringUtils.randomAlphabetic(8).toLowerCase();
    }

    /**
     * Wrapper method for adding a user to a space and to the given roles using the space manager
     */
    private void addMember(String space, String user, List roles, XWikiContext context)
        throws XWikiException
    {
        SpaceManager spaceManager = SpaceManagers.findSpaceManagerForSpace(space, context);
        if (!spaceManager.isMember(space, user, context)) {
            spaceManager.addMember(space, user, context);
            if (roles != null && roles.size() > 0) {
                spaceManager.addUserToRoles(space, user, roles, context);
            }
        }
    }

    /**
     * Wrapper method for testing if a user is a member of a space
     */
    private boolean isMember(String space, String user, XWikiContext context)
    {
        try {
            SpaceManager spaceManager = SpaceManagers.findSpaceManagerForSpace(space, context);
            return spaceManager.isMember(space, user, context);
        } catch (XWikiException e) {
            return false;
        }
    }

    /**
     * Wrapper method for sending a mail using the mail sender plug-in
     */
    private void sendMail(String action, JoinRequest request, String templateDocFullName,
        XWikiContext context) throws XWikiException
    {
        if (!mailNotification) {
            return;
        }

        VelocityContext vContext = new VelocityContext();
        String spaceName = request.getSpace();
        SpaceManager spaceManager = SpaceManagers.findSpaceManagerForSpace(spaceName, context);
        Space space = spaceManager.getSpace(spaceName, context);
        vContext.put(SPACE_VELOCITY_KEY, space);
        String fromUser = context.getWiki().getXWikiPreference("invitation_email", context);
        if (fromUser == null || fromUser.trim().length() == 0) {
            fromUser = context.getWiki().getXWikiPreference("admin_email", context);
        }
        String[] toUsers = new String[0];
        if (request instanceof Invitation) {
            Invitation invitation = (Invitation) request;
            vContext.put(INVITATION_VELOCITY_KEY, invitation);
            if (JoinRequestAction.SEND.equals(action)) {
                // invitation notification mail
                toUsers = new String[] {invitation.getInvitee()};
            } else {
                // accept or reject invitation mail
                toUsers = new String[] {invitation.getInviter()};
            }
        } else if (request instanceof MembershipRequest) {
            MembershipRequest membershipRequest = (MembershipRequest) request;
            vContext.put(MEMBERSHIP_REQUEST_VELOCITY_KEY, membershipRequest);
            if (JoinRequestAction.SEND.equals(action)) {
                // notify the space administrators of a new membership request pending for approval
                Collection admins = spaceManager.getAdmins(spaceName, context);
                toUsers = (String[]) admins.toArray(new String[admins.size()]);
            } else {
                // create, accept or reject membership request mail
                toUsers = new String[] {membershipRequest.getRequester()};
            }
        }

        if (fromUser == null) {
            throw new InvitationManagerException(InvitationManagerException.MODULE_PLUGIN_INVITATIONMANAGER,
                InvitationManagerException.ERROR_INVITATION_SENDER_EMAIL_INVALID,
                "Sender email is invalid");
        }

        boolean toUsersValid = toUsers.length > 0;
        for (int i = 0; i < toUsers.length && toUsersValid; i++) {
            if (!isEmailAddress(toUsers[i])) {
                toUsers[i] = getEmailAddress(toUsers[i], context);
            }
            if (toUsers[i] == null) {
                toUsersValid = false;
            }
        }

        if (!toUsersValid) {
            throw new InvitationManagerException(InvitationManagerException.MODULE_PLUGIN_INVITATIONMANAGER,
                InvitationManagerException.ERROR_INVITATION_TARGET_EMAIL_INVALID,
                "Target email is invalid");
        }
        String strToUsers = join(toUsers, ",");

        MailSenderPlugin mailSender = getMailSenderPlugin(context);

        // gets the template doc
        XWikiDocument mailDoc = context.getWiki().getDocument(templateDocFullName, context);
        XWikiDocument translatedMailDoc = mailDoc.getTranslatedDocument(context);

        // puts some generic variables in the velocity rendering context so we can write the
        // template doc almost as a normal document
        vContext.put("xwiki", new XWiki(context.getWiki(), context));
        vContext.put("context", new com.xpn.xwiki.api.Context(context));
        vContext.put("doc", translatedMailDoc);
        vContext.put("space", space);

        try {
            mailSender.sendMailFromTemplate(templateDocFullName, fromUser, strToUsers, null, null, context.getLanguage(), vContext, context);
        } catch (Exception e) {
            throw new InvitationManagerException(InvitationManagerException.MODULE_PLUGIN_INVITATIONMANAGER,
                InvitationManagerException.ERROR_INVITATION_SENDING_EMAIL_FAILED,
                "Sending notification email failed",
                e);
        }
    }

    private static final String join(String[] array, String separator)
    {
        StringBuffer result = new StringBuffer("");
        if (array.length > 0) {
            result.append(array[0]);
        }
        for (int i = 1; i < array.length; i++) {
            result.append("," + array[i]);
        }
        return result.toString();
    }

    private MailSenderPlugin getMailSenderPlugin(XWikiContext context)
        throws InvitationManagerException
    {
        MailSenderPlugin mailSender =
            (MailSenderPlugin) context.getWiki().getPlugin("mailsender", context);

        if (mailSender == null)
            throw new InvitationManagerException(InvitationManagerException.MODULE_PLUGIN_INVITATIONMANAGER,
                InvitationManagerException.ERROR_INVITATION_MANAGER_REQUIRES_MAILSENDER_PLUGIN,
                "Invitation Manager requires the mail sender plugin");

        return mailSender;
    }

    /**
     * @return the wiki name of the registered user with the given
     *         <code>wikiNameOrMailAddress</code>
     */
    private String getRegisteredUser(String wikiNameOrMailAddress, XWikiContext context)
    {
        if (!isEmailAddress(wikiNameOrMailAddress)) {
            return findUser(wikiNameOrMailAddress, context);
        }
        String email = wikiNameOrMailAddress;
        String sql =
            "select distinct doc.fullName from XWikiDocument as doc, BaseObject as obj, StringProperty as prop where doc.fullName=obj.name and obj.className = 'XWiki.XWikiUsers' and obj.id=prop.id.id and prop.id.name='email' and prop.value='"
                + email + "'";
        List userList = Collections.EMPTY_LIST;
        try {
            userList = context.getWiki().getStore().search(sql, 1, 0, context);
        } catch (XWikiException e) {
        }
        if (userList.size() > 0) {
            return (String) userList.get(0);
        } else {
            return null;
        }
    }

    /**
     * Helper method for testing if a given string is an email address.
     */
    private boolean isEmailAddress(String str)
    {
        return str.contains("@");
    }

    /**
     * @return the email address of the given user, provided he is registered
     */
    private String getEmailAddress(String user, XWikiContext context)
        throws InvitationManagerException
    {
        try {
            String wikiuser = (user.startsWith("XWiki.")) ? user : "XWiki." + user;

            if (wikiuser == null)
                return null;

            XWikiDocument userDoc = null;
            userDoc = context.getWiki().getDocument(wikiuser, context);

            if (userDoc.isNew())
                return null;

            String email = "";
            try {
                email = userDoc.getObject("XWiki.XWikiUsers").getStringValue("email");
            } catch (Exception e) {
                return null;
            }
            if ((email == null) || (email.equals("")))
                return null;

            return email;
        } catch (Exception e) {
            throw new InvitationManagerException(InvitationManagerException.MODULE_PLUGIN_INVITATIONMANAGER,
                InvitationManagerException.ERROR_INVITATION_CANNOT_FIND_EMAIL_ADDRESS,
                "Cannot find email address of user " + user,
                e);
        }
    }

    private String findUser(String userName, XWikiContext context)
    {
        userName = userName.trim();
        if (!userName.startsWith("XWiki.")) {
            userName = "XWiki." + userName;
        }
        if (context.getWiki().exists(userName, context))
            return userName;
        else
            return null;
    }

    /**
     * Creates and saves a user profile associated with the given space and the user who made the
     * membership request
     */
    private void createSpaceUserProfile(String spaceName, MembershipRequest request,
        XWikiContext context) throws XWikiException
    {
        SpaceManager spaceManager = SpaceManagers.findSpaceManagerForSpace(spaceName, context);
        SpaceUserProfile profile =
            new SpaceUserProfileImpl(request.getRequester(), spaceName, spaceManager, context);
        profile.setAllowNotifications("true".equals(request.getMap().get("allowNotifications")));
        profile.setAllowNotificationsFromSelf("true".equals(request.getMap().get(
            "allowNotificationsFromSelf")));
        String profileText = (String) request.getMap().get("profile");
        if (profileText == null) {
            profileText = request.getText();
        }
        if (profileText == null) {
            profileText = "";
        }
        profile.setProfile(profileText);
        profile.saveWithProgrammingRights();
    }

    /**
     * Creates and saves a user profile associated with the given space and the currently logged in
     * user
     */
    private void createSpaceUserProfile(String spaceName, XWikiContext context)
        throws XWikiException
    {
        SpaceManager spaceManager = SpaceManagers.findSpaceManagerForSpace(spaceName, context);
        SpaceUserProfile profile =
            new SpaceUserProfileImpl(context.getUser(), spaceName, spaceManager, context);
        profile.updateProfileFromRequest();
        profile.saveWithProgrammingRights();
    }

    public boolean isWithUserProfiles(XWikiContext context)
    {
        return context.getWiki().Param("xwiki.invitationmanager.userprofiles",
            USER_PROFILES_DEFAULT).equals("0") ? false : true;
    }

    public boolean isMailNotification()
    {
        return mailNotification;
    }

    public void setMailNotification(boolean mailNotification)
    {
        this.mailNotification = mailNotification;
    }
}
