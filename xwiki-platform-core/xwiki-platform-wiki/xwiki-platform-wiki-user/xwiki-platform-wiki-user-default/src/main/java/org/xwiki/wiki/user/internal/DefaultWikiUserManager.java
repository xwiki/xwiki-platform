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

import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.function.FailablePredicate;
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
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
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

    private DocumentReference getMembersGroupReference(String wikiId)
    {
        return new DocumentReference(wikiId, XWiki.SYSTEM_SPACE, "XWikiAllGroup");
    }

    private XWikiDocument getMembersGroupDocument(String wikiId) throws WikiUserManagerException
    {
        DocumentReference memberGroupReference = getMembersGroupReference(wikiId);

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

    /**
     * Applies the given update to the latest version of the member group document and saves it. Several requests can
     * modify the group at the same time (e.g. users joining the wiki), so the update is applied again to the newer
     * version of the group when another request saved it in the meantime.
     *
     * @param wikiId the wiki whose member group to update
     * @param update modifies the given copy of the group document, and returns {@code false} when there is nothing to
     *     save; it can be called several times
     * @param message the comment of the save
     */
    private void updateGroupDocument(String wikiId, FailablePredicate<XWikiDocument, XWikiException> update,
        String message) throws WikiUserManagerException
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        try {
            xwiki.updateDocument(getMembersGroupReference(wikiId), document -> {
                if (!update.test(document)) {
                    return false;
                }

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

                return true;
            }, message, false, xcontext);
        } catch (XWikiException e) {
            throw new WikiUserManagerException("Fail to save the member group", e);
        }
    }

    private List<String> getGroupMembers(XWikiDocument groupDoc)
    {
        List<String> members = new ArrayList<>();
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
        for (String member : getGroupMembers(getMembersGroupDocument(wikiId))) {
            if (!members.contains(member)) {
                members.add(member);
            }
        }

        return members;
    }

    @Override
    public boolean isMember(String userId, String wikiId) throws WikiUserManagerException
    {
        return getMembers(wikiId).contains(userId);
    }

    private void addMemberObject(XWikiDocument groupDoc, String userId) throws XWikiException
    {
        XWikiContext xcontext = xcontextProvider.get();
        int objectNumber = groupDoc.createXObject(GROUPCLASS_REFERENCE, xcontext);
        BaseObject object = groupDoc.getXObject(GROUPCLASS_REFERENCE, objectNumber);
        object.set(GROUP_CLASS_MEMBER_FIELD, userId, xcontext);
    }

    /**
     * Adds the given users to the given copy of the member group document.
     *
     * @return {@code true} if a user has been added, {@code false} if they were all members already
     */
    private boolean addMemberObjects(XWikiDocument groupDoc, Collection<String> userIds, Collection<String> members)
        throws XWikiException
    {
        List<String> newMembers = new ArrayList<>();
        for (String userId : userIds) {
            if (!members.contains(userId) && !newMembers.contains(userId)) {
                newMembers.add(userId);
            }
        }
        if (newMembers.isEmpty()) {
            return false;
        }

        // If the group does not contain any user yet, add an empty member (cf: XWIKI-6275).
        List<BaseObject> memberObjects = groupDoc.getXObjects(GROUPCLASS_REFERENCE);
        if (memberObjects == null || memberObjects.isEmpty()) {
            addMemberObject(groupDoc, "");
        }

        for (String userId : newMembers) {
            addMemberObject(groupDoc, userId);
        }

        return true;
    }

    @Override
    public void addMember(String userId, String wikiId) throws WikiUserManagerException
    {
        Collection<String> members = getMembers(wikiId);
        if (members.contains(userId)) {
            // Nothing to do !
            return;
        }

        // The user can have been added to the latest version of the group by another request.
        updateGroupDocument(wikiId, groupDoc -> addMemberObjects(groupDoc, List.of(userId), getGroupMembers(groupDoc)),
            String.format("Add [%s] to the group.", userId));
    }

    @Override
    public void addMembers(Collection<String> userIds, String wikiId) throws WikiUserManagerException
    {
        Collection<String> members = getMembers(wikiId);

        updateGroupDocument(wikiId, groupDoc -> {
            // The users can have been added to the latest version of the group by another request.
            List<String> currentMembers = new ArrayList<>(members);
            currentMembers.addAll(getGroupMembers(groupDoc));
            return addMemberObjects(groupDoc, userIds, currentMembers);
        }, "Add members to the group.");
    }

    /**
     * Removes the member objects of the given users from the given copy of the member group document.
     *
     * @return {@code false} if the group has no member object, {@code true} otherwise
     */
    private boolean removeMemberObjects(XWikiDocument groupDoc, Collection<String> userIds)
    {
        // Get the member objects
        List<BaseObject> objects = groupDoc.getXObjects(GROUPCLASS_REFERENCE);
        if (objects == null) {
            return false;
        }

        // Get the member objects to remove
        List<BaseObject> objectsToRemove = new ArrayList<>();
        for (BaseObject object : objects) {
            if (object != null && userIds.contains(object.getStringValue(GROUP_CLASS_MEMBER_FIELD))) {
                objectsToRemove.add(object);
            }
        }

        // Remove them
        for (BaseObject object : objectsToRemove) {
            groupDoc.removeXObject(object);
        }

        return true;
    }

    @Override
    public void removeMember(String userId, String wikiId) throws WikiUserManagerException
    {
        updateGroupDocument(wikiId, groupDoc -> removeMemberObjects(groupDoc, List.of(userId)),
            String.format("Remove [%s] from the group.", userId));
    }

    @Override
    public void removeMembers(Collection<String> userIds, String wikiId) throws WikiUserManagerException
    {
        updateGroupDocument(wikiId, groupDoc -> removeMemberObjects(groupDoc, userIds),
            "Remove some users from the group.");
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
        candidacy.setDateOfClosure(object.getDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE));

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
                if (Strings.CS.equals(invitation.getUserId(), userId)
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
                if (Strings.CS.equals(request.getUserId(), userId)
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

        // Get the candidacy. It can be null when the candidacy has been removed in the meantime (e.g. an invitation
        // that has been canceled) while a stale candidacy id is still being requested (e.g. on a form resubmission).
        BaseObject object = groupDoc.getXObject(WikiCandidateMemberClassInitializer.REFERENCE, candidacyId);
        return object != null ? readCandidacyFromObject(object, wikiId) : null;
    }

    @Override
    public MemberCandidacy askToJoin(String userId, String wikiId, String message)
        throws WikiUserManagerException
    {
        MemberCandidacy candidacy = new MemberCandidacy(wikiId, userId, MemberCandidacy.CandidateType.REQUEST);
        candidacy.setUserComment(message);

        // Add a candidacy object
        updateGroupDocument(wikiId, groupDoc -> {
            XWikiContext xcontext = xcontextProvider.get();
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
            return true;
        }, String.format("[%s] asks to join the wiki.", userId));

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

    /**
     * Copies the values set by the administrator who closed the given join request to its object in the given copy of
     * the member group document.
     */
    private void closeRequest(XWikiDocument groupDoc, MemberCandidacy request)
    {
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

        updateGroupDocument(request.getWikiId(), groupDoc -> {
            closeRequest(groupDoc, request);
            return true;
        }, String.format("Accept join request from user [%s]", request.getUserId()));
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

        updateGroupDocument(request.getWikiId(), groupDoc -> {
            closeRequest(groupDoc, request);
            return true;
        }, String.format("Reject join request from user [%s]", request.getUserId()));
    }

    @Override
    public void cancelCandidacy(MemberCandidacy candidacy) throws WikiUserManagerException
    {
        updateGroupDocument(candidacy.getWikiId(), groupDoc -> {
            // Get the candidacy object
            BaseObject object =
                groupDoc.getXObject(WikiCandidateMemberClassInitializer.REFERENCE, candidacy.getId());

            // Remove the candidacy, if any
            return object != null && groupDoc.removeXObject(object);
        }, String.format("Candidacy [%d] is canceled.", candidacy.getId()));
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

        // Add a candidacy object
        updateGroupDocument(wikiId, groupDoc -> {
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
            return true;
        }, String.format("[%s] is invited to join the wiki.", userId));

        return candidacy;
    }

    /**
     * Copies the values set by the user who answered the given invitation to its object in the given copy of the
     * member group document.
     */
    private void closeInvitation(XWikiDocument groupDoc, MemberCandidacy invitation)
    {
        // Get the candidacy object
        BaseObject object = groupDoc.getXObject(WikiCandidateMemberClassInitializer.REFERENCE, invitation.getId());

        // Set the new values
        object.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_USER_COMMENT, invitation.getUserComment());
        object.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE, invitation.getDateOfClosure());
        object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS,
                invitation.getStatus().name().toLowerCase());
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

        updateGroupDocument(invitation.getWikiId(), groupDoc -> {
            closeInvitation(groupDoc, invitation);
            return true;
        }, String.format("User [%s] has accepted to join the wiki. ", invitation.getUserId()));
    }

    @Override
    public void refuseInvitation(MemberCandidacy invitation, String message) throws WikiUserManagerException
    {
        // Set the values
        invitation.setUserComment(message);
        invitation.setStatus(MemberCandidacy.Status.REJECTED);
        invitation.setDateOfClosure(new Date());

        updateGroupDocument(invitation.getWikiId(), groupDoc -> {
            closeInvitation(groupDoc, invitation);
            return true;
        }, String.format("User [%s] has rejected the invitation to join the wiki.", invitation.getUserId()));
    }
}
