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
package org.xwiki.platform.wiki.creationjob.script;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.platform.wiki.creationjob.WikiCreator;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class WikiCreationJobScriptServicesTest
{
    @Rule
    public MockitoComponentMockingRule<WikiCreationJobScriptServices> mocker =
            new MockitoComponentMockingRule<>(WikiCreationJobScriptServices.class);

    private WikiCreator wikiCreator;

    private Execution execution;

    private AuthorizationManager authorizationManager;

    private WikiDescriptorManager wikiDescriptorManager;
    
    private DistributionManager distributionManager;

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    private XWiki xwiki;
    
    @Before
    public void setUp() throws Exception
    {
        wikiCreator = mocker.getInstance(WikiCreator.class);
        execution = mocker.getInstance(Execution.class);
        authorizationManager = mocker.getInstance(AuthorizationManager.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        distributionManager = mocker.getInstance(DistributionManager.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWikiId");

        ExecutionContext executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);
        
        ExtensionId extensionId = new ExtensionId("authorized-extension", "1.0");
        when(distributionManager.getWikiUIExtensionId()).thenReturn(extensionId);
    }
    
    @Test
    public void createWiki() throws Exception
    {
        Job job = mock(Job.class);
        when(wikiCreator.createWiki(any(WikiCreationRequest.class))).thenReturn(job);
        
        WikiCreationRequest wikiCreationRequest = new WikiCreationRequest();
        wikiCreationRequest.setExtensionId("authorized-extension", "1.0");
        assertEquals(job, mocker.getComponentUnderTest().createWiki(wikiCreationRequest));
        assertNull(mocker.getComponentUnderTest().getLastError());
    }

    @Test
    public void createWikiWhenExtensionIsNotAuthorized() throws Exception
    {
        WikiCreationRequest wikiCreationRequest = new WikiCreationRequest();
        wikiCreationRequest.setExtensionId("badExtension", "version");
        
        assertNull(mocker.getComponentUnderTest().createWiki(wikiCreationRequest));
        Exception lastError = mocker.getComponentUnderTest().getLastError();
        assertNotNull(lastError);
        assertEquals("The extension [badExtension-version] is not authorized.", lastError.getMessage());
        verify(mocker.getMockedLogger()).warn("Failed to create a new wiki.", lastError);
    }

    @Test
    public void createWikiWhenNoCreateWikiRight() throws Exception
    {
        DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "User");
        when(xcontext.getUserReference()).thenReturn(currentUser);
        AccessDeniedException exception =
                new AccessDeniedException(Right.CREATE_WIKI, currentUser, new WikiReference("mainWikiId"));
        doThrow(exception).when(authorizationManager).checkAccess(eq(Right.CREATE_WIKI), eq(currentUser),
                eq(new WikiReference("mainWikiId")));

        WikiCreationRequest wikiCreationRequest = new WikiCreationRequest();
        wikiCreationRequest.setExtensionId("authorized-extension", "1.0");

        assertNull(mocker.getComponentUnderTest().createWiki(wikiCreationRequest));
        Exception lastError = mocker.getComponentUnderTest().getLastError();
        assertNotNull(lastError);
        assertEquals(exception, lastError);
    }

    @Test
    public void getJobStatus() throws Exception
    {
        JobStatus jobStatus = mock(JobStatus.class);
        when(wikiCreator.getJobStatus("wikiId")).thenReturn(jobStatus);
        assertEquals(jobStatus, mocker.getComponentUnderTest().getJobStatus("wikiId"));
    }

    @Test
    public void newWikiCreationRequest() throws Exception
    {
        assertNotNull(mocker.getComponentUnderTest().newWikiCreationRequest());
    }
}
