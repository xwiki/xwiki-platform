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

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.MemberCandidacy;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.WikiUserManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.user.script.WikiUserManagerScriptService}
 *
 * @version $Id$
 * @since 5.4RC1
 */
public class WikiUserManagerScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<WikiUserManagerScriptService> mocker =
            new MockitoComponentMockingRule(WikiUserManagerScriptService.class);

    private WikiUserManager wikiUserManager;

    private WikiDescriptorManager wikiDescriptorManager;

    private AuthorizationManager authorizationManager;

    private Provider<XWikiContext> xcontextProvider;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Execution execution;

    private ExecutionContext executionContext;

    private XWikiContext xcontext;

    private XWikiDocument currentDoc;

    private DocumentReference userDocRef;

    @Before
    public void setUp() throws Exception
    {
        // Components mocks
        wikiUserManager = mocker.getInstance(WikiUserManager.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        authorizationManager = mocker.getInstance(AuthorizationManager.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        entityReferenceSerializer = mocker.getInstance(new DefaultParameterizedType(null,
                EntityReferenceSerializer.class, String.class));
        execution = mocker.getInstance(Execution.class);

        // Frequent uses
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");

        executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);

        currentDoc = mock(XWikiDocument.class);
        when(xcontext.getDoc()).thenReturn(currentDoc);

        userDocRef = new DocumentReference("mainWiki", "XWiki", "User");
        when(xcontext.getUserReference()).thenReturn(userDocRef);

        when(entityReferenceSerializer.serialize(eq(userDocRef))).thenReturn("mainWiki:XWiki.User");
    }

    /**
     * @return the exception expected when the current script has the not the programing right
     */
    private Exception currentScriptHasNotProgrammingRight() throws AccessDeniedException
    {
        DocumentReference authorDocRef = new DocumentReference("mainWiki", "XWiki", "Admin");
        when(currentDoc.getAuthorReference()).thenReturn(authorDocRef);
        DocumentReference currentDocRef = new DocumentReference("subwiki", "Test", "test");
        when(currentDoc.getDocumentReference()).thenReturn(currentDocRef);

        Exception exception = new AccessDeniedException(Right.PROGRAM, authorDocRef, currentDocRef);
        doThrow(exception).when(authorizationManager).checkAccess(Right.PROGRAM, authorDocRef, currentDocRef);

        return exception;
    }

    /**
     * @return the exception expected when the current user has the not the admin right
     */
    private Exception currentUserHasNotAdminRight() throws AccessDeniedException
    {
        WikiReference wiki = new WikiReference("subwiki");
        Exception exception = new AccessDeniedException(Right.ADMIN, userDocRef, wiki);
        doThrow(exception).when(authorizationManager).checkAccess(eq(Right.ADMIN), eq(userDocRef), eq(wiki));

        return exception;
    }

    @Test
    public void getUserScope() throws Exception
    {
        when(wikiUserManager.getUserScope("subwiki")).thenReturn(UserScope.GLOBAL_ONLY);
        UserScope result = mocker.getComponentUnderTest().getUserScope();
        assertEquals(UserScope.GLOBAL_ONLY, result);
    }

    @Test
    public void getUserScopeWithError() throws Exception
    {
        Exception exception = new WikiUserManagerException("Error in getUserScope");
        when(wikiUserManager.getUserScope("test")).thenThrow(exception);
        UserScope result = mocker.getComponentUnderTest().getUserScope("test");
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void setUserScope() throws Exception
    {
        boolean result = mocker.getComponentUnderTest().setUserScope("subwiki", "LOCAL_ONLY");
        assertEquals(true, result);
    }

    @Test
    public void setUserScopeWithoutPR() throws Exception
    {
        // Current script has not the programming right
        Exception exception = currentScriptHasNotProgrammingRight();

        boolean result = mocker.getComponentUnderTest().setUserScope("subwiki", "LOCAL_ONLY");
        assertEquals(false, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void setUserScopeWithoutAdminRight() throws Exception
    {
        // Current script has not the admin right
        Exception exception = currentUserHasNotAdminRight();

        boolean result = mocker.getComponentUnderTest().setUserScope("subwiki", "LOCAL_ONLY");
        assertEquals(false, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void setUserScopeWrongValue() throws Exception
    {
        boolean result = mocker.getComponentUnderTest().setUserScope("subwiki", "wrong value");
        assertEquals(false, result);
        assertTrue(mocker.getComponentUnderTest().getLastError() instanceof IllegalArgumentException);
    }

    @Test
    public void setUserScopeError() throws Exception
    {
        WikiUserManagerException exception = new WikiUserManagerException("error in setUserScope");
        doThrow(exception).when(wikiUserManager).setUserScope(anyString(), any(UserScope.class));

        boolean result = mocker.getComponentUnderTest().setUserScope("subwiki", "LOCAL_ONLY");
        assertEquals(false, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void getMembershipType() throws Exception
    {
        when(wikiUserManager.getMembershipType("subwiki")).thenReturn(MembershipType.INVITE);
        MembershipType result = mocker.getComponentUnderTest().getMembershipType();
        assertEquals(MembershipType.INVITE, result);
    }

    @Test
    public void getMembershipTypeWithError() throws Exception
    {
        Exception exception = new WikiUserManagerException("Error in getMembershipType");
        when(wikiUserManager.getMembershipType("test")).thenThrow(exception);
        MembershipType result = mocker.getComponentUnderTest().getMembershipType("test");
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void setMembershipType() throws Exception
    {
        boolean result = mocker.getComponentUnderTest().setUserScope("subwiki", "LOCAL_ONLY");
        assertEquals(true, result);
    }

    @Test
    public void setMembershipTypeWithoutPR() throws Exception
    {
        // Current script has not the programming right
        Exception exception = currentScriptHasNotProgrammingRight();

        boolean result = mocker.getComponentUnderTest().setMembershipType("subwiki", "INVITE");
        assertEquals(false, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void setMembershipTypeWithoutAdminRight() throws Exception
    {
        // Current script has not the admin right
        Exception exception = currentUserHasNotAdminRight();

        boolean result = mocker.getComponentUnderTest().setMembershipType("subwiki", "INVITE");
        assertEquals(false, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void setMembershipTypeWrongValue() throws Exception
    {
        boolean result = mocker.getComponentUnderTest().setMembershipType("subwiki", "wrong value");
        assertEquals(false, result);
        assertTrue(mocker.getComponentUnderTest().getLastError() instanceof IllegalArgumentException);
    }

    @Test
    public void setMembershipTypeError() throws Exception
    {
        WikiUserManagerException exception = new WikiUserManagerException("error in setMembershipType");
        doThrow(exception).when(wikiUserManager).setMembershipType(anyString(), any(MembershipType.class));

        boolean result = mocker.getComponentUnderTest().setMembershipType("subwiki", "INVITE");
        assertEquals(false, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void getMembers() throws Exception
    {
        Collection<String> members = new ArrayList<String>();
        when(wikiUserManager.getMembers("subwiki")).thenReturn(members);
        Collection<String> result = mocker.getComponentUnderTest().getMembers("subwiki");
        assertEquals(members, result);
    }

    @Test
    public void getMembersError() throws Exception
    {
        Exception exception = new WikiUserManagerException("error in getMembers");
        when(wikiUserManager.getMembers("subwiki")).thenThrow(exception);
        Collection<String> result = mocker.getComponentUnderTest().getMembers("subwiki");
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void isMember() throws Exception
    {
        when(wikiUserManager.isMember("mainWiki:XWiki.User", "subwiki")).thenReturn(true);
        boolean result = mocker.getComponentUnderTest().isMember("mainWiki:XWiki.User", "subwiki");
        assertTrue(result);

        when(wikiUserManager.isMember("mainWiki:XWiki.User", "subwiki2")).thenReturn(false);
        result = mocker.getComponentUnderTest().isMember("mainWiki:XWiki.User", "subwiki2");
        assertFalse(result);
    }

    @Test
    public void isMemberError() throws Exception
    {
        Exception exception = new WikiUserManagerException("error in isMember");
        when(wikiUserManager.isMember("mainWiki:XWiki.User", "subwiki")).thenThrow(exception);
        Boolean result = mocker.getComponentUnderTest().isMember("mainWiki:XWiki.User", "subwiki");
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void addMember() throws Exception
    {
        boolean result = mocker.getComponentUnderTest().addMember("xwiki:XWiki.UserA", "subwiki");
        assertTrue(result);

        verify(wikiUserManager).addMember("xwiki:XWiki.UserA", "subwiki");
    }

    @Test
    public void addMemberWithoutPR() throws Exception
    {
        // Current script has not the programming right
        Exception exception = currentScriptHasNotProgrammingRight();

        boolean result = mocker.getComponentUnderTest().addMember("xwiki:XWiki.UserA", "subwiki");
        assertEquals(false, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void addMemberWithoutAdminRight() throws Exception
    {
        // Current script has not the admin right
        Exception exception = currentUserHasNotAdminRight();

        boolean result = mocker.getComponentUnderTest().addMember("xwiki:XWiki.UserA", "subwiki");
        assertEquals(false, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void addMembers() throws Exception
    {
        Collection<String> userIds = new ArrayList<String>();
        boolean result = mocker.getComponentUnderTest().addMembers(userIds, "subwiki");
        assertTrue(result);

        verify(wikiUserManager).addMembers(userIds, "subwiki");
    }

    @Test
    public void addMembersWithoutPR() throws Exception
    {
        // Current script has not the programming right
        Exception exception = currentScriptHasNotProgrammingRight();

        Collection<String> userIds = new ArrayList<String>();
        boolean result = mocker.getComponentUnderTest().addMembers(userIds, "subwiki");
        assertEquals(false, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void addMembersWithoutAdminRight() throws Exception
    {
        // Current script has not the admin right
        Exception exception = currentUserHasNotAdminRight();

        Collection<String> userIds = new ArrayList<String>();
        boolean result = mocker.getComponentUnderTest().addMembers(userIds, "subwiki");
        assertEquals(false, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void removeMember() throws Exception
    {
        boolean result = mocker.getComponentUnderTest().removeMember("xwiki:XWiki.UserA", "subwiki");
        assertTrue(result);

        verify(wikiUserManager).removeMember("xwiki:XWiki.UserA", "subwiki");
    }

    @Test
    public void removeMemberWithoutPR() throws Exception
    {
        // Current script has not the programming right
        Exception exception = currentScriptHasNotProgrammingRight();

        boolean result = mocker.getComponentUnderTest().removeMember("xwiki:XWiki.UserA", "subwiki");
        assertEquals(false, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void removeMemberWithoutAdminRight() throws Exception
    {
        // Current script has not the admin right
        Exception exception = currentUserHasNotAdminRight();

        boolean result = mocker.getComponentUnderTest().removeMember("xwiki:XWiki.UserA", "subwiki");
        assertEquals(false, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void getCandidacyAsAdmin() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.UserA",
                MemberCandidacy.CandidateType.REQUEST);
        candidacy.setId(12);
        when(wikiUserManager.getCandidacy("subwiki", candidacy.getId())).thenReturn(candidacy);
        when(authorizationManager.hasAccess(eq(Right.ADMIN), eq(userDocRef),
                eq(new WikiReference("subwiki")))).thenReturn(true);

        MemberCandidacy result = mocker.getComponentUnderTest().getCandidacy("subwiki", 12);
        assertEquals(candidacy, result);
    }

    @Test
    public void getCandidacyAsUserConcerned() throws Exception
    {
        // Here, the candidate is the current user
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.REQUEST);
        candidacy.setId(12);

        when(wikiUserManager.getCandidacy("subwiki", candidacy.getId())).thenReturn(candidacy);

        MemberCandidacy result = mocker.getComponentUnderTest().getCandidacy("subwiki", 12);
        assertEquals(candidacy, result);
    }

    @Test
    public void getCandidacyWhenNoRight() throws Exception
    {
        // The current user is not the candidate
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.UserA",
                MemberCandidacy.CandidateType.REQUEST);
        candidacy.setId(12);
        when(wikiUserManager.getCandidacy("subwiki", candidacy.getId())).thenReturn(candidacy);

        // The current user does not have ADMIN right
        when(authorizationManager.hasAccess(eq(Right.ADMIN), eq(userDocRef),
                eq(new WikiReference("subwiki")))).thenReturn(false);

        MemberCandidacy result = mocker.getComponentUnderTest().getCandidacy("subwiki", 12);
        assertNull(result);

        Exception exception = mocker.getComponentUnderTest().getLastError();
        assertTrue(exception instanceof WikiUserManagerScriptServiceException);
        assertEquals("You are not allowed to see this candidacy.", exception.getMessage());
    }

    @Test
    public void getCandidacyWhenError() throws Exception
    {
        Exception exception = new WikiUserManagerException("error in getCandidacy");
        when(wikiUserManager.getCandidacy("subwiki", 42)).thenThrow(exception);

        MemberCandidacy result = mocker.getComponentUnderTest().getCandidacy("subwiki", 42);
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void getAllInvitations() throws Exception
    {
        ArrayList<MemberCandidacy> candidacies = new ArrayList<MemberCandidacy>();
        // the first candidacy concerns the current user
        candidacies.add(new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION));
        // not the second
        candidacies.add(new MemberCandidacy("subwiki", "mainWiki:XWiki.UserB",
                MemberCandidacy.CandidateType.INVITATION));

        // We do not have admin rights
        when(authorizationManager.hasAccess(eq(Right.ADMIN), eq(userDocRef),
                eq(new WikiReference("subwiki")))).thenReturn(false);

        when(wikiUserManager.getAllInvitations("subwiki")).thenReturn(candidacies);

        // Test
        Collection<MemberCandidacy> result = mocker.getComponentUnderTest().getAllInvitations("subwiki");

        // the result must have been filtered
        assertEquals(1, result.size());
        assertTrue(result.contains(candidacies.get(0)));
        assertFalse(result.contains(candidacies.get(1)));
    }

    @Test
    public void getAllRequests() throws Exception
    {
        ArrayList<MemberCandidacy> candidacies = new ArrayList<MemberCandidacy>();
        // the first candidacy concerns the current user
        candidacies.add(new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.REQUEST));
        // not the second
        candidacies.add(new MemberCandidacy("subwiki", "mainWiki:XWiki.UserB",
                MemberCandidacy.CandidateType.REQUEST));

        // We do not have admin rights
        when(authorizationManager.hasAccess(eq(Right.ADMIN), eq(userDocRef),
                eq(new WikiReference("subwiki")))).thenReturn(false);

        when(wikiUserManager.getAllRequests("subwiki")).thenReturn(candidacies);

        // Test
        Collection<MemberCandidacy> result = mocker.getComponentUnderTest().getAllRequests("subwiki");

        // the result must have been filtered
        assertEquals(1, result.size());
        assertTrue(result.contains(candidacies.get(0)));
        assertFalse(result.contains(candidacies.get(1)));
    }

    @Test
    public void join() throws Exception
    {
        String userId = "mainWiki:XWiki.User";
        String wikiId = "wikiId";
        boolean result = this.mocker.getComponentUnderTest().join(userId, wikiId);
        assertTrue(result);

        verify(wikiUserManager).join(userId, wikiId);
    }

    @Test
    public void joinWhenUserIsNotCurrentUser() throws Exception
    {
        String userId = "mainWiki:XWiki.User2";
        String wikiId = "wikiId";
        boolean result = this.mocker.getComponentUnderTest().join(userId, wikiId);
        assertFalse(result);
        assertEquals("User [mainWiki:XWiki.User] cannot call $services.wiki.user.join() with an other userId.",
                this.mocker.getComponentUnderTest().getLastError().getMessage());

        verify(wikiUserManager, never()).join(userId, wikiId);
    }

    @Test
    public void joinWhenError() throws Exception
    {
        String userId = "mainWiki:XWiki.User";
        String wikiId = "wikiId";

        WikiUserManagerException exception = new WikiUserManagerException("error in wikiUserManager#join()");
        doThrow(exception).when(wikiUserManager).join(userId, wikiId);

        boolean result = this.mocker.getComponentUnderTest().join(userId, wikiId);
        assertFalse(result);

        assertEquals(exception, this.mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void leave() throws Exception
    {
        String userId = "mainWiki:XWiki.User";
        String wikiId = "wikiId";
        boolean result = this.mocker.getComponentUnderTest().leave(userId, wikiId);
        assertTrue(result);

        verify(wikiUserManager).leave(userId, wikiId);
    }

    @Test
    public void leaveWhenUserIsNotCurrentUser() throws Exception
    {
        String userId = "mainWiki:XWiki.User2";
        String wikiId = "wikiId";
        boolean result = this.mocker.getComponentUnderTest().leave(userId, wikiId);
        assertFalse(result);
        assertEquals("User [mainWiki:XWiki.User] cannot call $services.wiki.user.leave() with an other userId.",
                this.mocker.getComponentUnderTest().getLastError().getMessage());

        verify(wikiUserManager, never()).leave(userId, wikiId);
    }

    @Test
    public void leaveWhenError() throws Exception
    {
        String userId = "mainWiki:XWiki.User";
        String wikiId = "wikiId";

        WikiUserManagerException exception = new WikiUserManagerException("error in wikiUserManager#leave()");
        doThrow(exception).when(wikiUserManager).leave(userId, wikiId);

        boolean result = this.mocker.getComponentUnderTest().leave(userId, wikiId);
        assertFalse(result);

        assertEquals(exception, this.mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void hasPendingInvitation() throws Exception
    {
        String wikiId = "subwiki";

        // Mocks
        when(wikiUserManager.hasPendingInvitation(userDocRef, wikiId)).thenReturn(true);

        // Test
        Boolean result = mocker.getComponentUnderTest().hasPendingInvitation(userDocRef, wikiId);
        assertTrue(result);

        // Second run
        when(wikiUserManager.hasPendingInvitation(userDocRef, wikiId)).thenReturn(false);
        assertFalse(mocker.getComponentUnderTest().hasPendingInvitation(userDocRef, wikiId));
    }

    @Test
    public void hasPendingInvitationWhenError() throws Exception
    {
        String wikiId = "subwiki";

        // Current user is not admin
        AccessDeniedException exception = new AccessDeniedException(userDocRef, new WikiReference(wikiId));
        doThrow(exception).when(authorizationManager).checkAccess(eq(Right.ADMIN), eq(userDocRef),
                eq(new WikiReference(wikiId)));

        DocumentReference userToTest = new DocumentReference("mainWiki", "XWiki", "UserABC");

        // Test
        Boolean result = mocker.getComponentUnderTest().hasPendingInvitation(userToTest, wikiId);
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());

    }

    @Test
    public void hasPendingInvitationWhenNoPR() throws Exception
    {
        String wikiId = "subwiki";
        Exception exception = currentScriptHasNotProgrammingRight();
        DocumentReference userToTest = new DocumentReference("mainWiki", "XWiki", "UserABC");

        // Test
        Boolean result = mocker.getComponentUnderTest().hasPendingInvitation(userToTest, wikiId);
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void hasPendingRequest() throws Exception
    {
        String wikiId = "subwiki";

        // Mocks
        when(wikiUserManager.hasPendingRequest(userDocRef, wikiId)).thenReturn(true);

        // Test
        Boolean result = mocker.getComponentUnderTest().hasPendingRequest(userDocRef, wikiId);
        assertTrue(result);

        // Second run
        when(wikiUserManager.hasPendingRequest(userDocRef, wikiId)).thenReturn(false);
        assertFalse(mocker.getComponentUnderTest().hasPendingRequest(userDocRef, wikiId));
    }

    @Test
    public void hasPendingRequestWhenError() throws Exception
    {
        String wikiId = "subwiki";

        // Current user is not admin
        AccessDeniedException exception = new AccessDeniedException(userDocRef, new WikiReference(wikiId));
        doThrow(exception).when(authorizationManager).checkAccess(eq(Right.ADMIN), eq(userDocRef),
                eq(new WikiReference(wikiId)));

        DocumentReference userToTest = new DocumentReference("mainWiki", "XWiki", "UserABC");

        // Test
        Boolean result = mocker.getComponentUnderTest().hasPendingRequest(userToTest, wikiId);
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());

    }

    @Test
    public void hasPendingRequestWhenNoPR() throws Exception
    {
        String wikiId = "subwiki";
        Exception exception = currentScriptHasNotProgrammingRight();
        DocumentReference userToTest = new DocumentReference("mainWiki", "XWiki", "UserABC");

        // Test
        Boolean result = mocker.getComponentUnderTest().hasPendingRequest(userToTest, wikiId);
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }
}
