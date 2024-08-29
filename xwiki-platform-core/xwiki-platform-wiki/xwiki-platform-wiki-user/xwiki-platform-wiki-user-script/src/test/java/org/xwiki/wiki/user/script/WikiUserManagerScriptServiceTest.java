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
import org.xwiki.model.reference.DocumentReferenceResolver;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    
    private DocumentReferenceResolver<String> documentReferenceResolver;

    private Execution execution;

    private ExecutionContext executionContext;

    private XWikiContext xcontext;

    private XWikiDocument currentDoc;

    private DocumentReference userDocRef;

    @Before
    public void setUp() throws Exception
    {
        // Components mocks
        wikiUserManager           = mocker.getInstance(WikiUserManager.class);
        wikiDescriptorManager     = mocker.getInstance(WikiDescriptorManager.class);
        authorizationManager      = mocker.getInstance(AuthorizationManager.class);
        xcontextProvider          = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        documentReferenceResolver = mocker.getInstance(new DefaultParameterizedType(null,
                DocumentReferenceResolver.class, String.class));
        execution                 = mocker.getInstance(Execution.class);

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
        
        DocumentReference userReference = new DocumentReference("mainWiki", "XWiki", "User");
        when(documentReferenceResolver.resolve("mainWiki:XWiki.User")).thenReturn(userReference);
        DocumentReference otherUser = new DocumentReference("mainWiki", "XWiki", "OtherUser");
        when(documentReferenceResolver.resolve("mainWiki:XWiki.OtherUser")).thenReturn(otherUser);
    }

    /**
     * Mocks the components to simulate that a non admin user have saved the current script. 
     *
     * @return the exception expected when the current script has the not the admin right
     */
    private Exception currentScriptHasNotAdminRight() throws AccessDeniedException
    {
        DocumentReference authorDocRef = new DocumentReference("mainWiki", "XWiki", "NonAdmin");
        when(currentDoc.getAuthorReference()).thenReturn(authorDocRef);
        
        DocumentReference currentDocRef = new DocumentReference("subwiki", "Space", "PageToTest");
        when(currentDoc.getDocumentReference()).thenReturn(currentDocRef);

        Exception exception = new AccessDeniedException(Right.ADMIN, authorDocRef, currentDocRef);
        doThrow(exception).when(authorizationManager).checkAccess(Right.ADMIN, authorDocRef, currentDocRef);

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
        // Mocks
        Exception expectedException = new WikiUserManagerException("Error in getUserScope");
        when(wikiUserManager.getUserScope("test")).thenThrow(expectedException);
        
        // Test
        UserScope result = mocker.getComponentUnderTest().getUserScope("test");
        
        // Asserts
        assertNull(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void setUserScope() throws Exception
    {
        // Test
        boolean result = mocker.getComponentUnderTest().setUserScope("subwiki", "LOCAL_ONLY");
        
        // Asserts
        assertEquals(true, result);
        verify(wikiUserManager).setUserScope(eq("subwiki"), eq(UserScope.LOCAL_ONLY));
    }

    @Test
    public void setUserScopeWhenScriptHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();

        // Test
        boolean result = mocker.getComponentUnderTest().setUserScope("subwiki", "LOCAL_ONLY");
        
        // Asserts
        assertFalse(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void setUserScopeWhenUserHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        boolean result = mocker.getComponentUnderTest().setUserScope("subwiki", "LOCAL_ONLY");
        
        // Asserts
        assertFalse(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void setUserScopeWhenWrongValue() throws Exception
    {
        // Test
        boolean result = mocker.getComponentUnderTest().setUserScope("subwiki", "wrong value");
        
        // Asserts
        assertFalse(result);
        assertTrue(mocker.getComponentUnderTest().getLastError() instanceof IllegalArgumentException);
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void setUserScopeError() throws Exception
    {
        // Mocks
        WikiUserManagerException expectedException = new WikiUserManagerException("error in setUserScope");
        doThrow(expectedException).when(wikiUserManager).setUserScope(any(), any(UserScope.class));

        // Test
        boolean result = mocker.getComponentUnderTest().setUserScope("subwiki", "LOCAL_ONLY");
        
        // Asserts
        assertFalse(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void getMembershipType() throws Exception
    {
        // Mocks
        when(wikiUserManager.getMembershipType("subwiki")).thenReturn(MembershipType.INVITE);
        
        // Test
        MembershipType result = mocker.getComponentUnderTest().getMembershipType();
        
        // Asserts
        assertEquals(MembershipType.INVITE, result);
    }

    @Test
    public void getMembershipTypeWithError() throws Exception
    {
        // Mocks
        Exception expectedException = new WikiUserManagerException("Error in getMembershipType");
        when(wikiUserManager.getMembershipType("test")).thenThrow(expectedException);
        
        // Test
        MembershipType result = mocker.getComponentUnderTest().getMembershipType("test");
        
        // Asserts
        assertNull(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void setMembershipType() throws Exception
    {
        // Test
        boolean result = mocker.getComponentUnderTest().setUserScope("subwiki", "LOCAL_ONLY");
        
        // Asserts
        assertTrue(result);
    }

    @Test
    public void setMembershipTypeWhenScriptHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();

        // Test
        boolean result = mocker.getComponentUnderTest().setMembershipType("subwiki", "INVITE");
        
        // Asserts
        assertFalse(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void setMembershipTypeWhenUserHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedExtension = currentUserHasNotAdminRight();

        // Test
        boolean result = mocker.getComponentUnderTest().setMembershipType("subwiki", "INVITE");
        
        // Asserts
        assertFalse(result);
        assertEquals(expectedExtension, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);    
    }

    @Test
    public void setMembershipTypeWrongValue() throws Exception
    {
        // Test
        boolean result = mocker.getComponentUnderTest().setMembershipType("subwiki", "wrong value");
        
        // Asserts
        assertEquals(false, result);
        assertTrue(mocker.getComponentUnderTest().getLastError() instanceof IllegalArgumentException);
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void setMembershipTypeError() throws Exception
    {
        // Mocks
        WikiUserManagerException expectedException = new WikiUserManagerException("error in setMembershipType");
        doThrow(expectedException).when(wikiUserManager).setMembershipType(any(), any(MembershipType.class));

        // Test
        boolean result = mocker.getComponentUnderTest().setMembershipType("subwiki", "INVITE");
        
        // Asserts
        assertEquals(false, result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void getMembers() throws Exception
    {
        // Mocks
        Collection<String> members = new ArrayList<String>();
        when(wikiUserManager.getMembers("subwiki")).thenReturn(members);
        
        // Test
        Collection<String> result = mocker.getComponentUnderTest().getMembers("subwiki");
        
        // Asserts
        assertEquals(members, result);
    }

    @Test
    public void getMembersError() throws Exception
    {
        // Mocks
        Exception expectedException = new WikiUserManagerException("error in getMembers");
        when(wikiUserManager.getMembers("subwiki")).thenThrow(expectedException);
        
        // Test
        Collection<String> result = mocker.getComponentUnderTest().getMembers("subwiki");
        
        // Asserts
        assertNull(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void isMember() throws Exception
    {
        // Mocks
        when(wikiUserManager.isMember("mainWiki:XWiki.User", "subwiki")).thenReturn(true);
        when(wikiUserManager.isMember("mainWiki:XWiki.User", "subwiki2")).thenReturn(false);
        
        // Test
        boolean result1 = mocker.getComponentUnderTest().isMember("mainWiki:XWiki.User", "subwiki");
        boolean result2 = mocker.getComponentUnderTest().isMember("mainWiki:XWiki.User", "subwiki2");
        
        // Asserts
        assertTrue(result1);
        assertFalse(result2);
    }

    @Test
    public void isMemberError() throws Exception
    {
        // Mocks
        Exception expectedException = new WikiUserManagerException("error in isMember");
        when(wikiUserManager.isMember("mainWiki:XWiki.User", "subwiki")).thenThrow(expectedException);
        
        // Test
        Boolean result = mocker.getComponentUnderTest().isMember("mainWiki:XWiki.User", "subwiki");
        
        // Asserts
        assertNull(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void addMember() throws Exception
    {
        // Test
        boolean result = mocker.getComponentUnderTest().addMember("xwiki:XWiki.UserA", "subwiki");
        
        // Asserts
        assertTrue(result);
        verify(wikiUserManager).addMember("xwiki:XWiki.UserA", "subwiki");
    }

    @Test
    public void addMemberWhenScriptHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();

        // Test
        boolean result = mocker.getComponentUnderTest().addMember("xwiki:XWiki.UserA", "subwiki");
        
        // Asserts
        assertEquals(false, result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void addMemberWhenUserHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();
        
        // Test
        boolean result = mocker.getComponentUnderTest().addMember("xwiki:XWiki.UserA", "subwiki");
        
        // Asserts
        assertFalse(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void addMembers() throws Exception
    {
        // Test
        Collection<String> userIds = new ArrayList<String>();
        boolean result = mocker.getComponentUnderTest().addMembers(userIds, "subwiki");
        
        // Asserts
        assertTrue(result);
        verify(wikiUserManager).addMembers(userIds, "subwiki");
    }

    @Test
    public void addMembersWhenScriptHasNoRight() throws Exception
    {
        // Mock
        Exception expectedExtension = currentScriptHasNotAdminRight();

        // Test
        Collection<String> userIds = new ArrayList<String>();
        boolean result = mocker.getComponentUnderTest().addMembers(userIds, "subwiki");
        
        // Asserts
        assertFalse(result);
        assertEquals(expectedExtension, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void addMembersWhenUserHasNoRight() throws Exception
    {
        // Mock
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        Collection<String> userIds = new ArrayList<String>();
        boolean result = mocker.getComponentUnderTest().addMembers(userIds, "subwiki");
        
        // Asserts
        assertFalse(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void removeMember() throws Exception
    {
        // Test
        boolean result = mocker.getComponentUnderTest().removeMember("xwiki:XWiki.UserA", "subwiki");
        
        // Asserts
        assertTrue(result);
        verify(wikiUserManager).removeMember("xwiki:XWiki.UserA", "subwiki");
    }

    @Test
    public void removeMemberWhenScriptHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();

        // Test
        boolean result = mocker.getComponentUnderTest().removeMember("xwiki:XWiki.UserA", "subwiki");
        
        // Asserts
        assertFalse(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void removeMemberWhenUserHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();
        
        // Test
        boolean result = mocker.getComponentUnderTest().removeMember("xwiki:XWiki.UserA", "subwiki");
        
        // Asserts
        assertEquals(false, result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void getCandidacyAsAdmin() throws Exception
    {
        // Mocks
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.REQUEST);
        candidacy.setId(12);
        candidacy.setAdminPrivateComment("private message");
        when(wikiUserManager.getCandidacy("subwiki", candidacy.getId())).thenReturn(candidacy);
        when(authorizationManager.hasAccess(eq(Right.ADMIN), eq(userDocRef),
                eq(new WikiReference("subwiki")))).thenReturn(true);

        // Test
        MemberCandidacy result = mocker.getComponentUnderTest().getCandidacy("subwiki", 12);
        
        // Asserts
        assertEquals(candidacy, result);
        assertEquals("private message", result.getAdminPrivateComment());
    }

    @Test
    public void getCandidacyAsUserConcerned() throws Exception
    {
        // Mocks
        
        // Here, the candidate is the current user
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.REQUEST);
        candidacy.setId(12);
        candidacy.setAdminPrivateComment("some private message that I should not be able to see");

        when(wikiUserManager.getCandidacy("subwiki", candidacy.getId())).thenReturn(candidacy);

        // Test
        MemberCandidacy result = mocker.getComponentUnderTest().getCandidacy("subwiki", 12);
        
        // Asserts
        assertEquals(candidacy, result);
        // Verify that the private message has been removed from the candidacy
        assertNull(candidacy.getAdminPrivateComment());
    }

    @Test
    public void getCandidacyWhenNoRight() throws Exception
    {
        // Mocks
        
        // The current user is not the candidate
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.REQUEST);
        candidacy.setId(12);
        when(wikiUserManager.getCandidacy("subwiki", candidacy.getId())).thenReturn(candidacy);

        // The current user does not have ADMIN right
        when(authorizationManager.hasAccess(eq(Right.ADMIN), eq(userDocRef),
                eq(new WikiReference("subwiki")))).thenReturn(false);

        // Test
        MemberCandidacy result = mocker.getComponentUnderTest().getCandidacy("subwiki", 12);
        
        // Asserts
        assertNull(result);
        Exception exception = mocker.getComponentUnderTest().getLastError();
        assertTrue(exception instanceof WikiUserManagerScriptServiceException);
        assertEquals("You are not allowed to see this candidacy.", exception.getMessage());
    }

    @Test
    public void getCandidacyWhenGuest() throws Exception
    {
        // Mocks

        // The current user is Guest
        when(xcontext.getUserReference()).thenReturn(null);

        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
            MemberCandidacy.CandidateType.REQUEST);
        candidacy.setId(12);
        when(wikiUserManager.getCandidacy("subwiki", candidacy.getId())).thenReturn(candidacy);


        // The current user does not have ADMIN right
        when(authorizationManager.hasAccess(eq(Right.ADMIN), eq(userDocRef),
            eq(new WikiReference("subwiki")))).thenReturn(false);

        // Test
        MemberCandidacy result = mocker.getComponentUnderTest().getCandidacy("subwiki", 12);

        // Asserts
        assertNull(result);
        Exception exception = mocker.getComponentUnderTest().getLastError();
        assertTrue(exception instanceof WikiUserManagerScriptServiceException);
        assertEquals("You are not allowed to see this candidacy.", exception.getMessage());
    }

    @Test
    public void getCandidacyWhenError() throws Exception
    {
        // Mocks
        Exception exception = new WikiUserManagerException("error in getCandidacy");
        when(wikiUserManager.getCandidacy("subwiki", 42)).thenThrow(exception);

        // Test
        MemberCandidacy result = mocker.getComponentUnderTest().getCandidacy("subwiki", 42);
        
        // Asserts
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
        candidacies.add(new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.INVITATION));
        candidacies.get(0).setAdminPrivateComment("private message");

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
        // The private message from the candidacy must be removed
        assertNull(((MemberCandidacy)result.toArray()[0]).getAdminPrivateComment());
    }

    @Test
    public void getAllInvitationsError() throws Exception
    {
        // Mocks
        Exception expectedException = new WikiUserManagerException("error in getAllInvitations()");
        when(wikiUserManager.getAllInvitations("subwiki")).thenThrow(expectedException);

        // Test
        Collection<MemberCandidacy> result = mocker.getComponentUnderTest().getAllInvitations("subwiki");
        
        // Asserts
        assertNull(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void getAllRequests() throws Exception
    {
        ArrayList<MemberCandidacy> candidacies = new ArrayList<MemberCandidacy>();
        // the first candidacy concerns the current user
        candidacies.add(new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.REQUEST));
        // not the second
        candidacies.add(new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.REQUEST));
        candidacies.get(0).setAdminPrivateComment("private message");

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
        // The private message from the candidacy must be removed
        assertNull(((MemberCandidacy)result.toArray()[0]).getAdminPrivateComment());
    }

    @Test
    public void getAllRequestError() throws Exception
    {
        // Mocks
        Exception expectedException = new WikiUserManagerException("error in getAllRequests()");
        when(wikiUserManager.getAllRequests("subwiki")).thenThrow(expectedException);

        // Test
        Collection<MemberCandidacy> result = mocker.getComponentUnderTest().getAllRequests("subwiki");

        // Asserts
        assertNull(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void join() throws Exception
    {
        // Test
        String userId = "mainWiki:XWiki.User";
        String wikiId = "wikiId";
        boolean result = this.mocker.getComponentUnderTest().join(userId, wikiId);
        
        // Asserts
        assertTrue(result);
        verify(wikiUserManager).join(userId, wikiId);
    }

    @Test
    public void joinWhenUserIsNotCurrentUser() throws Exception
    {
        // Test
        String userId = "mainWiki:XWiki.OtherUser";
        String wikiId = "wikiId";
        boolean result = this.mocker.getComponentUnderTest().join(userId, wikiId);
        
        // Asserts
        assertFalse(result);
        assertEquals("User [mainWiki:XWiki.User] cannot call $services.wiki.user.join() with an other userId.",
                this.mocker.getComponentUnderTest().getLastError().getMessage());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void joinWhenError() throws Exception
    {
        String userId = "mainWiki:XWiki.User";
        String wikiId = "wikiId";

        // Mocks
        WikiUserManagerException expectedException = new WikiUserManagerException("error in wikiUserManager#join()");
        doThrow(expectedException).when(wikiUserManager).join(userId, wikiId);

        // Test
        boolean result = this.mocker.getComponentUnderTest().join(userId, wikiId);
        
        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void leave() throws Exception
    {
        String userId = "mainWiki:XWiki.User";
        String wikiId = "wikiId";
        
        // Test
        boolean result = this.mocker.getComponentUnderTest().leave(userId, wikiId);
        
        // Asserts
        assertTrue(result);
        verify(wikiUserManager).leave(userId, wikiId);
    }

    @Test
    public void leaveWhenUserIsNotCurrentUser() throws Exception
    {
        String userId = "mainWiki:XWiki.OtherUser";
        String wikiId = "wikiId";
        
        // Test
        boolean result = this.mocker.getComponentUnderTest().leave(userId, wikiId);
        
        // Asserts
        assertFalse(result);
        assertEquals("User [mainWiki:XWiki.User] cannot call $services.wiki.user.leave() with an other userId.",
                this.mocker.getComponentUnderTest().getLastError().getMessage());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void leaveWhenError() throws Exception
    {
        String userId = "mainWiki:XWiki.User";
        String wikiId = "wikiId";

        // Mocks
        WikiUserManagerException expectedException = new WikiUserManagerException("error in wikiUserManager#leave()");
        doThrow(expectedException).when(wikiUserManager).leave(userId, wikiId);

        // Test
        boolean result = this.mocker.getComponentUnderTest().leave(userId, wikiId);
        
        // Asserts
        assertFalse(result);
        assertEquals(expectedException, this.mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void hasPendingInvitation() throws Exception
    {
        String wikiId = "subwiki";

        // First test
        when(wikiUserManager.hasPendingInvitation(userDocRef, wikiId)).thenReturn(true);
        assertTrue(mocker.getComponentUnderTest().hasPendingInvitation(userDocRef, wikiId));

        // Second test
        when(wikiUserManager.hasPendingInvitation(userDocRef, wikiId)).thenReturn(false);
        assertFalse(mocker.getComponentUnderTest().hasPendingInvitation(userDocRef, wikiId));
    }

    @Test
    public void hasPendingInvitationWhenError() throws Exception
    {
        String wikiId = "subwiki";

        // Mocks
        DocumentReference userToTest = new DocumentReference("mainWiki", "XWiki", "User");
        Exception expectedException = new WikiUserManagerException("exception");
        doThrow(expectedException).when(wikiUserManager).hasPendingInvitation(userToTest, wikiId);

        // Test
        Boolean result = mocker.getComponentUnderTest().hasPendingInvitation(userToTest, wikiId);
        
        // Asserts
        assertNull(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void hasPendingInvitationWhenPageHasNoRight() throws Exception
    {
        String wikiId = "subwiki";
        DocumentReference userToTest = new DocumentReference("mainWiki", "XWiki", "User");
        
        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();

        // Test
        Boolean result = mocker.getComponentUnderTest().hasPendingInvitation(userToTest, wikiId);
        
        // Asserts
        assertNull(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void hasPendingRequest() throws Exception
    {
        String wikiId = "subwiki";
        
        // First test
        when(wikiUserManager.hasPendingRequest(userDocRef, wikiId)).thenReturn(true);
        assertTrue(mocker.getComponentUnderTest().hasPendingRequest(userDocRef, wikiId));

        // Second test
        when(wikiUserManager.hasPendingRequest(userDocRef, wikiId)).thenReturn(false);
        assertFalse(mocker.getComponentUnderTest().hasPendingRequest(userDocRef, wikiId));
    }

    @Test
    public void hasPendingRequestWhenError() throws Exception
    {
        String wikiId = "subwiki";
        DocumentReference userToTest = new DocumentReference("mainWiki", "XWiki", "User");
        
        // Mocks
        Exception expectedException = new WikiUserManagerException("exception");
        doThrow(expectedException).when(wikiUserManager).hasPendingRequest(userToTest, wikiId);

        // Test
        Boolean result = mocker.getComponentUnderTest().hasPendingRequest(userToTest, wikiId);
        
        // Asserts
        assertNull(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());

    }

    @Test
    public void hasPendingRequestWhenScriptHasNoRight() throws Exception
    {
        String wikiId = "subwiki";
        DocumentReference userToTest = new DocumentReference("mainWiki", "XWiki", "User");
        
        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();

        // Test
        Boolean result = mocker.getComponentUnderTest().hasPendingRequest(userToTest, wikiId);
        
        // Asserts
        assertNull(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }
    
    @Test
    public void acceptRequest() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);
        
        // Test
        Boolean result = mocker.getComponentUnderTest().acceptRequest(candidacy, "message", "comment");
        
        // Asserts
        assertTrue(result);
        assertNull(mocker.getComponentUnderTest().getLastError());
        verify(wikiUserManager).acceptRequest(candidacy, "message", "comment");
    }

    @Test
    public void acceptRequestWhenUserHasNoAdminRight() throws Exception
    {        
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.INVITATION);
        
        // Mocks
        Exception expecyedException = currentUserHasNotAdminRight();

        // Test
        Boolean result = mocker.getComponentUnderTest().acceptRequest(candidacy, "message", "comment");

        // Asserts
        assertFalse(result);
        assertEquals(expecyedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void acceptRequestWhenNoAdminRightButConcerned() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);
        
        // Mocks
        currentUserHasNotAdminRight();

        // Test
        Boolean result = mocker.getComponentUnderTest().acceptRequest(candidacy, "message", "comment");

        // Asserts
        assertTrue(result);
        assertNull(mocker.getComponentUnderTest().getLastError());
        verify(wikiUserManager).acceptRequest(candidacy, "message", "comment");
    }
    
    @Test
    public void askToJoin() throws Exception
    {
        // Mocks
        MemberCandidacy candidacy = new MemberCandidacy();
        when(wikiUserManager.askToJoin("mainWiki:XWiki.User", "subwiki", "please!")).thenReturn(candidacy);
        
        // Test
        MemberCandidacy result = mocker.getComponentUnderTest().askToJoin("mainWiki:XWiki.User", "subwiki", "please!");
        
        // Asserts
        assertEquals(candidacy, result);
        assertNull(mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void askToJoinWhenScriptHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentScriptHasNotAdminRight();
        
        // Test
        MemberCandidacy result = mocker.getComponentUnderTest().askToJoin("mainWiki:XWiki.User", "subwiki", "please!");
        
        // Asserts
        assertNull(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void askToJoinWhenUsertHasNoRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        MemberCandidacy result = mocker.getComponentUnderTest().askToJoin("mainWiki:XWiki.OtherUser",
                "subwiki", "please!");

        // Asserts
        assertNull(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void askToJoinWhenUserHasNoRightButConcerned() throws Exception
    {
        // Mocks
        currentUserHasNotAdminRight();
        MemberCandidacy candidacy = new MemberCandidacy();
        when(wikiUserManager.askToJoin("mainWiki:XWiki.User", "subwiki", "please!")).thenReturn(candidacy);

        // Test
        MemberCandidacy result = mocker.getComponentUnderTest().askToJoin("mainWiki:XWiki.User",
                "subwiki", "please!");

        // Asserts
        assertEquals(candidacy, result);
        assertNull(mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void refuseRequest() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Test
        Boolean result = mocker.getComponentUnderTest().refuseRequest(candidacy, "message", "comment");

        // Asserts
        assertTrue(result);
        assertNull(mocker.getComponentUnderTest().getLastError());
        verify(wikiUserManager).refuseRequest(candidacy, "message", "comment");
    }

    @Test
    public void refuseRequestWhenUserHasNoAdminRight() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.INVITATION);
        
        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        Boolean result = mocker.getComponentUnderTest().refuseRequest(candidacy, "message", "comment");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void refuseRequestWhenUserHasNoAdminRightButConcerned() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);
        
        // Mocks
        currentUserHasNotAdminRight();

        // Test
        Boolean result = mocker.getComponentUnderTest().refuseRequest(candidacy, "message", "comment");

        // Asserts
        assertTrue(result);
        assertNull(mocker.getComponentUnderTest().getLastError());
        verify(wikiUserManager).refuseRequest(candidacy, "message", "comment");
    }

    @Test
    public void cancelCandidacy() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Test
        Boolean result = mocker.getComponentUnderTest().cancelCandidacy(candidacy);

        // Asserts
        assertTrue(result);
        assertNull(mocker.getComponentUnderTest().getLastError());
        verify(wikiUserManager).cancelCandidacy(candidacy);

    }

    @Test
    public void cancelCandidacyWhenUserHasNoAdminRight() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.INVITATION);
        
        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        Boolean result = mocker.getComponentUnderTest().cancelCandidacy(candidacy);

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void cancelCandidacyWhenUserHasNoAdminRightButConcerned() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);
        
        // Mocks
        currentUserHasNotAdminRight();

        // Test
        Boolean result = mocker.getComponentUnderTest().cancelCandidacy(candidacy);

        // Asserts
        assertTrue(result);
        assertNull(mocker.getComponentUnderTest().getLastError());
        verify(wikiUserManager).cancelCandidacy(candidacy);
    }

    @Test
    public void invite() throws Exception
    {
        // Mocks
        when(wikiUserManager.invite(any(), any(), any())).thenReturn(new MemberCandidacy());
        
        // Test
        MemberCandidacy result = mocker.getComponentUnderTest().invite("someUser", "subwiki", "someMessage");

        // Asserts
        assertNotNull(result);
    }

    @Test
    public void inviteWhenUserHasNoAdminRight() throws Exception
    {
        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        MemberCandidacy result = mocker.getComponentUnderTest().invite("someUser", "subwiki", "someMessage");

        // Asserts
        assertNull(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void acceptInvitation() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Test
        Boolean result = mocker.getComponentUnderTest().acceptInvitation(candidacy, "thanks");

        // Asserts
        assertTrue(result);
        assertNull(mocker.getComponentUnderTest().getLastError());
        verify(wikiUserManager).acceptInvitation(candidacy, "thanks");
    }

    @Test
    public void acceptInvitationWhenUserHasNoAdminRight() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.INVITATION);
        
        // Mocks
        Exception exception = currentUserHasNotAdminRight();

        // Test
        Boolean result = mocker.getComponentUnderTest().acceptInvitation(candidacy, "thanks");

        // Asserts
        assertFalse(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void acceptInvitationWhenUserHasNoAdminRightButConcerned() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);
        
        // Mocks
        currentUserHasNotAdminRight();

        // Test
        Boolean result = mocker.getComponentUnderTest().acceptInvitation(candidacy, "thanks");

        // Asserts
        assertTrue(result);
        assertNull(mocker.getComponentUnderTest().getLastError());
        verify(wikiUserManager).acceptInvitation(candidacy, "thanks");
    }

    @Test
    public void refuseInvitation() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);

        // Test
        Boolean result = mocker.getComponentUnderTest().refuseInvitation(candidacy, "no thanks");

        // Asserts
        assertTrue(result);
        assertNull(mocker.getComponentUnderTest().getLastError());
        verify(wikiUserManager).refuseInvitation(candidacy, "no thanks");

    }

    @Test
    public void refuseInvitationWhenUserHasNoAdminRight() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.OtherUser",
                MemberCandidacy.CandidateType.INVITATION);
        
        // Mocks
        Exception expectedException = currentUserHasNotAdminRight();

        // Test
        Boolean result = mocker.getComponentUnderTest().refuseInvitation(candidacy, "no thanks");

        // Asserts
        assertFalse(result);
        assertEquals(expectedException, mocker.getComponentUnderTest().getLastError());
        verifyNoInteractions(wikiUserManager);
    }

    @Test
    public void refuseInvitationWhenUserHasNoAdminRightButConcerned() throws Exception
    {
        MemberCandidacy candidacy = new MemberCandidacy("subwiki", "mainWiki:XWiki.User",
                MemberCandidacy.CandidateType.INVITATION);
        
        // Mocks
        currentUserHasNotAdminRight();

        // Test
        Boolean result = mocker.getComponentUnderTest().refuseInvitation(candidacy, "no thanks");

        // Asserts
        assertTrue(result);
        assertNull(mocker.getComponentUnderTest().getLastError());
        verify(wikiUserManager).refuseInvitation(candidacy, "no thanks");
    }
}
