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
package org.xwiki.wiki.user.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.user.MemberCandidacy;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserConfiguration;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.WikiUserManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for {@link WikiUserManager}.
 *
 * @version $Id$
 * @since 5.3M2
 */
@Component
@Singleton
public class DefaultWikiUserManager implements WikiUserManager
{
    private static final LocalDocumentReference GROUPCLASS_REFERENCE = new LocalDocumentReference(XWiki.SYSTEM_SPACE,
        "XWikiGroups");

    private static final String GROUP_CLASS_MEMBER_FIELD = "member";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private WikiUserConfigurationHelper wikiUserConfigurationHelper;

    @Inject
    private EntityReferenceSerializer<String> documentReferenceSerializer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public UserScope getUserScope(String wikiId) throws WikiUserManagerException
    {
        return wikiUserConfigurationHelper.getConfiguration(wikiId).getUserScope();
    }

    @Override
    public void setUserScope(String wikiId, UserScope scope) throws WikiUserManagerException
    {
        WikiUserConfiguration configuration = wikiUserConfigurationHelper.getConfiguration(wikiId);
        configuration.setUserScope(scope);
        wikiUserConfigurationHelper.saveConfiguration(configuration, wikiId);
    }

    @Override
    public MembershipType getMembershipType(String wikiId) throws WikiUserManagerException
    {
        return wikiUserConfigurationHelper.getConfiguration(wikiId).getMembershipType();
    }

    @Override
    public void setMembershipType(String wikiId, MembershipType type) throws WikiUserManagerException
    {
        WikiUserConfiguration configuration = wikiUserConfigurationHelper.getConfiguration(wikiId);
        configuration.setMembershipType(type);
        wikiUserConfigurationHelper.saveConfiguration(configuration, wikiId);
    }

    @Override
    public Collection<String> getLocalUsers(String wikiId) throws WikiUserManagerException
    {
        // TODO: Implement this method. This is not urgent because no one needs it yet.
        return null;
    }

    private XWikiDocument getMembersGroupDocument(String wikiId) throws WikiUserManagerException
    {
        // Reference to the document
        DocumentReference memberGroupReference = new DocumentReference(wikiId, XWiki.SYSTEM_SPACE, "XWikiAllGroup");

        // Get the document
        try {
            XWikiContext xcontext = xcontextProvider.get();
            XWiki xwiki = xcontext.getWiki();
            return xwiki.getDocument(memberGroupReference, xcontext);
        } catch (XWikiException e) {
            throw new WikiUserManagerException(String.format("Fail to load the member group document [%s].",
                    memberGroupReference.toString()), e);
        }
    }

    private void saveGroupDocument(XWikiDocument document, String message) throws WikiUserManagerException
    {
        // Get the XWiki objects
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        // The document should be hidden
        document.setHidden(true);

        // The document must have a creator
        if (document.getCreatorReference() == null) {
            document.setCreatorReference(xcontext.getUserReference());
        }
        // The document must have an author
        if (document.getAuthorReference() == null) {
            document.setAuthorReference(xcontext.getUserReference());
        }

        // Save the document
        try {
            xwiki.saveDocument(document, message, xcontext);
        } catch (XWikiException e) {
            throw new WikiUserManagerException("Fail to save the member group", e);
        }
    }

    @Override
    public Collection<String> getMembers(String wikiId) throws WikiUserManagerException
    {
        List<String> members = new ArrayList<>();

        try {
            // Get the descriptor
            WikiDescriptor descriptor = wikiDescriptorManager.getById(wikiId);
            // Add the wiki owner
            members.add(descriptor.getOwnerId());
        } catch (WikiManagerException e) {
            throw new WikiUserManagerException(String.format("Failed to get the descriptor for [%s]", wikiId), e);
        }

        // Get the other members from the wiki AllGroup
        XWikiDocument groupDoc = getMembersGroupDocument(wikiId);
        List<BaseObject> memberObjects = groupDoc.getXObjects(GROUPCLASS_REFERENCE);
        if (memberObjects != null) {
            for (BaseObject object : memberObjects) {
                if (object == null) {
                    continue;
                }
                String member = object.getStringValue(GROUP_CLASS_MEMBER_FIELD);
                if (!member.isEmpty() && !members.contains(member)) {
                    members.add(member);
                }
            }
        }

        return members;
    }

