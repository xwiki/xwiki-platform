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
package org.xwiki.wiki.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.internal.standard.StandardURLConfiguration;
import org.xwiki.wiki.configuration.WikiConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

public class WikiManagerScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<WikiManagerScriptService> mocker =
        new MockitoComponentMockingRule<WikiManagerScriptService>(WikiManagerScriptService.class);

    private WikiManager wikiManager;

    private WikiDescriptorManager wikiDescriptorManager;

    private Provider<XWikiContext> xcontextProvider;

    private Execution execution;

    private AuthorizationManager authorizationManager;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private ScriptServiceManager scriptServiceManager;

    private StandardURLConfiguration standardURLConfiguration;

    private WikiConfiguration wikiConfiguration;

    private XWikiContext xcontext;

    private ExecutionContext executionContext;

    private DocumentReference currentUserRef;

    private XWikiDocument currentDoc;

    private WikiDescriptorDocumentHelper wikiDescriptorDocumentHelper;

    @Before
    public void setUp() throws Exception
    {
        wikiManager = mocker.getInstance(WikiManager.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        authorizationManager = mocker.getInstance(AuthorizationManager.class);
        scriptServiceManager = mocker.getInstance(ScriptServiceManager.class);
        entityReferenceSerializer =
            mocker.getInstance(new DefaultParameterizedType(null, EntityReferenceSerializer.class, String.class));
        standardURLConfiguration = mocker.getInstance(StandardURLConfiguration.class);
        wikiConfiguration = mocker.getInstance(WikiConfiguration.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        execution = mocker.getInstance(Execution.class);
        executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);

        currentUserRef = new DocumentReference("mainWiki", "XWiki", "User");
        when(xcontext.getUserReference()).thenReturn(currentUserRef);

        currentDoc = mock(XWikiDocument.class);
        when(xcontext.getDoc()).thenReturn(currentDoc);

        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");

        when(entityReferenceSerializer.serialize(currentUserRef)).thenReturn("mainWiki:XWiki.User");

        wikiDescriptorDocumentHelper = mocker.getInstance(WikiDescriptorDocumentHelper.class);
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
    private Exception currentUserHasNotProgrammingRight() throws AccessDeniedException
    {
        WikiReference wiki = new WikiReference("mainWiki");
        Exception exception = new AccessDeniedException(Right.PROGRAM, currentUserRef, wiki);
        doThrow(exception).when(authorizationManager).checkAccess(eq(Right.PROGRAM), eq(currentUserRef), eq(wiki));

        return exception;
    }

    /**
     * @return the exception expected when the current user has the not the 'create wiki' right
     */
    private Exception currentUserHasNotCreateWikiRight() throws AccessDeniedException
    {
        WikiReference wiki = new WikiReference("mainWiki");
        Exception exception = new AccessDeniedException(Right.CREATE_WIKI, currentUserRef, wiki);
        doThrow(exception).when(authorizationManager).checkAccess(eq(Right.CREATE_WIKI), eq(currentUserRef), eq(wiki));

        return exception;
    }

    /**
     * @param wikiId the id of the wiki for which to get the descriptor
     * @return the wiki descriptor document for the wiki identified by the given wikiId
     */
    private DocumentReference getAndSetupDescriptorDocument(String wikiId)
    {
        DocumentReference descriptorDocument =
            new DocumentReference("mainWiki", "XWiki", "XWikiServer" + StringUtils.capitalize(wikiId));
        when(wikiDescriptorDocumentHelper.getDocumentReferenceFromId(wikiId)).thenReturn(descriptorDocument);

        return descriptorDocument;
    }

    @Test
    public void get() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().get("template");
        // Verify
        verify(scriptServiceManager).get("wiki.template");
    }

    @Test
    public void createWiki() throws Exception
    {
        WikiDescriptor descriptor = new WikiDescriptor("newiki", "alias");
        when(wikiManager.create("newwiki", "alias", true)).thenReturn(descriptor);

        WikiDescriptor result = mocker.getComponentUnderTest().createWiki("newwiki", "alias", "userA", true);
        assertEquals(descriptor, result);
        assertEquals("userA", result.getOwnerId());
        verify(wikiDescriptorManager).saveDescriptor(result);
    }

    @Test
    public void createWikiWithoutPR() throws Exception
    {
        Exception exception = currentScriptHasNotProgrammingRight();

        WikiDescriptor result = mocker.getComponentUnderTest().createWiki("newwiki", "alias", "userA", true);
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void createWikiWithoutCreateWikiRight() throws Exception
    {
        Exception exception = currentUserHasNotCreateWikiRight();

        WikiDescriptor result = mocker.getComponentUnderTest().createWiki("newwiki", "alias", "userA", true);
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void createWikiNoFailOnExistWithoutPR() throws Exception
    {
        Exception exception = currentUserHasNotProgrammingRight();

        WikiDescriptor result = mocker.getComponentUnderTest().createWiki("newwiki", "alias", "userA", false);
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void createWikiError() throws Exception
    {
        Exception exception = new WikiManagerException("error on create");
        when(wikiManager.create("newwiki", "alias", true)).thenThrow(exception);
        WikiDescriptor result = mocker.getComponentUnderTest().createWiki("newwiki", "alias", "userA", true);
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void getByAlias() throws Exception
    {
        WikiDescriptor descriptor = new WikiDescriptor("wikiId", "wikiAlias");
        when(wikiDescriptorManager.getByAlias("wikiAlias")).thenReturn(descriptor);

        WikiDescriptor result = mocker.getComponentUnderTest().getByAlias("wikiAlias");
        assertEquals(descriptor, result);
    }

    @Test
    public void getByAliasError() throws Exception
    {
        Exception exception = new WikiManagerException("error in getByAlias");
        when(wikiDescriptorManager.getByAlias("wikiAlias")).thenThrow(exception);

        WikiDescriptor result = mocker.getComponentUnderTest().getByAlias("wikiAlias");
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void getById() throws Exception
    {
        WikiDescriptor descriptor = new WikiDescriptor("wikiId", "wikiAlias");
        when(wikiDescriptorManager.getById("wikiId")).thenReturn(descriptor);

        WikiDescriptor result = mocker.getComponentUnderTest().getById("wikiId");
        assertEquals(descriptor, result);
    }

    @Test
    public void getByIdError() throws Exception
    {
        Exception exception = new WikiManagerException("error in getById");
        when(wikiDescriptorManager.getById("wikiId")).thenThrow(exception);

        WikiDescriptor result = mocker.getComponentUnderTest().getById("wikiId");
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void getAll() throws Exception
    {
        WikiDescriptor descriptor1 = new WikiDescriptor("wikiId1", "wikiAlias1");
        WikiDescriptor descriptor2 = new WikiDescriptor("wikiId2", "wikiAlias2");
        Collection<WikiDescriptor> descriptors = new ArrayList<WikiDescriptor>();
        descriptors.add(descriptor1);
        descriptors.add(descriptor2);
        when(wikiDescriptorManager.getAll()).thenReturn(descriptors);

        Collection<WikiDescriptor> result = mocker.getComponentUnderTest().getAll();
        assertEquals(descriptors, result);
    }

    @Test
    public void getAllIds() throws Exception
    {
        Collection<String> wikiIds = Arrays.asList("wikiId1", "wikiId2");
        when(wikiDescriptorManager.getAllIds()).thenReturn(wikiIds);

        Collection<String> result = mocker.getComponentUnderTest().getAllIds();
        assertEquals(wikiIds, result);
    }

    @Test
    public void getAllError() throws Exception
    {
        Exception exception = new WikiManagerException("error in getAll");
        when(wikiDescriptorManager.getAll()).thenThrow(exception);

        Collection<WikiDescriptor> result = mocker.getComponentUnderTest().getAll();
        assertTrue(result.isEmpty());
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void getAllIdsError() throws Exception
    {
        Exception exception = new WikiManagerException("error in getAllIds");
        when(wikiDescriptorManager.getAllIds()).thenThrow(exception);

        Collection<String> result = mocker.getComponentUnderTest().getAllIds();
        assertTrue(result.isEmpty());
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void exists() throws Exception
    {
        when(wikiDescriptorManager.exists("wikiId")).thenReturn(true);
        when(wikiDescriptorManager.exists("no")).thenReturn(false);

        assertTrue(mocker.getComponentUnderTest().exists("wikiId"));
        assertFalse(mocker.getComponentUnderTest().exists("no"));
    }

    @Test
    public void existsError() throws Exception
    {
        Exception exception = new WikiManagerException("error in exists");
        when(wikiDescriptorManager.exists("wikiId")).thenThrow(exception);

        Boolean result = mocker.getComponentUnderTest().exists("wikiId");
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void idAvailable() throws Exception
    {
        when(wikiManager.idAvailable("wikiId")).thenReturn(true);
        when(wikiManager.idAvailable("no")).thenReturn(false);

        assertTrue(mocker.getComponentUnderTest().idAvailable("wikiId"));
        assertFalse(mocker.getComponentUnderTest().idAvailable("no"));
    }

    @Test
    public void idAvailableError() throws Exception
    {
        Exception exception = new WikiManagerException("error in idAvailable");
        when(wikiManager.idAvailable("wikiId")).thenThrow(exception);

        Boolean result = mocker.getComponentUnderTest().idAvailable("wikiId");
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
        assertEquals(exception, mocker.getComponentUnderTest().getLastException());
    }

    @Test
    public void getMainWikiDescriptor() throws Exception
    {
        WikiDescriptor descriptor = new WikiDescriptor("mainWiki", "wikiAlias");
        when(wikiDescriptorManager.getMainWikiDescriptor()).thenReturn(descriptor);

        WikiDescriptor result = mocker.getComponentUnderTest().getMainWikiDescriptor();
        assertEquals(descriptor, result);
    }

    @Test
    public void getMainWikiDescriptorError() throws Exception
    {
        Exception exception = new WikiManagerException("error in getMainWikiDescriptor");
        when(wikiDescriptorManager.getMainWikiDescriptor()).thenThrow(exception);

        WikiDescriptor result = mocker.getComponentUnderTest().getMainWikiDescriptor();
        assertNull(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void getMainWikiId() throws Exception
    {
        String result = mocker.getComponentUnderTest().getMainWikiId();
        assertEquals("mainWiki", result);
    }

    @Test
    public void getCurrentWikiId() throws Exception
    {
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("currentWiki");
        String result = mocker.getComponentUnderTest().getCurrentWikiId();
        assertEquals("currentWiki", result);
    }

    @Test
    public void saveDescriptorWhenICanEditDescriptorDocument() throws Exception
    {
        WikiDescriptor oldDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        oldDescriptor.setOwnerId("SomeUser");
        when(wikiDescriptorManager.getById(oldDescriptor.getId())).thenReturn(oldDescriptor);

        DocumentReference wikiDescriptorDocRef = getAndSetupDescriptorDocument(oldDescriptor.getId());
        when(this.authorizationManager.hasAccess(Right.EDIT, currentUserRef, wikiDescriptorDocRef)).thenReturn(true);

        // Changing some value, not the owner.
        WikiDescriptor descriptor = new WikiDescriptor(oldDescriptor.getId(), "wikiAlias");
        descriptor.setOwnerId(oldDescriptor.getOwnerId());
        boolean result = mocker.getComponentUnderTest().saveDescriptor(descriptor);
        assertTrue(result);

        // The descriptor has been saved
        verify(wikiDescriptorManager).saveDescriptor(descriptor);
    }

    @Test
    public void saveDescriptorWhenIAmOwner() throws Exception
    {
        WikiDescriptor oldDescriptor = mock(WikiDescriptor.class);
        when(oldDescriptor.getId()).thenReturn("wikiId");
        when(oldDescriptor.getOwnerId()).thenReturn("mainWiki:XWiki.User");
        when(wikiDescriptorManager.getById(oldDescriptor.getId())).thenReturn(oldDescriptor);

        // Changing some value, not the owner.
        WikiDescriptor descriptor = new WikiDescriptor(oldDescriptor.getId(), "wikiAlias");
        descriptor.setOwnerId(oldDescriptor.getOwnerId());
        boolean result = mocker.getComponentUnderTest().saveDescriptor(descriptor);
        assertTrue(result);

        // The owner of the old descriptor was verified (once by us, once by the call).
        verify(oldDescriptor, times(2)).getOwnerId();

        // The descriptor has been saved
        verify(wikiDescriptorManager).saveDescriptor(descriptor);
    }

    @Test
    public void saveDescriptorWhenIAmLocalAdmin() throws Exception
    {
        WikiDescriptor oldDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        oldDescriptor.setOwnerId("SomeUser");
        when(wikiDescriptorManager.getById(oldDescriptor.getId())).thenReturn(oldDescriptor);

        // Local admin.
        when(authorizationManager.hasAccess(eq(Right.ADMIN), eq(currentUserRef), eq(new WikiReference("wikiId"))))
            .thenReturn(true);

        // Changing some value, not the owner.
        WikiDescriptor descriptor = new WikiDescriptor(oldDescriptor.getId(), "wikiAlias");
        descriptor.setOwnerId(oldDescriptor.getOwnerId());
        boolean result = mocker.getComponentUnderTest().saveDescriptor(descriptor);
        assertTrue(result);

        // The right has been checked
        verify(authorizationManager).hasAccess(eq(Right.ADMIN), eq(currentUserRef), eq(new WikiReference("wikiId")));
        // The descriptor has been saved
        verify(wikiDescriptorManager).saveDescriptor(descriptor);
    }

    @Test
    public void saveDescriptorWhenIAmNotOwnerNorLocalAdminNorGlobalAdmin() throws Exception
    {
        WikiDescriptor oldDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        oldDescriptor.setOwnerId("SomeUser");
        when(wikiDescriptorManager.getById(oldDescriptor.getId())).thenReturn(oldDescriptor);

        when(authorizationManager.hasAccess(eq(Right.ADMIN), eq(currentUserRef), eq(new WikiReference("wikiId"))))
            .thenReturn(false);

        // Changing some value, not the owner.
        WikiDescriptor descriptor = new WikiDescriptor(oldDescriptor.getId(), "wikiAlias");
        oldDescriptor.setOwnerId(oldDescriptor.getOwnerId());
        boolean result = mocker.getComponentUnderTest().saveDescriptor(descriptor);
        assertFalse(result);

        // The descriptor has not been saved
        verify(wikiDescriptorManager, never()).saveDescriptor(descriptor);

        Exception exception = new AccessDeniedException(currentUserRef, new WikiReference("wikiId"));
        assertEquals(exception.getMessage(), mocker.getComponentUnderTest().getLastError().getMessage());
        assertEquals(exception.getClass(), mocker.getComponentUnderTest().getLastError().getClass());
    }

    @Test
    public void saveDescriptorWhenIAmLocalAdminAndChangeOwner() throws Exception
    {
        WikiDescriptor oldDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        oldDescriptor.setOwnerId("SomeUser");
        when(wikiDescriptorManager.getById(oldDescriptor.getId())).thenReturn(oldDescriptor);

        // Changing the owner.
        WikiDescriptor descriptor = new WikiDescriptor(oldDescriptor.getId(), "wikiAlias");
        descriptor.setOwnerId("SomeOtherUserOrMyself");
        boolean result = mocker.getComponentUnderTest().saveDescriptor(descriptor);
        assertFalse(result);

        // The right has been checked
        verify(authorizationManager).hasAccess(eq(Right.ADMIN), eq(currentUserRef), eq(new WikiReference("wikiId")));

        // The descriptor has not been saved
        verify(wikiDescriptorManager, never()).saveDescriptor(descriptor);

        Exception expectedException = new AccessDeniedException(currentUserRef, new WikiReference("wikiId"));
        assertEquals(expectedException.getMessage(), mocker.getComponentUnderTest().getLastError().getMessage());
        assertEquals(expectedException.getClass(), mocker.getComponentUnderTest().getLastError().getClass());
    }

    @Test
    public void saveDescriptorWhenICanEditDescriptorDocumentAndChangeOwner() throws Exception
    {
        WikiDescriptor oldDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        oldDescriptor.setOwnerId("SomeUser");
        when(wikiDescriptorManager.getById(oldDescriptor.getId())).thenReturn(oldDescriptor);

        DocumentReference wikiDescriptorDocRef = getAndSetupDescriptorDocument(oldDescriptor.getId());
        when(this.authorizationManager.hasAccess(Right.EDIT, currentUserRef, wikiDescriptorDocRef)).thenReturn(true);

        // Changing the owner is possible, since I can directly edit the wiki descriptor anyway.
        WikiDescriptor descriptor = new WikiDescriptor(oldDescriptor.getId(), "wikiAlias");
        descriptor.setOwnerId("SomeOtherUserOrMyself");
        boolean result = mocker.getComponentUnderTest().saveDescriptor(descriptor);
        assertTrue(result);

        // The descriptor has been saved
        verify(wikiDescriptorManager).saveDescriptor(descriptor);
    }

    @Test
    public void saveDescriptorWhenDescriptorDidNotExist() throws Exception
    {
        WikiDescriptor descriptor = new WikiDescriptor("wikiId", "wikiAlias");
        boolean result = mocker.getComponentUnderTest().saveDescriptor(descriptor);
        assertFalse(result);

        // Verify the rights have been checked
        verify(authorizationManager).hasAccess(eq(Right.ADMIN), eq(currentUserRef), eq(new WikiReference("mainWiki")));

        // The descriptor has not been saved
        verify(wikiDescriptorManager, never()).saveDescriptor(descriptor);
    }

    @Test
    public void saveDescriptorWhenDescriptorDidNotExistAndIAmGlobalAdmin() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN, currentUserRef, new WikiReference("mainWiki")))
            .thenReturn(true);

        WikiDescriptor descriptor = new WikiDescriptor("wikiId", "wikiAlias");
        boolean result = mocker.getComponentUnderTest().saveDescriptor(descriptor);
        assertTrue(result);

        // Verify the rights have been checked
        verify(authorizationManager).hasAccess(eq(Right.ADMIN), eq(currentUserRef), eq(new WikiReference("mainWiki")));

        // The descriptor has been saved
        verify(wikiDescriptorManager).saveDescriptor(descriptor);
    }

    @Test
    public void isPathMode() throws Exception
    {
        when(standardURLConfiguration.isPathBasedMultiWiki()).thenReturn(true);
        assertTrue(mocker.getComponentUnderTest().isPathMode());

        when(standardURLConfiguration.isPathBasedMultiWiki()).thenReturn(false);
        assertFalse(mocker.getComponentUnderTest().isPathMode());
    }

    @Test
    public void getAliasSuffix() throws Exception
    {
        when(wikiConfiguration.getAliasSuffix()).thenReturn("mysuffix.org");
        assertEquals(mocker.getComponentUnderTest().getAliasSuffix(), "mysuffix.org");
    }

    @Test
    public void getCurrentDescriptor() throws Exception
    {
        mocker.getComponentUnderTest().getCurrentWikiDescriptor();

        verify(wikiDescriptorManager).getCurrentWikiDescriptor();
    }
}
