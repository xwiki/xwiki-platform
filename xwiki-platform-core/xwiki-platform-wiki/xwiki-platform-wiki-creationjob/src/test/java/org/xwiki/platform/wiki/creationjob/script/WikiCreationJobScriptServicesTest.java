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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentTest
class WikiCreationJobScriptServicesTest
{
    @InjectMockComponents
    private WikiCreationJobScriptServices wikiCreationJobScriptServices;

    @MockComponent
    private WikiCreator wikiCreator;

    @MockComponent
    private Execution execution;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private DistributionManager distributionManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private Logger logger;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @BeforeEach
    void setUp()
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("mainWikiId");

        ExecutionContext executionContext = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(executionContext);

        ExtensionId extensionId = new ExtensionId("authorized-extension", "1.0");
        when(this.distributionManager.getWikiUIExtensionId()).thenReturn(extensionId);
    }

    @Test
    void createWiki() throws Exception
    {
        Job job = mock(Job.class);
        when(this.wikiCreator.createWiki(any(WikiCreationRequest.class))).thenReturn(job);

        WikiCreationRequest wikiCreationRequest = new WikiCreationRequest();
        wikiCreationRequest.setExtensionId("authorized-extension", "1.0");
        assertEquals(job, this.wikiCreationJobScriptServices.createWiki(wikiCreationRequest));
        assertNull(this.wikiCreationJobScriptServices.getLastError());
    }

    @Test
    void createWikiWhenExtensionIsNotAuthorized()
    {
        WikiCreationRequest wikiCreationRequest = new WikiCreationRequest();
        wikiCreationRequest.setExtensionId("badExtension", "version");

        assertNull(this.wikiCreationJobScriptServices.createWiki(wikiCreationRequest));
        Exception lastError = this.wikiCreationJobScriptServices.getLastError();
        assertNotNull(lastError);
        assertEquals("The extension [badExtension/version] is not authorized.", lastError.getMessage());
        verify(this.logger).warn("Failed to create a new wiki.", lastError);
    }

    @Test
    void createWikiWhenNoCreateWikiRight() throws Exception
    {
        DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "User");
        when(this.xcontext.getUserReference()).thenReturn(currentUser);
        AccessDeniedException exception =
            new AccessDeniedException(Right.CREATE_WIKI, currentUser, new WikiReference("mainWikiId"));
        doThrow(exception).when(this.authorizationManager).checkAccess(eq(Right.CREATE_WIKI), eq(currentUser),
            eq(new WikiReference("mainWikiId")));

        WikiCreationRequest wikiCreationRequest = new WikiCreationRequest();
        wikiCreationRequest.setExtensionId("authorized-extension", "1.0");

        assertNull(this.wikiCreationJobScriptServices.createWiki(wikiCreationRequest));
        Exception lastError = this.wikiCreationJobScriptServices.getLastError();
        assertNotNull(lastError);
        assertEquals(exception, lastError);
    }

    @Test
    void getJobStatus()
    {
        JobStatus jobStatus = mock(JobStatus.class);
        when(this.wikiCreator.getJobStatus("wikiId")).thenReturn(jobStatus);
        assertEquals(jobStatus, this.wikiCreationJobScriptServices.getJobStatus("wikiId"));
    }

    @Test
    void newWikiCreationRequest()
    {
        assertNotNull(this.wikiCreationJobScriptServices.newWikiCreationRequest());
    }
}