    @Override
    public boolean isMember(String userId, String wikiId) throws WikiUserManagerException
    {
        return getMembers(wikiId).contains(userId);
    }

    private void addMemberObject(XWikiDocument groupDoc, String userId)
        throws WikiUserManagerException
    {
        try {
            XWikiContext xcontext = xcontextProvider.get();
            int objectNumber = groupDoc.createXObject(GROUPCLASS_REFERENCE, xcontext);
            BaseObject object = groupDoc.getXObject(GROUPCLASS_REFERENCE, objectNumber);
            object.set(GROUP_CLASS_MEMBER_FIELD, userId, xcontext);
        } catch (XWikiException e) {
            throw new WikiUserManagerException("Fail to add a member to the group", e);
        }
    }

    @Override
    public void addMember(String userId, String wikiId) throws WikiUserManagerException
    {
        Collection<String> members = getMembers(wikiId);
        if (members.contains(userId)) {
            // Nothing to do !
            return;
        }

        // Get the group document
        XWikiDocument groupDoc = getMembersGroupDocument(wikiId);

        // Avoid modifying the cached document
        groupDoc.clone();

        // Add a member object
        // If the group does not contain any user yet, add an empty member (cf: XWIKI-6275).
        List<BaseObject> memberObjects = groupDoc.getXObjects(GROUPCLASS_REFERENCE);
        if (memberObjects == null || memberObjects.isEmpty()) {
            addMemberObject(groupDoc, "");
        }

        // Add the user
        addMemberObject(groupDoc, userId);

        // Save the document
        saveGroupDocument(groupDoc, String.format("Add [%s] to the group.", userId));
    }

    @Override
    public void addMembers(Collection<String> userIds, String wikiId) throws WikiUserManagerException
    {
        Collection<String> members = getMembers(wikiId);

        // Get the group document
        XWikiDocument groupDoc = getMembersGroupDocument(wikiId);

        // Avoid modifying the cached document
        groupDoc.clone();

        // If the group does not contain any user yet, add an empty member (cf: XWIKI-6275).
        List<BaseObject> memberObjects = groupDoc.getXObjects(GROUPCLASS_REFERENCE);
        if (memberObjects == null || memberObjects.isEmpty()) {
            addMemberObject(groupDoc, "");
        }

        // Add members
        for (String userId : userIds) {
            if (!members.contains(userId)) {
                // Add a member object
                addMemberObject(groupDoc, userId);
            }
        }

        // Save the document
        saveGroupDocument(groupDoc, "Add members to the group.");
    }

    @Override
    public void removeMember(String userId, String wikiId) throws WikiUserManagerException
    {
        // Get the group document
        XWikiDocument groupDoc = getMembersGroupDocument(wikiId);

        // Avoid modifying the cached document
        groupDoc.clone();

        // Get the member objects
        List<BaseObject> objects = groupDoc.getXObjects(GROUPCLASS_REFERENCE);
        if (objects != null) {

            // Get the member objects to remove
            List<BaseObject> objectsToRemove = new ArrayList<>();
            for (BaseObject object : objects) {
                if (object == null) {
                    continue;
                }
                String member = object.getStringValue(GROUP_CLASS_MEMBER_FIELD);
                if (userId.equals(member)) {
                    objectsToRemove.add(object);
                }
            }

            // Remove them
            for (BaseObject object : objectsToRemove) {
                groupDoc.removeXObject(object);
            }

            // Save the document
            saveGroupDocument(groupDoc, String.format("Remove [%s] from the group.", userId));
        }
    }

    @Override
    public void removeMembers(Collection<String> userIds, String wikiId) throws WikiUserManagerException
    {
        // Get the group document
        XWikiDocument groupDoc = getMembersGroupDocument(wikiId);

        // Avoid modifying the cached document
        groupDoc.clone();

        // Get the member objects
        List<BaseObject> objects = groupDoc.getXObjects(GROUPCLASS_REFERENCE);
        if (objects != null) {

            // Get the member objects to remove
            List<BaseObject> objectsToRemove = new ArrayList<>();
            for (String userId: userIds) {
                for (BaseObject object : objects) {
                    if (object == null) {
                        continue;
                    }
                    String member = object.getStringValue(GROUP_CLASS_MEMBER_FIELD);
                    if (userId.equals(member)) {
                        objectsToRemove.add(object);
                    }
                }
            }

            // Remove them
            for (BaseObject object : objectsToRemove) {
                groupDoc.removeXObject(object);
            }

            // Save the document
            saveGroupDocument(groupDoc, "Remove some users from the group.");
        }
    }

