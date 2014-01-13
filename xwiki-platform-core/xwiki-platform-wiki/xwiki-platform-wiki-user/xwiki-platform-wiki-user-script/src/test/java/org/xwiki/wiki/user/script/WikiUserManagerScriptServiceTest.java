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
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.WikiUserManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

    @Before
    public void setUp() throws Exception
    {
        // Components mocks
        wikiUserManager = mocker.getInstance(WikiUserManager.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        authorizationManager = mocker.getInstance(AuthorizationManager.class);
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
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

        DocumentReference userDocRef = new DocumentReference("mainWiki", "XWiki", "User");
        when(xcontext.getUserReference()).thenReturn(userDocRef);
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
     * @return the exception expected when the current script has the not the admin right
     */
    private Exception currentScriptHasNotAdminRight() throws AccessDeniedException
    {
        DocumentReference userDocRef = new DocumentReference("mainWiki", "XWiki", "User");
        when(xcontext.getUserReference()).thenReturn(userDocRef);

        WikiReference wiki = new WikiReference("subwiki");
        Exception exception = new AccessDeniedException(Right.PROGRAM, userDocRef, wiki);
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
        assertEquals(null, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void setUserScope() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(xcontext.getDoc()).thenReturn(doc);
        DocumentReference userDocRef = new DocumentReference("mainWiki", "XWiki", "User");
        when(xcontext.getUserReference()).thenReturn(userDocRef);

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
        Exception exception = currentScriptHasNotAdminRight();

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
        assertEquals(null, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void setMembershipType() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(xcontext.getDoc()).thenReturn(doc);
        DocumentReference userDocRef = new DocumentReference("mainWiki", "XWiki", "User");
        when(xcontext.getUserReference()).thenReturn(userDocRef);

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
        Exception exception = currentScriptHasNotAdminRight();

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
}
