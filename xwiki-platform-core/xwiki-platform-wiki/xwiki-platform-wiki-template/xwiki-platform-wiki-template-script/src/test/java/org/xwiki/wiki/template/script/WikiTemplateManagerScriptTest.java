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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.provisioning.WikiProvisioningJob;
import org.xwiki.wiki.template.WikiTemplateManager;
import org.xwiki.wiki.template.WikiTemplateManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.security.authorization.Right.ADMIN;

/**
 * Test of {@link WikiTemplateManagerScript}.
 *
 * @version $Id$
 */
@ComponentTest
class WikiTemplateManagerScriptTest
{
    @InjectMockComponents
    private WikiTemplateManagerScript wikiTemplateManagerScript;

    @MockComponent
    private WikiTemplateManager wikiTemplateManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Mock
    private XWikiContext xcontext;

    @MockComponent
    private Execution execution;

    private static final DocumentReference CURRENT_USER_REF = new DocumentReference("mainWiki", "XWiki", "User");

    @Mock
    private XWikiDocument currentDoc;

    private ExecutionContext executionContext;

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.executionContext = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(this.executionContext);
        when(this.xcontext.getUserReference()).thenReturn(CURRENT_USER_REF);
        when(this.xcontext.getDoc()).thenReturn(this.currentDoc);
        when(this.xcontext.getMainXWiki()).thenReturn("mainWiki");
        when(this.entityReferenceSerializer.serialize(CURRENT_USER_REF)).thenReturn("mainWiki:XWiki.User");
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
     * @return the exception expected when the current user has the not the admin right
     */
    private Exception currentUserHasNotAdminRight() throws AccessDeniedException
    {
        WikiReference wiki = new WikiReference("wikiId");
        Exception exception = new AccessDeniedException(ADMIN, CURRENT_USER_REF, wiki);
        doThrow(exception).when(this.authorizationManager).checkAccess(ADMIN, CURRENT_USER_REF, wiki);

        return exception;
    }

    /**
     * @return the exception expected when the current user has the not the 'create wiki' right
     */
    private Exception currentUserHasNotCreateWikiRight() throws AccessDeniedException
    {
        WikiReference wiki = new WikiReference("mainWiki");
        Exception exception = new AccessDeniedException(Right.CREATE_WIKI, CURRENT_USER_REF, wiki);
        doThrow(exception).when(this.authorizationManager).checkAccess(Right.CREATE_WIKI, CURRENT_USER_REF, wiki);

        return exception;
    }

    @Test
    void getTemplates() throws Exception
    {
        Collection<WikiDescriptor> templates = new ArrayList<>();
        WikiDescriptor descriptor = new WikiDescriptor("templateId", "templateAlias");
        templates.add(descriptor);

        when(this.wikiTemplateManager.getTemplates()).thenReturn(templates);

        Collection<WikiDescriptor> results = this.wikiTemplateManagerScript.getTemplates();
        assertEquals(templates, results);
    }

    @Test
    void getTemplatesError() throws Exception
    {
        Exception exception = new WikiTemplateManagerException("Error in getTemplates");
        when(this.wikiTemplateManager.getTemplates()).thenThrow(exception);

        Collection<WikiDescriptor> results = this.wikiTemplateManagerScript.getTemplates();
        assertTrue(results.isEmpty());
        assertEquals(exception, this.wikiTemplateManagerScript.getLastError());
        assertEquals("Error while getting all the wiki templates.", this.logCapture.getMessage(0));
    }

    @Test
    void setTemplateWhenCurrentUserIsOwner() throws Exception
    {
        WikiDescriptor wikiDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        wikiDescriptor.setOwnerId("mainWiki:XWiki.User");
        when(this.wikiDescriptorManager.getById("wikiId")).thenReturn(wikiDescriptor);

        // Test 1
        boolean result = this.wikiTemplateManagerScript.setTemplate("wikiId", true);
        assertTrue(result);
        verify(this.wikiTemplateManager).setTemplate("wikiId", true);

        // Test 2
        result = this.wikiTemplateManagerScript.setTemplate("wikiId", false);
        assertTrue(result);
        verify(this.wikiTemplateManager).setTemplate("wikiId", false);
    }

