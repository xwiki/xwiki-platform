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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.internal.standard.StandardURLConfiguration;
import org.xwiki.wiki.configuration.WikiConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class WikiManagerScriptServiceTest
{
    @InjectMockComponents
    private WikiManagerScriptService wikiManagerScriptService;

    @MockComponent
    private WikiManager wikiManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private Execution execution;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private ScriptServiceManager scriptServiceManager;

    @MockComponent
    private StandardURLConfiguration standardURLConfiguration;

    @MockComponent
    private WikiConfiguration wikiConfiguration;

    @MockComponent
    private WikiDescriptorDocumentHelper wikiDescriptorDocumentHelper;

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    private XWikiContext xcontext;

    private ExecutionContext executionContext;

    private DocumentReference currentUserRef;

    private XWikiDocument currentDoc;

    @BeforeEach
    void setUp()
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.executionContext = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(this.executionContext);

        this.currentUserRef = new DocumentReference("mainWiki", "XWiki", "User");
        when(this.xcontext.getUserReference()).thenReturn(this.currentUserRef);

        this.currentDoc = mock(XWikiDocument.class);
        when(this.xcontext.getDoc()).thenReturn(this.currentDoc);

        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");

        when(this.entityReferenceSerializer.serialize(this.currentUserRef)).thenReturn("mainWiki:XWiki.User");
    }

    /**
     * @return the exception expected when the current script has the not the programing right
     */
    private Exception currentScriptHasNotProgrammingRight() throws AccessDeniedException
    {
        DocumentReference authorDocRef = new DocumentReference("mainWiki", "XWiki", "Admin");
        when(this.currentDoc.getAuthorReference()).thenReturn(authorDocRef);
        DocumentReference currentDocRef = new DocumentReference("subwiki", "Test", "test");
        when(this.currentDoc.getDocumentReference()).thenReturn(currentDocRef);

        Exception exception = new AccessDeniedException(Right.PROGRAM, authorDocRef, currentDocRef);
        doThrow(exception).when(this.authorizationManager).checkAccess(Right.PROGRAM, authorDocRef, currentDocRef);

        return exception;
    }

    /**
     * @return the exception expected when the current user doesn't have the admin right
     */
    private Exception currentUserHasNotProgrammingRight() throws AccessDeniedException
    {
        WikiReference wiki = new WikiReference("mainWiki");
        Exception exception = new AccessDeniedException(Right.PROGRAM, this.currentUserRef, wiki);
        doThrow(exception).when(this.authorizationManager).checkAccess(eq(Right.PROGRAM), eq(this.currentUserRef),
            eq(wiki));

        return exception;
    }

    /**
     * @return the exception expected when the current user has the not the 'create wiki' right
     */
    private Exception currentUserHasNotCreateWikiRight() throws AccessDeniedException
    {
        WikiReference wiki = new WikiReference("mainWiki");
        Exception exception = new AccessDeniedException(Right.CREATE_WIKI, this.currentUserRef, wiki);
        doThrow(exception).when(this.authorizationManager).checkAccess(eq(Right.CREATE_WIKI), eq(this.currentUserRef),
            eq(wiki));

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
        when(this.wikiDescriptorDocumentHelper.getDocumentReferenceFromId(wikiId)).thenReturn(descriptorDocument);

        return descriptorDocument;
    }

    @Test
    void get()
    {
        // Test
        this.wikiManagerScriptService.get("template");
        // Verify
        verify(this.scriptServiceManager).get("wiki.template");
    }

    @Test
    void createWiki() throws Exception
    {
        WikiDescriptor descriptor = new WikiDescriptor("newiki", "alias", "userA");
        when(this.wikiManager.create("newwiki", "alias", "userA", true)).thenReturn(descriptor);

        WikiDescriptor result = this.wikiManagerScriptService.createWiki("newwiki", "alias", "userA", true);
        assertEquals(descriptor, result);
    }

    @Test
    void createWikiWithoutPR() throws Exception
    {
        Exception exception = currentScriptHasNotProgrammingRight();

        WikiDescriptor result = this.wikiManagerScriptService.createWiki("newwiki", "alias", "userA", true);
        assertNull(result);
        assertEquals(exception, this.wikiManagerScriptService.getLastError());
        assertEquals("Access denied when checking [programming] access to [subwiki:Test.test] for user"
            + " [mainWiki:XWiki.Admin]", this.logCapture.getMessage(0));
    }

    @Test
    void createWikiWithoutCreateWikiRight() throws Exception
    {
        Exception exception = currentUserHasNotCreateWikiRight();

        WikiDescriptor result = this.wikiManagerScriptService.createWiki("newwiki", "alias", "userA", true);
        assertNull(result);
        assertEquals(exception, this.wikiManagerScriptService.getLastError());
        assertEquals("Access denied when checking [createwiki] access to [Wiki mainWiki] for user"
            + " [mainWiki:XWiki.User]", this.logCapture.getMessage(0));
    }

    @Test
    void createWikiNoFailOnExistWithoutPR() throws Exception
    {
        Exception exception = currentUserHasNotProgrammingRight();

        WikiDescriptor result = this.wikiManagerScriptService.createWiki("newwiki", "alias", "userA", false);
        assertNull(result);
        assertEquals(exception, this.wikiManagerScriptService.getLastError());
        assertEquals("Access denied when checking [programming] access to [Wiki mainWiki] for user"
            + " [mainWiki:XWiki.User]", this.logCapture.getMessage(0));
    }

    @Test
    void createWikiError() throws Exception
    {
        Exception exception = new WikiManagerException("error on create");
        when(this.wikiManager.create("newwiki", "alias", "userA", true)).thenThrow(exception);
        WikiDescriptor result = this.wikiManagerScriptService.createWiki("newwiki", "alias", "userA", true);
        assertNull(result);
        assertEquals(exception, this.wikiManagerScriptService.getLastError());
        assertEquals("error on create", this.logCapture.getMessage(0));
    }

    @Test
    void getByAlias() throws Exception
    {
        WikiDescriptor descriptor = new WikiDescriptor("wikiId", "wikiAlias");
        when(this.wikiDescriptorManager.getByAlias("wikiAlias")).thenReturn(descriptor);

        WikiDescriptor result = this.wikiManagerScriptService.getByAlias("wikiAlias");
        assertEquals(descriptor, result);
    }

    @Test
    void getByAliasError() throws Exception
    {
        Exception exception = new WikiManagerException("error in getByAlias");
        when(this.wikiDescriptorManager.getByAlias("wikiAlias")).thenThrow(exception);

        WikiDescriptor result = this.wikiManagerScriptService.getByAlias("wikiAlias");
        assertNull(result);
        assertEquals(exception, this.wikiManagerScriptService.getLastError());
        assertEquals("error in getByAlias", this.logCapture.getMessage(0));
    }

    @Test
    void getById() throws Exception
    {
        WikiDescriptor descriptor = new WikiDescriptor("wikiId", "wikiAlias");
        when(this.wikiDescriptorManager.getById("wikiId")).thenReturn(descriptor);

        WikiDescriptor result = this.wikiManagerScriptService.getById("wikiId");
        assertEquals(descriptor, result);
    }

    @Test
    void getByIdError() throws Exception
    {
        Exception exception = new WikiManagerException("error in getById");
        when(this.wikiDescriptorManager.getById("wikiId")).thenThrow(exception);

        WikiDescriptor result = this.wikiManagerScriptService.getById("wikiId");
        assertNull(result);
        assertEquals(exception, this.wikiManagerScriptService.getLastError());
        assertEquals("error in getById", this.logCapture.getMessage(0));
    }

    @Test
    void getAll() throws Exception
    {
        WikiDescriptor descriptor1 = new WikiDescriptor("wikiId1", "wikiAlias1");
        WikiDescriptor descriptor2 = new WikiDescriptor("wikiId2", "wikiAlias2");
        Collection<WikiDescriptor> descriptors = new ArrayList<>();
        descriptors.add(descriptor1);
        descriptors.add(descriptor2);
        when(this.wikiDescriptorManager.getAll()).thenReturn(descriptors);

        Collection<WikiDescriptor> result = this.wikiManagerScriptService.getAll();
        assertEquals(descriptors, result);
    }

    @Test
    void getAllIds() throws Exception
    {
        Collection<String> wikiIds = List.of("wikiId1", "wikiId2");
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(wikiIds);

        Collection<String> result = this.wikiManagerScriptService.getAllIds();
        assertEquals(wikiIds, result);
    }

    @Test
    void getAllError() throws Exception
    {
        Exception exception = new WikiManagerException("error in getAll");
        when(this.wikiDescriptorManager.getAll()).thenThrow(exception);

        Collection<WikiDescriptor> result = this.wikiManagerScriptService.getAll();
        assertTrue(result.isEmpty());
        assertEquals(exception, this.wikiManagerScriptService.getLastError());
        assertEquals("error in getAll", this.logCapture.getMessage(0));
    }

    @Test
    void getAllIdsError() throws Exception
    {
        Exception exception = new WikiManagerException("error in getAllIds");
        when(this.wikiDescriptorManager.getAllIds()).thenThrow(exception);

        Collection<String> result = this.wikiManagerScriptService.getAllIds();
        assertTrue(result.isEmpty());
        assertEquals(exception, this.wikiManagerScriptService.getLastError());
        assertEquals("error in getAllIds", this.logCapture.getMessage(0));
    }

    @Test
    void exists() throws Exception
    {
        when(this.wikiDescriptorManager.exists("wikiId")).thenReturn(true);
        when(this.wikiDescriptorManager.exists("no")).thenReturn(false);

        assertTrue(this.wikiManagerScriptService.exists("wikiId"));
        assertFalse(this.wikiManagerScriptService.exists("no"));
    }

    @Test
    void existsError() throws Exception
    {
        Exception exception = new WikiManagerException("error in exists");
        when(this.wikiDescriptorManager.exists("wikiId")).thenThrow(exception);

        Boolean result = this.wikiManagerScriptService.exists("wikiId");
        assertNull(result);
        assertEquals(exception, this.wikiManagerScriptService.getLastError());
        assertEquals("error in exists", this.logCapture.getMessage(0));
    }

    @Test
    void idAvailable() throws Exception
    {
        when(this.wikiManager.idAvailable("wikiId")).thenReturn(true);
        when(this.wikiManager.idAvailable("no")).thenReturn(false);

        assertTrue(this.wikiManagerScriptService.idAvailable("wikiId"));
        assertFalse(this.wikiManagerScriptService.idAvailable("no"));
    }

    @Test
    void idAvailableError() throws Exception
    {
        Exception exception = new WikiManagerException("error in idAvailable");
        when(this.wikiManager.idAvailable("wikiId")).thenThrow(exception);

        Boolean result = this.wikiManagerScriptService.idAvailable("wikiId");
        assertNull(result);
        assertEquals(exception, this.wikiManagerScriptService.getLastError());
        assertEquals(exception, this.wikiManagerScriptService.getLastException());
        assertEquals("error in idAvailable", this.logCapture.getMessage(0));
    }

    @Test
    void getMainWikiDescriptor() throws Exception
    {
        WikiDescriptor descriptor = new WikiDescriptor("mainWiki", "wikiAlias");
        when(this.wikiDescriptorManager.getMainWikiDescriptor()).thenReturn(descriptor);

        WikiDescriptor result = this.wikiManagerScriptService.getMainWikiDescriptor();
        assertEquals(descriptor, result);
    }

    @Test
    void getMainWikiDescriptorError() throws Exception
    {
        Exception exception = new WikiManagerException("error in getMainWikiDescriptor");
        when(this.wikiDescriptorManager.getMainWikiDescriptor()).thenThrow(exception);

        WikiDescriptor result = this.wikiManagerScriptService.getMainWikiDescriptor();
        assertNull(result);
        assertEquals(exception, this.wikiManagerScriptService.getLastError());
        assertEquals("error in getMainWikiDescriptor", this.logCapture.getMessage(0));
    }

    @Test
    void getMainWikiId()
    {
        String result = this.wikiManagerScriptService.getMainWikiId();
        assertEquals("mainWiki", result);
    }

    @Test
    void getCurrentWikiId() throws Exception
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("currentWiki");
        String result = this.wikiManagerScriptService.getCurrentWikiId();
        assertEquals("currentWiki", result);
    }

    @Test
    void saveDescriptorWhenICanEditDescriptorDocument() throws Exception
    {
        WikiDescriptor oldDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        oldDescriptor.setOwnerId("SomeUser");
        when(this.wikiDescriptorManager.getById(oldDescriptor.getId())).thenReturn(oldDescriptor);

        DocumentReference wikiDescriptorDocRef = getAndSetupDescriptorDocument(oldDescriptor.getId());
        when(this.authorizationManager.hasAccess(Right.EDIT, this.currentUserRef, wikiDescriptorDocRef))
            .thenReturn(true);

        // Changing some value, not the owner.
        WikiDescriptor descriptor = new WikiDescriptor(oldDescriptor.getId(), "wikiAlias");
        descriptor.setOwnerId(oldDescriptor.getOwnerId());
        boolean result = this.wikiManagerScriptService.saveDescriptor(descriptor);
        assertTrue(result);

        // The descriptor has been saved
        verify(this.wikiDescriptorManager).saveDescriptor(descriptor);
    }

    @Test
    void saveDescriptorWhenIAmOwner() throws Exception
    {
        WikiDescriptor oldDescriptor = mock(WikiDescriptor.class);
        when(oldDescriptor.getId()).thenReturn("wikiId");
        when(oldDescriptor.getOwnerId()).thenReturn("mainWiki:XWiki.User");
        when(this.wikiDescriptorManager.getById(oldDescriptor.getId())).thenReturn(oldDescriptor);

        // Changing some value, not the owner.
        WikiDescriptor descriptor = new WikiDescriptor(oldDescriptor.getId(), "wikiAlias");
        descriptor.setOwnerId(oldDescriptor.getOwnerId());
        boolean result = this.wikiManagerScriptService.saveDescriptor(descriptor);
        assertTrue(result);

        // The owner of the old descriptor was verified (once by us, once by the call).
        verify(oldDescriptor, times(2)).getOwnerId();

        // The descriptor has been saved
        verify(this.wikiDescriptorManager).saveDescriptor(descriptor);
    }

    @Test
    void saveDescriptorWhenIAmLocalAdmin() throws Exception
    {
        WikiDescriptor oldDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        oldDescriptor.setOwnerId("SomeUser");
        when(this.wikiDescriptorManager.getById(oldDescriptor.getId())).thenReturn(oldDescriptor);

        // Local admin.
        when(this.authorizationManager.hasAccess(eq(Right.ADMIN), eq(this.currentUserRef),
            eq(new WikiReference("wikiId")))).thenReturn(true);

        // Changing some value, not the owner.
        WikiDescriptor descriptor = new WikiDescriptor(oldDescriptor.getId(), "wikiAlias");
        descriptor.setOwnerId(oldDescriptor.getOwnerId());
        boolean result = this.wikiManagerScriptService.saveDescriptor(descriptor);
        assertTrue(result);

        // The right has been checked
        verify(this.authorizationManager).hasAccess(eq(Right.ADMIN), eq(this.currentUserRef),
            eq(new WikiReference("wikiId")));
        // The descriptor has been saved
        verify(this.wikiDescriptorManager).saveDescriptor(descriptor);
    }

    @Test
    void saveDescriptorWhenIAmNotOwnerNorLocalAdminNorGlobalAdmin() throws Exception
    {
        WikiDescriptor oldDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        oldDescriptor.setOwnerId("SomeUser");
        when(this.wikiDescriptorManager.getById(oldDescriptor.getId())).thenReturn(oldDescriptor);

        when(this.authorizationManager.hasAccess(eq(Right.ADMIN), eq(this.currentUserRef),
            eq(new WikiReference("wikiId")))).thenReturn(false);

        // Changing some value, not the owner.
        WikiDescriptor descriptor = new WikiDescriptor(oldDescriptor.getId(), "wikiAlias");
        oldDescriptor.setOwnerId(oldDescriptor.getOwnerId());
        boolean result = this.wikiManagerScriptService.saveDescriptor(descriptor);
        assertFalse(result);

        // The descriptor has not been saved
        verify(this.wikiDescriptorManager, never()).saveDescriptor(descriptor);

        String expectedMessage = "Access denied when checking  access to [Wiki wikiId] for user [mainWiki:XWiki.User]";
        assertEquals(expectedMessage, this.wikiManagerScriptService.getLastError().getMessage());
        assertEquals(AccessDeniedException.class, this.wikiManagerScriptService.getLastError().getClass());
        assertEquals(expectedMessage, this.logCapture.getMessage(0));
    }

    @Test
    void saveDescriptorWhenIAmLocalAdminAndChangeOwner() throws Exception
    {
        WikiDescriptor oldDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        oldDescriptor.setOwnerId("SomeUser");
        when(this.wikiDescriptorManager.getById(oldDescriptor.getId())).thenReturn(oldDescriptor);

        // Changing the owner.
        WikiDescriptor descriptor = new WikiDescriptor(oldDescriptor.getId(), "wikiAlias");
        descriptor.setOwnerId("SomeOtherUserOrMyself");
        boolean result = this.wikiManagerScriptService.saveDescriptor(descriptor);
        assertFalse(result);

        // The right has been checked
        verify(this.authorizationManager).hasAccess(eq(Right.ADMIN), eq(this.currentUserRef),
            eq(new WikiReference("wikiId")));

        // The descriptor has not been saved
        verify(this.wikiDescriptorManager, never()).saveDescriptor(descriptor);

        String expectedMessage = "Access denied when checking  access to [Wiki wikiId] for user [mainWiki:XWiki.User]";
        assertEquals(expectedMessage, this.wikiManagerScriptService.getLastError().getMessage());
        assertEquals(AccessDeniedException.class, this.wikiManagerScriptService.getLastError().getClass());
        assertEquals(expectedMessage, this.logCapture.getMessage(0));
    }

    @Test
    void saveDescriptorWhenICanEditDescriptorDocumentAndChangeOwner() throws Exception
    {
        WikiDescriptor oldDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        oldDescriptor.setOwnerId("SomeUser");
        when(this.wikiDescriptorManager.getById(oldDescriptor.getId())).thenReturn(oldDescriptor);

        DocumentReference wikiDescriptorDocRef = getAndSetupDescriptorDocument(oldDescriptor.getId());
        when(this.authorizationManager.hasAccess(Right.EDIT, this.currentUserRef, wikiDescriptorDocRef))
            .thenReturn(true);

        // Changing the owner is possible, since I can directly edit the wiki descriptor anyway.
        WikiDescriptor descriptor = new WikiDescriptor(oldDescriptor.getId(), "wikiAlias");
        descriptor.setOwnerId("SomeOtherUserOrMyself");
        boolean result = this.wikiManagerScriptService.saveDescriptor(descriptor);
        assertTrue(result);

        // The descriptor has been saved
        verify(this.wikiDescriptorManager).saveDescriptor(descriptor);
    }

    @Test
    void saveDescriptorWhenDescriptorDidNotExist() throws Exception
    {
        WikiDescriptor descriptor = new WikiDescriptor("wikiId", "wikiAlias");
        boolean result = this.wikiManagerScriptService.saveDescriptor(descriptor);
        assertFalse(result);

        // Verify the rights have been checked
        verify(this.authorizationManager).hasAccess(eq(Right.ADMIN), eq(this.currentUserRef),
            eq(new WikiReference("mainWiki")));

        // The descriptor has not been saved
        verify(this.wikiDescriptorManager, never()).saveDescriptor(descriptor);

        assertEquals("Access denied when checking  access to [Wiki wikiId] for user [mainWiki:XWiki.User]",
            this.logCapture.getMessage(0));
    }

    @Test
    void saveDescriptorWhenDescriptorDidNotExistAndIAmGlobalAdmin() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN, this.currentUserRef, new WikiReference("mainWiki")))
            .thenReturn(true);

        WikiDescriptor descriptor = new WikiDescriptor("wikiId", "wikiAlias");
        boolean result = this.wikiManagerScriptService.saveDescriptor(descriptor);
        assertTrue(result);

        // Verify the rights have been checked
        verify(this.authorizationManager).hasAccess(eq(Right.ADMIN), eq(this.currentUserRef),
            eq(new WikiReference("mainWiki")));

        // The descriptor has been saved
        verify(this.wikiDescriptorManager).saveDescriptor(descriptor);
    }

    @Test
    void isPathMode()
    {
        when(this.standardURLConfiguration.isPathBasedMultiWiki()).thenReturn(true);
        assertTrue(this.wikiManagerScriptService.isPathMode());

        when(this.standardURLConfiguration.isPathBasedMultiWiki()).thenReturn(false);
        assertFalse(this.wikiManagerScriptService.isPathMode());
    }

    @Test
    void getAliasSuffix()
    {
        when(this.wikiConfiguration.getAliasSuffix()).thenReturn("mysuffix.org");
        assertEquals("mysuffix.org", this.wikiManagerScriptService.getAliasSuffix());
    }

    @Test
    void getCurrentDescriptor() throws Exception
    {
        this.wikiManagerScriptService.getCurrentWikiDescriptor();

        verify(this.wikiDescriptorManager).getCurrentWikiDescriptor();
    }
}