    private MemberCandidacy readCandidacyFromObject(BaseObject object, String wikiId)
    {
        MemberCandidacy candidacy = new MemberCandidacy();

        candidacy.setId(object.getNumber());
        candidacy.setWikiId(wikiId);
        candidacy.setUserId(object.getStringValue(WikiCandidateMemberClassInitializer.FIELD_USER));
        candidacy.setUserComment(object.getLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_USER_COMMENT));
        candidacy.setAdminId(object.getStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN));
        candidacy.setAdminComment(object.getLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_COMMENT));
        candidacy.setAdminPrivateComment(object.getLargeStringValue(
                WikiCandidateMemberClassInitializer.FIELD_ADMIN_PRIVATE_COMMENT));
        candidacy.setStatus(
                MemberCandidacy.Status.valueOf(
                        object.getStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS).toUpperCase()));
        candidacy.setType(
                MemberCandidacy.CandidateType.valueOf(
                        object.getStringValue(WikiCandidateMemberClassInitializer.FIELD_TYPE).toUpperCase())
        );
        candidacy.setDateOfCreation(object.getDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CREATION));
        candidacy.setDateOfCreation(object.getDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE));

        return candidacy;
    }

    private Collection<MemberCandidacy> getAllMemberCandidacies(String wikiId, MemberCandidacy.CandidateType type)
        throws WikiUserManagerException
    {
        // Get the group document
        XWikiDocument groupDoc = getMembersGroupDocument(wikiId);

        // Collect all the candidacy of the good type
        Collection<MemberCandidacy> candidacies = new ArrayList<>();
        String typeString = type.name().toLowerCase();
        List<BaseObject> candidacyObjects = groupDoc.getXObjects(WikiCandidateMemberClassInitializer.REFERENCE);
        if (candidacyObjects != null) {
            for (BaseObject object : candidacyObjects) {
                if (object != null
                        && object.getStringValue(WikiCandidateMemberClassInitializer.FIELD_TYPE).equals(typeString)) {
                    candidacies.add(readCandidacyFromObject(object, wikiId));
                }
            }
        }

        return candidacies;
    }

    @Override
    public Collection<MemberCandidacy> getAllInvitations(String wikiId) throws WikiUserManagerException
    {
        return getAllMemberCandidacies(wikiId, MemberCandidacy.CandidateType.INVITATION);
    }

    @Override
    public Collection<MemberCandidacy> getAllRequests(String wikiId) throws WikiUserManagerException
    {
        return getAllMemberCandidacies(wikiId, MemberCandidacy.CandidateType.REQUEST);
    }

    @Override
    public boolean hasPendingInvitation(DocumentReference user, String wikiId) throws WikiUserManagerException
    {
        Collection<MemberCandidacy> invitations = getAllInvitations(wikiId);
        if (invitations != null) {
            String userId = documentReferenceSerializer.serialize(user);
            for (MemberCandidacy invitation : invitations) {
                if (StringUtils.equals(invitation.getUserId(), userId)
                    && invitation.getStatus() == MemberCandidacy.Status.PENDING) {
                    return true;
                }
            }
        }
        // No pending invitation
        return false;
    }

    @Override
    public boolean hasPendingRequest(DocumentReference user, String wikiId) throws WikiUserManagerException
    {
        Collection<MemberCandidacy> requests = getAllRequests(wikiId);
        if (requests != null) {
            String userId = documentReferenceSerializer.serialize(user);
            for (MemberCandidacy request : requests) {
                if (StringUtils.equals(request.getUserId(), userId)
                    && request.getStatus() == MemberCandidacy.Status.PENDING) {
                    return true;
                }
            }
        }
        // No pending request
        return false;
    }

    @Override
    public MemberCandidacy getCandidacy(String wikiId, int candidacyId) throws WikiUserManagerException
    {
        // Get the group document
        XWikiDocument groupDoc = getMembersGroupDocument(wikiId);

        // Get the candidacy
        BaseObject object = groupDoc.getXObject(WikiCandidateMemberClassInitializer.REFERENCE, candidacyId);
        return readCandidacyFromObject(object, wikiId);
    }

    @Override
    public MemberCandidacy askToJoin(String userId, String wikiId, String message)
        throws WikiUserManagerException
    {
        MemberCandidacy candidacy = new MemberCandidacy(wikiId, userId, MemberCandidacy.CandidateType.REQUEST);
        candidacy.setUserComment(message);

        // Get the group document
        XWikiDocument groupDoc = getMembersGroupDocument(wikiId);

        // Avoid modifying the cached document
        groupDoc.clone();

        // Add a candidacy object
        XWikiContext xcontext = xcontextProvider.get();
        try {
            int objectNumber = groupDoc.createXObject(WikiCandidateMemberClassInitializer.REFERENCE, xcontext);
            candidacy.setId(objectNumber);
            BaseObject object = groupDoc.getXObject(WikiCandidateMemberClassInitializer.REFERENCE, objectNumber);
            object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_USER, candidacy.getUserId());
            object.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_USER_COMMENT,
                    candidacy.getUserComment());
            object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS,
                    candidacy.getStatus().name().toLowerCase());
            object.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CREATION,
                    candidacy.getDateOfCreation());
            object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_TYPE,
                    candidacy.getType().name().toLowerCase());
        } catch (XWikiException e) {
            throw new WikiUserManagerException("Failed to create a new join request.", e);
        }

        // Save the document
        saveGroupDocument(groupDoc, String.format("[%s] asks to join the wiki.", userId));

        return candidacy;
    }

    @Override
    public void join(String userId, String wikiId) throws WikiUserManagerException
    {
        // Check if the user has the right to join the wiki
        if (!wikiUserConfigurationHelper.getConfiguration(wikiId).getMembershipType().equals(MembershipType.OPEN)) {
            throw new WikiUserManagerException(String.format("The user [%s] is not authorized to join the wiki [%s].",
                    userId, wikiId));
        }

        // Join the wiki
        addMember(userId, wikiId);
    }

    @Override
    public void leave(String userId, String wikiId) throws WikiUserManagerException
    {
        removeMember(userId, wikiId);
    }

    @Override
    public void acceptRequest(MemberCandidacy request, String message, String privateComment)
        throws WikiUserManagerException
    {
        // Add the user to the members
        addMember(request.getUserId(), request.getWikiId());

        // Then, update the candidacy object
        XWikiContext xcontext = xcontextProvider.get();

        // Set the values
        request.setAdminId(documentReferenceSerializer.serialize(xcontext.getUserReference()));
        request.setAdminComment(message);
        request.setAdminPrivateComment(privateComment);
        request.setStatus(MemberCandidacy.Status.ACCEPTED);
        request.setDateOfClosure(new Date());

        // Get the group document
        XWikiDocument groupDoc = getMembersGroupDocument(request.getWikiId());

        // Avoid modifying the cached document
        groupDoc.clone();

        // Get the candidacy object
        BaseObject object = groupDoc.getXObject(WikiCandidateMemberClassInitializer.REFERENCE, request.getId());

        // Set the new values
        object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN, request.getAdminId());
        object.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_COMMENT, request.getAdminComment());
        object.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_PRIVATE_COMMENT,
                request.getAdminPrivateComment());
        object.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE,
                request.getDateOfClosure());
        object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS,
                request.getStatus().name().toLowerCase());

        // Save the document
        saveGroupDocument(groupDoc, String.format("Accept join request from user [%s]", request.getUserId()));
    }

    @Override
    public void refuseRequest(MemberCandidacy request, String message, String privateComment)
        throws WikiUserManagerException
    {
        // Update the candidacy object
        XWikiContext xcontext = xcontextProvider.get();

        // Set the values
        request.setAdminId(documentReferenceSerializer.serialize(xcontext.getUserReference()));
        request.setAdminComment(message);
        request.setAdminPrivateComment(privateComment);
        request.setStatus(MemberCandidacy.Status.REJECTED);
        request.setDateOfClosure(new Date());

        // Get the group document
        XWikiDocument groupDoc = getMembersGroupDocument(request.getWikiId());

        // Avoid modifying the cached document
        groupDoc.clone();

        // Get the candidacy object
        BaseObject object = groupDoc.getXObject(WikiCandidateMemberClassInitializer.REFERENCE, request.getId());

        // Set the new values
        object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN, request.getAdminId());
        object.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_COMMENT, request.getAdminComment());
        object.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_PRIVATE_COMMENT,
                request.getAdminPrivateComment());
        object.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE,
                request.getDateOfClosure());
        object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS,
                request.getStatus().name().toLowerCase());

        // Save the document
        saveGroupDocument(groupDoc, String.format("Reject join request from user [%s]", request.getUserId()));
    }

    @Override
    public void cancelCandidacy(MemberCandidacy candidacy) throws WikiUserManagerException
    {
        // Get the group document
        XWikiDocument groupDoc = getMembersGroupDocument(candidacy.getWikiId());

        // Avoid modifying the cached document
        groupDoc.clone();

        // Get the candidacy object
        BaseObject object = groupDoc.getXObject(WikiCandidateMemberClassInitializer.REFERENCE, candidacy.getId());

        // Remove the candidacy, if any
        if (object != null) {
            groupDoc.removeXObject(object);
            saveGroupDocument(groupDoc, String.format("Candidacy [%d] is canceled.", candidacy.getId()));
        }
    }

    @Override
    public MemberCandidacy invite(String userId, String wikiId, String message)
        throws WikiUserManagerException
    {
        XWikiContext xcontext = xcontextProvider.get();

        // Create the candidacy
        MemberCandidacy candidacy = new MemberCandidacy(wikiId, userId, MemberCandidacy.CandidateType.INVITATION);
        candidacy.setUserComment(message);
        candidacy.setAdminId(documentReferenceSerializer.serialize(xcontext.getUserReference()));

        // Get the group document
        XWikiDocument groupDoc = getMembersGroupDocument(wikiId);

        // Avoid modifying the cached document
        groupDoc.clone();

        // Add a candidacy object
        try {
            int objectNumber = groupDoc.createXObject(WikiCandidateMemberClassInitializer.REFERENCE, xcontext);
            candidacy.setId(objectNumber);
            BaseObject object = groupDoc.getXObject(WikiCandidateMemberClassInitializer.REFERENCE, objectNumber);
            object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_USER, candidacy.getUserId());
            object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN, candidacy.getAdminId());
            object.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_COMMENT, message);
            object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS,
                    candidacy.getStatus().name().toLowerCase());
            object.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CREATION,
                    candidacy.getDateOfCreation());
            object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_TYPE,
                    candidacy.getType().name().toLowerCase());
        } catch (XWikiException e) {
            throw new WikiUserManagerException("Failed to create a new invitation object.", e);
        }

        // Save the document
        saveGroupDocument(groupDoc, String.format("[%s] is invited to join the wiki.", userId));

        return candidacy;
    }

    @Override
    public void acceptInvitation(MemberCandidacy invitation, String message) throws WikiUserManagerException
    {
        // Add the user to the members
        addMember(invitation.getUserId(), invitation.getWikiId());

        // Set the values
        invitation.setUserComment(message);
        invitation.setStatus(MemberCandidacy.Status.ACCEPTED);
        invitation.setDateOfClosure(new Date());

        // Get the group document
        XWikiDocument groupDoc = getMembersGroupDocument(invitation.getWikiId());

        // Avoid modifying the cached document
        groupDoc.clone();

        // Get the candidacy object
        BaseObject object = groupDoc.getXObject(WikiCandidateMemberClassInitializer.REFERENCE, invitation.getId());

        // Set the new values
        object.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_USER_COMMENT, invitation.getUserComment());
        object.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE, invitation.getDateOfClosure());
        object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS,
                invitation.getStatus().name().toLowerCase());

        // Save the document
        saveGroupDocument(groupDoc, String.format("User [%s] has accepted to join the wiki. ", invitation.getUserId()));
    }

    @Override
    public void refuseInvitation(MemberCandidacy invitation, String message) throws WikiUserManagerException
    {
        // Set the values
        invitation.setUserComment(message);
        invitation.setStatus(MemberCandidacy.Status.REJECTED);
        invitation.setDateOfClosure(new Date());

        // Get the group document
        XWikiDocument groupDoc = getMembersGroupDocument(invitation.getWikiId());

        // Avoid modifying the cached document
        groupDoc.clone();

        // Get the candidacy object
        BaseObject object = groupDoc.getXObject(WikiCandidateMemberClassInitializer.REFERENCE, invitation.getId());

        // Set the new values
        object.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_USER_COMMENT, invitation.getUserComment());
        object.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE, invitation.getDateOfClosure());
        object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS,
                invitation.getStatus().name().toLowerCase());

        // Save the document
        saveGroupDocument(groupDoc, String.format("User [%s] has rejected the invitation to join the wiki.",
                invitation.getUserId()));
    }
}
