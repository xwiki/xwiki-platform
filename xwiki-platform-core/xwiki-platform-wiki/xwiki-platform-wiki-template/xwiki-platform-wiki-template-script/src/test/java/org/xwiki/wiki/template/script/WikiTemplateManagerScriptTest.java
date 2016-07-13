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
package org.xwiki.wiki.template.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.provisioning.WikiProvisioningJob;
import org.xwiki.wiki.template.WikiTemplateManager;
import org.xwiki.wiki.template.WikiTemplateManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WikiTemplateManagerScriptTest
{
    @Rule
    public MockitoComponentMockingRule<WikiTemplateManagerScript> mocker =
            new MockitoComponentMockingRule(WikiTemplateManagerScript.class);

    private WikiTemplateManager wikiTemplateManager;

    private WikiDescriptorManager wikiDescriptorManager;

    private AuthorizationManager authorizationManager;

    private Provider<XWikiContext> xcontextProvider;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private XWikiContext xcontext;

    private Execution execution;

    private DocumentReference currentUserRef;

    private XWikiDocument currentDoc;

    private ExecutionContext executionContext;

    @Before
    public void setUp() throws Exception
    {
        wikiTemplateManager = mocker.getInstance(WikiTemplateManager.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        authorizationManager = mocker.getInstance(AuthorizationManager.class);
        entityReferenceSerializer = mocker.getInstance(new DefaultParameterizedType(null,
                EntityReferenceSerializer.class, String.class));
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

        when(xcontext.getMainXWiki()).thenReturn("mainWiki");

        when(entityReferenceSerializer.serialize(currentUserRef)).thenReturn("mainWiki:XWiki.User");
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
        WikiReference wiki = new WikiReference("wikiId");
        Exception exception = new AccessDeniedException(Right.ADMIN, currentUserRef, wiki);
        doThrow(exception).when(authorizationManager).checkAccess(eq(Right.ADMIN), eq(currentUserRef), eq(wiki));

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

    @Test
    public void getTemplates() throws Exception
    {
        Collection<WikiDescriptor> templates = new ArrayList<WikiDescriptor>();
        WikiDescriptor descriptor = new WikiDescriptor("templateId", "templateAlias");
        templates.add(descriptor);

        when(wikiTemplateManager.getTemplates()).thenReturn(templates);

        Collection<WikiDescriptor> results = mocker.getComponentUnderTest().getTemplates();
        assertEquals(templates, results);
    }

    @Test
    public void getTemplatesError() throws Exception
    {
        Exception exception = new WikiTemplateManagerException("Error in getTemplates");
        when(wikiTemplateManager.getTemplates()).thenThrow(exception);

        Collection<WikiDescriptor> results = mocker.getComponentUnderTest().getTemplates();
        assertTrue(results.isEmpty());
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
        verify(mocker.getMockedLogger()).error("Error while getting all the wiki templates.", exception);
    }

    @Test
    public void setTemplateWhenCurrentUserIsOwner() throws Exception
    {
        WikiDescriptor wikiDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        wikiDescriptor.setOwnerId("mainWiki:XWiki.User");
        when(wikiDescriptorManager.getById("wikiId")).thenReturn(wikiDescriptor);

        // Test 1
        boolean result = mocker.getComponentUnderTest().setTemplate("wikiId", true);
        assertTrue(result);
        verify(wikiTemplateManager).setTemplate("wikiId", true);

        // Test 2
        result = mocker.getComponentUnderTest().setTemplate("wikiId", false);
        assertTrue(result);
        verify(wikiTemplateManager).setTemplate("wikiId", false);
    }

    @Test
    public void setTemplateWithoutPR() throws Exception
    {
        Exception exception = currentScriptHasNotProgrammingRight();

        boolean result = mocker.getComponentUnderTest().setTemplate("wikiId", true);
        assertFalse(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
        verify(mocker.getMockedLogger()).error("Access denied for [mainWiki:XWiki.User] to change the template value" +
                " of the wiki [wikiId]. The user has not the right to perform this operation or the script has not " +
                "the programming right.", exception);
    }

    @Test
    public void setTemplateWithoutAdminRight() throws Exception
    {
        Exception exception = currentUserHasNotAdminRight();

        WikiDescriptor wikiDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        when(wikiDescriptorManager.getById("wikiId")).thenReturn(wikiDescriptor);

        boolean result = mocker.getComponentUnderTest().setTemplate("wikiId", true);
        assertFalse(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
        verify(mocker.getMockedLogger()).error("Access denied for [mainWiki:XWiki.User] to change the template value" +
                " of the wiki [wikiId]. The user has not the right to perform this operation or the script has not " +
                "the programming right.", exception);
    }

    @Test
    public void setTemplateErrorWithDescriptorManager() throws Exception
    {
        Exception exception = new WikiManagerException("error in getById");
        when(wikiDescriptorManager.getById("wikiId")).thenThrow(exception);

        boolean result = mocker.getComponentUnderTest().setTemplate("wikiId", true);
        assertFalse(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
        verify(mocker.getMockedLogger()).error("Failed to get the descriptor of the wiki [wikiId].", exception);
    }

    @Test
    public void setTemplateErrorWithTemplateManager() throws Exception
    {
        WikiDescriptor wikiDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        when(wikiDescriptorManager.getById("wikiId")).thenReturn(wikiDescriptor);

        Exception exception = new WikiTemplateManagerException("error in setTemplate");
        doThrow(exception).when(wikiTemplateManager).setTemplate("wikiId", true);

        boolean result = mocker.getComponentUnderTest().setTemplate("wikiId", true);
        assertFalse(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
        verify(mocker.getMockedLogger()).error("Failed to set the template value [true] for the wiki [wikiId].",
                exception);
    }

    @Test
    public void isTemplate() throws Exception
    {
        when(wikiTemplateManager.isTemplate("wikiTemplate")).thenReturn(true);
        when(wikiTemplateManager.isTemplate("subwiki")).thenReturn(false);

        assertTrue(mocker.getComponentUnderTest().isTemplate("wikiTemplate"));
        assertFalse(mocker.getComponentUnderTest().isTemplate("subwiki"));
    }

    @Test
    public void isTemplateError() throws Exception
    {
        Exception exception = new WikiTemplateManagerException("error in isTemplate");

        when(wikiTemplateManager.isTemplate("wikiTemplate")).thenThrow(exception);

        assertNull(mocker.getComponentUnderTest().isTemplate("wikiTemplate"));
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
        verify(mocker.getMockedLogger()).error("Failed to get if the wiki [wikiTemplate] is a template or not.",
                exception);
    }

    @Test
    public void createWikiFromTemplate() throws Exception
    {
        // Test
        boolean result = mocker.getComponentUnderTest().createWikiFromTemplate("newWikiId", "newWikiAlias",
                "templateId", "ownerId", true);

        // Verify
        assertTrue(result);
        verify(wikiTemplateManager).createWikiFromTemplate("newWikiId", "newWikiAlias", "templateId", "ownerId", true);
    }

    @Test
    public void createWikiFromTemplateWithoutPR() throws Exception
    {
        Exception exception = currentScriptHasNotProgrammingRight();

        // Test
        boolean result = mocker.getComponentUnderTest().createWikiFromTemplate("newWikiId", "newWikiAlias",
                "templateId", "ownerId", true);

        // Verify
        assertFalse(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
        verify(mocker.getMockedLogger()).error(
                "Error, you or this script does not have the right to create a wiki from a template.", exception);
    }

    @Test
    public void createWikiFromTemplateWithoutCreateRight() throws Exception
    {
        Exception exception = currentUserHasNotCreateWikiRight();

        // Test
        boolean result = mocker.getComponentUnderTest().createWikiFromTemplate("newWikiId", "newWikiAlias",
                "templateId", "ownerId", true);

        // Verify
        assertFalse(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
        verify(mocker.getMockedLogger()).error(
                "Error, you or this script does not have the right to create a wiki from a template.", exception);
    }

    @Test
    public void createWikiFromTemplateError() throws Exception
    {
        Exception exception = new WikiTemplateManagerException("error in createWikiFromTemplate.");

        when(wikiTemplateManager.createWikiFromTemplate("newWikiId", "newWikiAlias", "templateId",
                "ownerId", true)).thenThrow(exception);

        // Test
        boolean result = mocker.getComponentUnderTest().createWikiFromTemplate("newWikiId", "newWikiAlias",
                "templateId", "ownerId", true);

        // Verify
        assertFalse(result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
        verify(mocker.getMockedLogger()).error("Failed to create the wiki from the template.", exception);
    }

    @Test
    public void getLastException() throws Exception
    {
        Exception exception = new Exception("test");
        executionContext.setProperty(WikiTemplateManagerScript.CONTEXT_LASTEXCEPTION, exception);
        assertEquals(exception, mocker.getComponentUnderTest().getLastException());
    }

    @Test
    public void getWikiProvisioningJobStatus() throws Exception
    {
        WikiProvisioningJob job = mock(WikiProvisioningJob.class);
        when(wikiTemplateManager.getWikiProvisioningJob(anyList())).thenReturn(job);
        JobStatus status = mock(JobStatus.class);
        when(job.getStatus()).thenReturn(status);

        List<String> jobId = new ArrayList<String>();
        JobStatus result = mocker.getComponentUnderTest().getWikiProvisioningJobStatus(jobId);

        assertEquals(status, result);
    }

    @Test
    public void getWikiProvisioningJobStatusWithBadId() throws Exception
    {
        List<String> jobId = new ArrayList<String>();
        JobStatus result = mocker.getComponentUnderTest().getWikiProvisioningJobStatus(jobId);

        assertEquals(null, result);
    }

    @Test
    public void getWikiProvisioningJobStatusWithException() throws Exception
    {
        Exception exception = new WikiTemplateManagerException("test");
        when(wikiTemplateManager.getWikiProvisioningJob(anyList())).thenThrow(exception);

        List<String> jobId = new ArrayList<String>();
        JobStatus result = mocker.getComponentUnderTest().getWikiProvisioningJobStatus(jobId);

        assertEquals(null, result);
        assertEquals(exception, mocker.getComponentUnderTest().getLastError());
        verify(mocker.getMockedLogger()).error("Failed to get tge wiki provisioning job.", exception);

    }

}