    @Test
    void setTemplateWithoutPR() throws Exception
    {
        Exception exception = currentScriptHasNotProgrammingRight();

        boolean result = this.wikiTemplateManagerScript.setTemplate("wikiId", true);
        assertFalse(result);
        assertEquals(exception, this.wikiTemplateManagerScript.getLastError());
        assertEquals("""
                Access denied for [mainWiki:XWiki.User] to change the template value of the wiki [wikiId]. \
                The user has not the right to perform this operation or the script has not the programming right.""",
            this.logCapture.getMessage(0));
    }

    @Test
    void setTemplateWithoutAdminRight() throws Exception
    {
        Exception exception = currentUserHasNotAdminRight();

        WikiDescriptor wikiDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        when(this.wikiDescriptorManager.getById("wikiId")).thenReturn(wikiDescriptor);

        boolean result = this.wikiTemplateManagerScript.setTemplate("wikiId", true);
        assertFalse(result);
        assertEquals(exception, this.wikiTemplateManagerScript.getLastError());
        assertEquals("""
                Access denied for [mainWiki:XWiki.User] to change the template value of the wiki [wikiId]. The user \
                has not the right to perform this operation or the script has not the programming right.""",
            this.logCapture.getMessage(0));
    }

    @Test
    void setTemplateErrorWithDescriptorManager() throws Exception
    {
        Exception exception = new WikiManagerException("error in getById");
        when(this.wikiDescriptorManager.getById("wikiId")).thenThrow(exception);

        boolean result = this.wikiTemplateManagerScript.setTemplate("wikiId", true);
        assertFalse(result);
        assertEquals(exception, this.wikiTemplateManagerScript.getLastError());
        assertEquals("Failed to get the descriptor of the wiki [wikiId].", this.logCapture.getMessage(0));
    }

    @Test
    void setTemplateErrorWithTemplateManager() throws Exception
    {
        WikiDescriptor wikiDescriptor = new WikiDescriptor("wikiId", "wikiAlias");
        when(this.wikiDescriptorManager.getById("wikiId")).thenReturn(wikiDescriptor);

        Exception exception = new WikiTemplateManagerException("error in setTemplate");
        doThrow(exception).when(this.wikiTemplateManager).setTemplate("wikiId", true);

        boolean result = this.wikiTemplateManagerScript.setTemplate("wikiId", true);
        assertFalse(result);
        assertEquals(exception, this.wikiTemplateManagerScript.getLastError());
        assertEquals("Failed to set the template value [true] for the wiki [wikiId].", this.logCapture.getMessage(0));
    }

    @Test
    void isTemplate() throws Exception
    {
        when(this.wikiTemplateManager.isTemplate("wikiTemplate")).thenReturn(true);
        when(this.wikiTemplateManager.isTemplate("subwiki")).thenReturn(false);

        assertTrue(this.wikiTemplateManagerScript.isTemplate("wikiTemplate"));
        assertFalse(this.wikiTemplateManagerScript.isTemplate("subwiki"));
    }

    @Test
    void isTemplateError() throws Exception
    {
        Exception exception = new WikiTemplateManagerException("error in isTemplate");

        when(this.wikiTemplateManager.isTemplate("wikiTemplate")).thenThrow(exception);

        assertNull(this.wikiTemplateManagerScript.isTemplate("wikiTemplate"));
        assertEquals(exception, this.wikiTemplateManagerScript.getLastError());
        assertEquals("Failed to get if the wiki [wikiTemplate] is a template or not.", this.logCapture.getMessage(0));
    }

