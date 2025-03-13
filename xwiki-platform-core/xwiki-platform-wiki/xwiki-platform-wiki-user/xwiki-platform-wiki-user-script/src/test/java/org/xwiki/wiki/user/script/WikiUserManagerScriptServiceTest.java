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
import java.util.Collections;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.MemberCandidacy;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.WikiUserManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.xwiki.security.authorization.Right.ADMIN;
import static org.xwiki.wiki.user.UserScope.LOCAL_ONLY;

/**
 * Unit tests for {@link WikiUserManagerScriptService}
 *
 * @version $Id$
 * @since 5.4RC1
 */
@ComponentTest
class WikiUserManagerScriptServiceTest
{
    @InjectMockComponents
    private WikiUserManagerScriptService wikiUserManagerScriptService;

    @MockComponent
    private WikiUserManager wikiUserManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private Execution execution;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWikiDocument currentDoc;

    private static final DocumentReference USER_DOC_REF = new DocumentReference("mainWiki", "XWiki", "User");

    @BeforeEach
    void setUp()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        when(this.execution.getContext()).thenReturn(new ExecutionContext());
        when(this.xcontext.getDoc()).thenReturn(this.currentDoc);
        when(this.xcontext.getUserReference()).thenReturn(USER_DOC_REF);
        DocumentReference userReference = new DocumentReference("mainWiki", "XWiki", "User");
        when(this.documentReferenceResolver.resolve("mainWiki:XWiki.User")).thenReturn(userReference);
        DocumentReference otherUser = new DocumentReference("mainWiki", "XWiki", "OtherUser");
        when(this.documentReferenceResolver.resolve("mainWiki:XWiki.OtherUser")).thenReturn(otherUser);
    }

    /**
     * Mocks the components to simulate that a non admin user have saved the current script.
     *
     * @return the exception expected when the current script has the not the admin right
     */
    private Exception currentScriptHasNotAdminRight() throws AccessDeniedException
    {
        DocumentReference authorDocRef = new DocumentReference("mainWiki", "XWiki", "NonAdmin");
        when(this.currentDoc.getAuthorReference()).thenReturn(authorDocRef);

        DocumentReference currentDocRef = new DocumentReference("subwiki", "Space", "PageToTest");
        when(this.currentDoc.getDocumentReference()).thenReturn(currentDocRef);

        Exception exception = new AccessDeniedException(ADMIN, authorDocRef, currentDocRef);
        doThrow(exception).when(this.authorizationManager).checkAccess(ADMIN, authorDocRef, currentDocRef);

        return exception;
    }

    /**
     * Mocks the components to simulate that the current user is not an admin.
     *
     * @return the exception expected when the current user has the not the admin right
     */
    private Exception currentUserHasNotAdminRight() throws AccessDeniedException
    {
        WikiReference wiki = new WikiReference("subwiki");
        Exception exception = new AccessDeniedException(ADMIN, USER_DOC_REF, wiki);

        doThrow(exception).when(this.authorizationManager).checkAccess(ADMIN, USER_DOC_REF, wiki);

        return exception;
    }

    @Test
    void getUserScope() throws Exception
    {
        when(this.wikiUserManager.getUserScope("subwiki")).thenReturn(UserScope.GLOBAL_ONLY);
        UserScope result = this.wikiUserManagerScriptService.getUserScope();
        assertEquals(UserScope.GLOBAL_ONLY, result);
    }

    @Test
    void getUserScopeWithError() throws Exception
    {
        // Mocks
        Exception expectedException = new WikiUserManagerException("Error in getUserScope");
        when(this.wikiUserManager.getUserScope("test")).thenThrow(expectedException);

        // Test
        UserScope result = this.wikiUserManagerScriptService.getUserScope("test");

        // Asserts
        assertNull(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void setUserScope() throws Exception
    {
        // Test
        boolean result = this.wikiUserManagerScriptService.setUserScope("subwiki", "LOCAL_ONLY");

        // Asserts
        assertTrue(result);
        verify(this.wikiUserManager).setUserScope("subwiki", LOCAL_ONLY);
    }

    @Test
    void setUserScopeWhenScriptHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.setUserScope("subwiki", "LOCAL_ONLY");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void setUserScopeWhenUserHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.setUserScope("subwiki", "LOCAL_ONLY");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void setUserScopeWhenWrongValue()
    {
        // Test
        boolean result = this.wikiUserManagerScriptService.setUserScope("subwiki", "wrong value");

        // Asserts
        assertFalse(result);
        assertInstanceOf(IllegalArgumentException.class, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void setUserScopeError() throws Exception
    {
        // Mocks
        WikiUserManagerException expectedException = new WikiUserManagerException("error in setUserScope");
        doThrow(expectedException).when(this.wikiUserManager).setUserScope(any(), any(UserScope.class));

        // Test
        boolean result = this.wikiUserManagerScriptService.setUserScope("subwiki", "LOCAL_ONLY");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void getMembershipType() throws Exception
    {
        // Mocks
        when(this.wikiUserManager.getMembershipType("subwiki")).thenReturn(MembershipType.INVITE);

        // Test
        MembershipType result = this.wikiUserManagerScriptService.getMembershipType();

        // Asserts
        assertEquals(MembershipType.INVITE, result);
    }

    @Test
    void getMembershipTypeWithError() throws Exception
    {
        // Mocks
        Exception expectedException = new WikiUserManagerException("Error in getMembershipType");
        when(this.wikiUserManager.getMembershipType("test")).thenThrow(expectedException);

        // Test
        MembershipType result = this.wikiUserManagerScriptService.getMembershipType("test");

        // Asserts
        assertNull(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void setMembershipType()
    {
        // Test
        boolean result = this.wikiUserManagerScriptService.setUserScope("subwiki", "LOCAL_ONLY");

        // Asserts
        assertTrue(result);
    }

    @Test
    void setMembershipTypeWhenScriptHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.setMembershipType("subwiki", "INVITE");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void setMembershipTypeWhenUserHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedExtension = currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.setMembershipType("subwiki", "INVITE");

        // Asserts
        assertFalse(result);
        assertEquals(expectedExtension, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void setMembershipTypeWrongValue()
    {
        // Test
        boolean result = this.wikiUserManagerScriptService.setMembershipType("subwiki", "wrong value");

        // Asserts
        assertFalse(result);
        assertInstanceOf(IllegalArgumentException.class, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void setMembershipTypeError() throws Exception
    {
        // Mocks
        WikiUserManagerException expectedException = new WikiUserManagerException("error in setMembershipType");
        doThrow(expectedException).when(this.wikiUserManager).setMembershipType(any(), any(MembershipType.class));

        // Test
        boolean result = this.wikiUserManagerScriptService.setMembershipType("subwiki", "INVITE");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void getMembers() throws Exception
    {
        // Mocks
        Collection<String> members = Collections.emptyList();
        when(this.wikiUserManager.getMembers("subwiki")).thenReturn(members);

        // Test
        Collection<String> result = this.wikiUserManagerScriptService.getMembers("subwiki");

        // Asserts
        assertEquals(members, result);
    }

    @Test
    void getMembersError() throws Exception
    {
        // Mocks
        Exception expectedException = new WikiUserManagerException("error in getMembers");
        when(this.wikiUserManager.getMembers("subwiki")).thenThrow(expectedException);

        // Test
        Collection<String> result = this.wikiUserManagerScriptService.getMembers("subwiki");

        // Asserts
        assertNull(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void isMember() throws Exception
    {
        // Mocks
        when(this.wikiUserManager.isMember("mainWiki:XWiki.User", "subwiki")).thenReturn(true);
        when(this.wikiUserManager.isMember("mainWiki:XWiki.User", "subwiki2")).thenReturn(false);

        // Test
        boolean result1 = this.wikiUserManagerScriptService.isMember("mainWiki:XWiki.User", "subwiki");
        boolean result2 = this.wikiUserManagerScriptService.isMember("mainWiki:XWiki.User", "subwiki2");

        // Asserts
        assertTrue(result1);
        assertFalse(result2);
    }

    @Test
    void isMemberError() throws Exception
    {
        // Mocks
        Exception expectedException = new WikiUserManagerException("error in isMember");
        when(this.wikiUserManager.isMember("mainWiki:XWiki.User", "subwiki")).thenThrow(expectedException);

        // Test
        Boolean result = this.wikiUserManagerScriptService.isMember("mainWiki:XWiki.User", "subwiki");

        // Asserts
        assertNull(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void addMember() throws Exception
    {
        // Test
        boolean result = this.wikiUserManagerScriptService.addMember("xwiki:XWiki.UserA", "subwiki");

        // Asserts
        assertTrue(result);
        verify(this.wikiUserManager).addMember("xwiki:XWiki.UserA", "subwiki");
    }

    @Test
    void addMemberWhenScriptHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.addMember("xwiki:XWiki.UserA", "subwiki");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void addMemberWhenUserHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.addMember("xwiki:XWiki.UserA", "subwiki");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void addMembers() throws Exception
    {
        // Test
        Collection<String> userIds = new ArrayList<>();
        boolean result = this.wikiUserManagerScriptService.addMembers(userIds, "subwiki");

        // Asserts
        assertTrue(result);
        verify(this.wikiUserManager).addMembers(userIds, "subwiki");
    }

    @Test
    void addMembersWhenScriptHasNoRight() throws Exception
    {
        // Mock
        Exception expectedExtension = currentScriptHasNotAdminRight();

        // Test
        Collection<String> userIds = new ArrayList<>();
        boolean result = this.wikiUserManagerScriptService.addMembers(userIds, "subwiki");

        // Asserts
        assertFalse(result);
        assertEquals(expectedExtension, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void addMembersWhenUserHasNoRight() throws Exception
    {
        // Mock
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        Collection<String> userIds = new ArrayList<>();
        boolean result = this.wikiUserManagerScriptService.addMembers(userIds, "subwiki");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void removeMember() throws Exception
    {
        // Test
        boolean result = this.wikiUserManagerScriptService.removeMember("xwiki:XWiki.UserA", "subwiki");

        // Asserts
        assertTrue(result);
        verify(this.wikiUserManager).removeMember("xwiki:XWiki.UserA", "subwiki");
    }

    @Test
    void removeMemberWhenScriptHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.removeMember("xwiki:XWiki.UserA", "subwiki");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void removeMemberWhenUserHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.removeMember("xwiki:XWiki.UserA", "subwiki");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void getCandidacyAsAdmin() throws Exception
    {
        // Mocks
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.REQUEST);
        candidacy.setId(12);
        candidacy.setAdminPrivateComment("private message");
        when(this.wikiUserManager.getCandidacy("subwiki", candidacy.getId())).thenReturn(candidacy);
        when(this.authorizationManager.hasAccess(ADMIN, USER_DOC_REF, new WikiReference("subwiki"))).thenReturn(true);

        // Test
        MemberCandidacy result = this.wikiUserManagerScriptService.getCandidacy("subwiki", 12);

        // Asserts
        assertEquals(candidacy, result);
        assertEquals("private message", result.getAdminPrivateComment());
    }

    @Test
    void getCandidacyAsUserConcerned() throws Exception
    {
        // Mocks

        // Here, the candidate is the current user
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.REQUEST);
        candidacy.setId(12);
        candidacy.setAdminPrivateComment("some private message that I should not be able to see");

        when(this.wikiUserManager.getCandidacy("subwiki", candidacy.getId())).thenReturn(candidacy);

        // Test
        MemberCandidacy result = this.wikiUserManagerScriptService.getCandidacy("subwiki", 12);

        // Asserts
        assertEquals(candidacy, result);
        // Verify that the private message has been removed from the candidacy
        assertNull(candidacy.getAdminPrivateComment());
    }

    @Test
    void getCandidacyWhenNoRight() throws Exception
    {
        // Mocks

        // The current user is not the candidate
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.REQUEST);
        candidacy.setId(12);
        when(this.wikiUserManager.getCandidacy("subwiki", candidacy.getId())).thenReturn(candidacy);

        // The current user does not have ADMIN right
        when(this.authorizationManager.hasAccess(ADMIN, USER_DOC_REF, new WikiReference("subwiki"))).thenReturn(false);

        // Test
        MemberCandidacy result = this.wikiUserManagerScriptService.getCandidacy("subwiki", 12);

        // Asserts
        assertNull(result);
        Exception exception = this.wikiUserManagerScriptService.getLastError();
        assertInstanceOf(WikiUserManagerScriptServiceException.class, exception);
        assertEquals("You are not allowed to see this candidacy.", exception.getMessage());
    }

    @Test
    void getCandidacyWhenGuest() throws Exception
    {
        // Mocks

        // The current user is Guest
        when(this.xcontext.getUserReference()).thenReturn(null);

        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
            MemberCandidacy.CandidateType.REQUEST);
        candidacy.setId(12);
        when(this.wikiUserManager.getCandidacy("subwiki", candidacy.getId())).thenReturn(candidacy);


        // The current user does not have ADMIN right
        when(this.authorizationManager.hasAccess(ADMIN, USER_DOC_REF, new WikiReference("subwiki"))).thenReturn(false);

        // Test
        MemberCandidacy result = this.wikiUserManagerScriptService.getCandidacy("subwiki", 12);

        // Asserts
        assertNull(result);
        Exception exception = this.wikiUserManagerScriptService.getLastError();
        assertInstanceOf(WikiUserManagerScriptServiceException.class, exception);
        assertEquals("You are not allowed to see this candidacy.", exception.getMessage());
    }

    @Test
    void getCandidacyWhenError() throws Exception
    {
        // Mocks
        Exception exception = new WikiUserManagerException("error in getCandidacy");
        when(this.wikiUserManager.getCandidacy("subwiki", 42)).thenThrow(exception);

        // Test
        MemberCandidacy result = this.wikiUserManagerScriptService.getCandidacy("subwiki", 42);

        // Asserts
        assertNull(result);
        assertEquals(exception, this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void getAllInvitations() throws Exception
    {
        ArrayList<MemberCandidacy> candidacies = new ArrayList<>();
        // the first candidacy concerns the current user
        candidacies.add(new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION));
        // not the second
        candidacies.add(new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.INVITATION));
        candidacies.get(0).setAdminPrivateComment("private message");

        // We do not have admin rights
        when(this.authorizationManager.hasAccess(ADMIN, USER_DOC_REF, new WikiReference("subwiki"))).thenReturn(false);

        when(this.wikiUserManager.getAllInvitations("subwiki")).thenReturn(candidacies);

        // Test
        Collection<MemberCandidacy> result = this.wikiUserManagerScriptService.getAllInvitations("subwiki");

        // the result must have been filtered
        assertEquals(1, result.size());
        assertTrue(result.contains(candidacies.get(0)));
        assertFalse(result.contains(candidacies.get(1)));
        // The private message from the candidacy must be removed
        assertNull(((MemberCandidacy)result.toArray()[0]).getAdminPrivateComment());
    }

    @Test
    void getAllInvitationsError() throws Exception
    {
        // Mocks
        Exception expectedException = new WikiUserManagerException("error in getAllInvitations()");
        when(this.wikiUserManager.getAllInvitations("subwiki")).thenThrow(expectedException);

        // Test
        Collection<MemberCandidacy> result = this.wikiUserManagerScriptService.getAllInvitations("subwiki");

        // Asserts
        assertNull(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void getAllRequests() throws Exception
    {
        ArrayList<MemberCandidacy> candidacies = new ArrayList<>();
        // the first candidacy concerns the current user
        candidacies.add(new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.REQUEST));
        // not the second
        candidacies.add(new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.REQUEST));
        candidacies.get(0).setAdminPrivateComment("private message");

        // We do not have admin rights
        when(this.authorizationManager.hasAccess(ADMIN, USER_DOC_REF, new WikiReference("subwiki"))).thenReturn(false);

        when(this.wikiUserManager.getAllRequests("subwiki")).thenReturn(candidacies);

        // Test
        Collection<MemberCandidacy> result = this.wikiUserManagerScriptService.getAllRequests("subwiki");

        // the result must have been filtered
        assertEquals(1, result.size());
        assertTrue(result.contains(candidacies.get(0)));
        assertFalse(result.contains(candidacies.get(1)));
        // The private message from the candidacy must be removed
        assertNull(((MemberCandidacy)result.toArray()[0]).getAdminPrivateComment());
    }

    @Test
    void getAllRequestError() throws Exception
    {
        // Mocks
        Exception expectedException = new WikiUserManagerException("error in getAllRequests()");
        when(this.wikiUserManager.getAllRequests("subwiki")).thenThrow(expectedException);

        // Test
        Collection<MemberCandidacy> result = this.wikiUserManagerScriptService.getAllRequests("subwiki");

        // Asserts
        assertNull(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void join() throws Exception
    {
        // Test
        String userId = "mainWiki:XWiki.User";
        String wikiId = "wikiId";
        boolean result = this.wikiUserManagerScriptService.join(userId, wikiId);

        // Asserts
        assertTrue(result);
        verify(this.wikiUserManager).join(userId, wikiId);
    }

    @Test
    void joinWhenUserIsNotCurrentUser()
    {
        // Test
        String userId = "mainWiki:XWiki.OtherUser";
        String wikiId = "wikiId";
        boolean result = this.wikiUserManagerScriptService.join(userId, wikiId);

        // Asserts
        assertFalse(result);
        assertEquals("User [mainWiki:XWiki.User] cannot call $services.wiki.user.join() with an other userId.",
            this.wikiUserManagerScriptService.getLastError().getMessage());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void joinWhenError() throws Exception
    {
        String userId = "mainWiki:XWiki.User";
        String wikiId = "wikiId";

        // Mocks
        WikiUserManagerException expectedException = new WikiUserManagerException("error in wikiUserManager#join()");
        doThrow(expectedException).when(this.wikiUserManager).join(userId, wikiId);

        // Test
        boolean result = this.wikiUserManagerScriptService.join(userId, wikiId);

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void leave() throws Exception
    {
        String userId = "mainWiki:XWiki.User";
        String wikiId = "wikiId";

        // Test
        boolean result = this.wikiUserManagerScriptService.leave(userId, wikiId);

        // Asserts
        assertTrue(result);
        verify(this.wikiUserManager).leave(userId, wikiId);
    }

    @Test
    void leaveWhenUserIsNotCurrentUser()
    {
        String userId = "mainWiki:XWiki.OtherUser";
        String wikiId = "wikiId";

        // Test
        boolean result = this.wikiUserManagerScriptService.leave(userId, wikiId);

        // Asserts
        assertFalse(result);
        assertEquals("User [mainWiki:XWiki.User] cannot call $services.wiki.user.leave() with an other userId.",
            this.wikiUserManagerScriptService.getLastError().getMessage());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void leaveWhenError() throws Exception
    {
        String userId = "mainWiki:XWiki.User";
        String wikiId = "wikiId";

        // Mocks
        WikiUserManagerException expectedException = new WikiUserManagerException("error in wikiUserManager#leave()");
        doThrow(expectedException).when(this.wikiUserManager).leave(userId, wikiId);

        // Test
        boolean result = this.wikiUserManagerScriptService.leave(userId, wikiId);

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void hasPendingInvitation() throws Exception
    {
        String wikiId = "subwiki";

        // First test
        when(this.wikiUserManager.hasPendingInvitation(USER_DOC_REF, wikiId)).thenReturn(true);
        assertTrue(this.wikiUserManagerScriptService.hasPendingInvitation(USER_DOC_REF, wikiId));

        // Second test
        when(this.wikiUserManager.hasPendingInvitation(USER_DOC_REF, wikiId)).thenReturn(false);
        assertFalse(this.wikiUserManagerScriptService.hasPendingInvitation(USER_DOC_REF, wikiId));
    }

    @Test
    void hasPendingInvitationWhenError() throws Exception
    {
        String wikiId = "subwiki";

        // Mocks
        DocumentReference userToTest = new DocumentReference("mainWiki", "XWiki", "User");
        Exception expectedException = new WikiUserManagerException("exception");
        doThrow(expectedException).when(this.wikiUserManager).hasPendingInvitation(userToTest, wikiId);

        // Test
        Boolean result = this.wikiUserManagerScriptService.hasPendingInvitation(userToTest, wikiId);

        // Asserts
        assertNull(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void hasPendingInvitationWhenPageHasNoRight() throws Exception
    {
        String wikiId = "subwiki";
        DocumentReference userToTest = new DocumentReference("mainWiki", "XWiki", "User");

        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();

        // Test
        Boolean result = this.wikiUserManagerScriptService.hasPendingInvitation(userToTest, wikiId);

        // Asserts
        assertNull(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void hasPendingRequest() throws Exception
    {
        String wikiId = "subwiki";

        // First test
        when(this.wikiUserManager.hasPendingRequest(USER_DOC_REF, wikiId)).thenReturn(true);
        assertTrue(this.wikiUserManagerScriptService.hasPendingRequest(USER_DOC_REF, wikiId));

        // Second test
        when(this.wikiUserManager.hasPendingRequest(USER_DOC_REF, wikiId)).thenReturn(false);
        assertFalse(this.wikiUserManagerScriptService.hasPendingRequest(USER_DOC_REF, wikiId));
    }

    @Test
    void hasPendingRequestWhenError() throws Exception
    {
        String wikiId = "subwiki";
        DocumentReference userToTest = new DocumentReference("mainWiki", "XWiki", "User");

        // Mocks
        Exception expectedException = new WikiUserManagerException("exception");
        doThrow(expectedException).when(this.wikiUserManager).hasPendingRequest(userToTest, wikiId);

        // Test
        Boolean result = this.wikiUserManagerScriptService.hasPendingRequest(userToTest, wikiId);

        // Asserts
        assertNull(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());

    }

    @Test
    void hasPendingRequestWhenScriptHasNoRight() throws Exception
    {
        String wikiId = "subwiki";
        DocumentReference userToTest = new DocumentReference("mainWiki", "XWiki", "User");

        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();

        // Test
        Boolean result = this.wikiUserManagerScriptService.hasPendingRequest(userToTest, wikiId);

        // Asserts
        assertNull(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void acceptRequest() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Test
        boolean result = this.wikiUserManagerScriptService.acceptRequest(candidacy, "message", "comment");

        // Asserts
        assertTrue(result);
        assertNull(this.wikiUserManagerScriptService.getLastError());
        verify(this.wikiUserManager).acceptRequest(candidacy, "message", "comment");
    }

    @Test
    void acceptRequestWhenUserHasNoAdminRight() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.INVITATION);

        // Mocks
        Exception expecyedException = currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.acceptRequest(candidacy, "message", "comment");

        // Asserts
        assertFalse(result);
        assertEquals(expecyedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void acceptRequestWhenNoAdminRightButConcerned() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Mocks
        currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.acceptRequest(candidacy, "message", "comment");

        // Asserts
        assertTrue(result);
        assertNull(this.wikiUserManagerScriptService.getLastError());
        verify(this.wikiUserManager).acceptRequest(candidacy, "message", "comment");
    }

    @Test
    void askToJoin() throws Exception
    {
        // Mocks
        MemberCandidacy candidacy = new MemberCandidacy();
        when(this.wikiUserManager.askToJoin("mainWiki:XWiki.User", "subwiki", "please!")).thenReturn(candidacy);

        // Test
        MemberCandidacy result =
            this.wikiUserManagerScriptService.askToJoin("mainWiki:XWiki.User", "subwiki", "please!");

        // Asserts
        assertEquals(candidacy, result);
        assertNull(this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void askToJoinWhenScriptHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();

        // Test
        MemberCandidacy result =
            this.wikiUserManagerScriptService.askToJoin("mainWiki:XWiki.User", "subwiki", "please!");

        // Asserts
        assertNull(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void askToJoinWhenUsertHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        MemberCandidacy result = this.wikiUserManagerScriptService.askToJoin("mainWiki:XWiki.OtherUser",
                "subwiki", "please!");

        // Asserts
        assertNull(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void askToJoinWhenUserHasNoRightButConcerned() throws Exception
    {
        // Mocks
        currentUserHasNotAdminRight();
        MemberCandidacy candidacy = new MemberCandidacy();
        when(this.wikiUserManager.askToJoin("mainWiki:XWiki.User", "subwiki", "please!")).thenReturn(candidacy);

        // Test
        MemberCandidacy result = this.wikiUserManagerScriptService.askToJoin("mainWiki:XWiki.User",
                "subwiki", "please!");

        // Asserts
        assertEquals(candidacy, result);
        assertNull(this.wikiUserManagerScriptService.getLastError());
    }

    @Test
    void refuseRequest() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Test
        boolean result = this.wikiUserManagerScriptService.refuseRequest(candidacy, "message", "comment");

        // Asserts
        assertTrue(result);
        assertNull(this.wikiUserManagerScriptService.getLastError());
        verify(this.wikiUserManager).refuseRequest(candidacy, "message", "comment");
    }

    @Test
    void refuseRequestWhenUserHasNoAdminRight() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.INVITATION);

        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.refuseRequest(candidacy, "message", "comment");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void refuseRequestWhenUserHasNoAdminRightButConcerned() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Mocks
        currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.refuseRequest(candidacy, "message", "comment");

        // Asserts
        assertTrue(result);
        assertNull(this.wikiUserManagerScriptService.getLastError());
        verify(this.wikiUserManager).refuseRequest(candidacy, "message", "comment");
    }

    @Test
    void cancelCandidacy() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Test
        boolean result = this.wikiUserManagerScriptService.cancelCandidacy(candidacy);

        // Asserts
        assertTrue(result);
        assertNull(this.wikiUserManagerScriptService.getLastError());
        verify(this.wikiUserManager).cancelCandidacy(candidacy);

    }

    @Test
    void cancelCandidacyWhenUserHasNoAdminRight() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.INVITATION);

        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.cancelCandidacy(candidacy);

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void cancelCandidacyWhenUserHasNoAdminRightButConcerned() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Mocks
        currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.cancelCandidacy(candidacy);

        // Asserts
        assertTrue(result);
        assertNull(this.wikiUserManagerScriptService.getLastError());
        verify(this.wikiUserManager).cancelCandidacy(candidacy);
    }

    @Test
    void invite() throws Exception
    {
        // Mocks
        when(this.wikiUserManager.invite(any(), any(), any())).thenReturn(new MemberCandidacy());

        // Test
        MemberCandidacy result = this.wikiUserManagerScriptService.invite("someUser", "subwiki", "someMessage");

        // Asserts
        assertNotNull(result);
    }

    @Test
    void inviteWhenUserHasNoAdminRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        MemberCandidacy result = this.wikiUserManagerScriptService.invite("someUser", "subwiki", "someMessage");

        // Asserts
        assertNull(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void acceptInvitation() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Test
        boolean result = this.wikiUserManagerScriptService.acceptInvitation(candidacy, "thanks");

        // Asserts
        assertTrue(result);
        assertNull(this.wikiUserManagerScriptService.getLastError());
        verify(this.wikiUserManager).acceptInvitation(candidacy, "thanks");
    }

    @Test
    void acceptInvitationWhenUserHasNoAdminRight() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.INVITATION);

        // Mocks
        Exception exception = currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.acceptInvitation(candidacy, "thanks");

        // Asserts
        assertFalse(result);
        assertEquals(exception, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void acceptInvitationWhenUserHasNoAdminRightButConcerned() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Mocks
        currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.acceptInvitation(candidacy, "thanks");

        // Asserts
        assertTrue(result);
        assertNull(this.wikiUserManagerScriptService.getLastError());
        verify(this.wikiUserManager).acceptInvitation(candidacy, "thanks");
    }

    @Test
    void refuseInvitation() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Test
        boolean result = this.wikiUserManagerScriptService.refuseInvitation(candidacy, "no thanks");

        // Asserts
        assertTrue(result);
        assertNull(this.wikiUserManagerScriptService.getLastError());
        verify(this.wikiUserManager).refuseInvitation(candidacy, "no thanks");

    }

    @Test
    void refuseInvitationWhenUserHasNoAdminRight() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.INVITATION);

        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.refuseInvitation(candidacy, "no thanks");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.wikiUserManagerScriptService.getLastError());
        verifyNoInteractions(this.wikiUserManager);
    }

    @Test
    void refuseInvitationWhenUserHasNoAdminRightButConcerned() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Mocks
        currentUserHasNotAdminRight();

        // Test
        boolean result = this.wikiUserManagerScriptService.refuseInvitation(candidacy, "no thanks");

        // Asserts
        assertTrue(result);
        assertNull(this.wikiUserManagerScriptService.getLastError());
        verify(this.wikiUserManager).refuseInvitation(candidacy, "no thanks");
    }
}