    @Test
    void createWikiFromTemplate() throws Exception
    {
        // Test
        boolean result = this.wikiTemplateManagerScript.createWikiFromTemplate("newWikiId", "newWikiAlias",
                "templateId", "ownerId", true);

        // Verify
        assertTrue(result);
        verify(this.wikiTemplateManager).createWikiFromTemplate("newWikiId", "newWikiAlias", "templateId", "ownerId",
            true);
    }

    @Test
    void createWikiFromTemplateWithoutPR() throws Exception
    {
        Exception exception = currentScriptHasNotProgrammingRight();

        // Test
        boolean result = this.wikiTemplateManagerScript.createWikiFromTemplate("newWikiId", "newWikiAlias",
                "templateId", "ownerId", true);

        // Verify
        assertFalse(result);
        assertEquals(exception, this.wikiTemplateManagerScript.getLastError());
        assertEquals("Error, you or this script does not have the right to create a wiki from a template.",
            this.logCapture.getMessage(0));
    }

    @Test
    void createWikiFromTemplateWithoutCreateRight() throws Exception
    {
        Exception exception = currentUserHasNotCreateWikiRight();

        // Test
        boolean result = this.wikiTemplateManagerScript.createWikiFromTemplate("newWikiId", "newWikiAlias",
                "templateId", "ownerId", true);

        // Verify
        assertFalse(result);
        assertEquals(exception, this.wikiTemplateManagerScript.getLastError());
        assertEquals("Error, you or this script does not have the right to create a wiki from a template.",
            this.logCapture.getMessage(0));
    }

    @Test
    void createWikiFromTemplateError() throws Exception
    {
        Exception exception = new WikiTemplateManagerException("error in createWikiFromTemplate.");

        when(this.wikiTemplateManager.createWikiFromTemplate("newWikiId", "newWikiAlias", "templateId",
                "ownerId", true)).thenThrow(exception);

        // Test
        boolean result = this.wikiTemplateManagerScript.createWikiFromTemplate("newWikiId", "newWikiAlias",
                "templateId", "ownerId", true);

        // Verify
        assertFalse(result);
        assertEquals(exception, this.wikiTemplateManagerScript.getLastError());
        assertEquals("Failed to create the wiki from the template.", this.logCapture.getMessage(0));
    }

    @Test
    void getLastException()
    {
        Exception exception = new Exception("test");
        this.executionContext.setProperty(WikiTemplateManagerScript.CONTEXT_LASTEXCEPTION, exception);
        assertEquals(exception, this.wikiTemplateManagerScript.getLastException());
    }

    @Test
    void getWikiProvisioningJobStatus() throws Exception
    {
        WikiProvisioningJob job = mock(WikiProvisioningJob.class);
        when(this.wikiTemplateManager.getWikiProvisioningJob(anyList())).thenReturn(job);
        JobStatus status = mock(JobStatus.class);
        when(job.getStatus()).thenReturn(status);

        List<String> jobId = new ArrayList<>();
        JobStatus result = this.wikiTemplateManagerScript.getWikiProvisioningJobStatus(jobId);

        assertEquals(status, result);
    }

    @Test
    void getWikiProvisioningJobStatusWithBadId()
    {
        List<String> jobId = new ArrayList<>();
        JobStatus result = this.wikiTemplateManagerScript.getWikiProvisioningJobStatus(jobId);

        assertNull(result);
    }

    @Test
    void getWikiProvisioningJobStatusWithException() throws Exception
    {
        Exception exception = new WikiTemplateManagerException("test");
        when(this.wikiTemplateManager.getWikiProvisioningJob(anyList())).thenThrow(exception);

        List<String> jobId = new ArrayList<>();
        JobStatus result = this.wikiTemplateManagerScript.getWikiProvisioningJobStatus(jobId);

        assertNull(result);
        assertEquals(exception, this.wikiTemplateManagerScript.getLastError());
        assertEquals("Failed to get tge wiki provisioning job.", this.logCapture.getMessage(0));

    }

}
