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
package org.xwiki.extension.xar.script;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.internal.validator.AbstractExtensionValidator;
import org.xwiki.extension.job.ExtensionRequest;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.xar.internal.job.DiffXarJob;
import org.xwiki.extension.xar.internal.job.RepairXarJob;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of {@link XarExtensionScriptService}.
 *
 * @version $Id$
 * @since 11.0
 */
@ComponentTest
class XarExtensionScriptServiceTest
{
    @InjectMockComponents
    private XarExtensionScriptService xarExtensionScriptService;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private Execution execution;

    private ExecutionContext executionContext = new ExecutionContext();

    @BeforeEach
    public void setup()
    {
        when(execution.getContext()).thenReturn(executionContext);
    }

    @Test
    void repairInstalledExtension() throws JobException
    {
        when(this.authorization.hasAccess(Right.PROGRAM)).thenReturn(true);
        String wiki = "subwiki";
        String extensionId = "fakeextension";
        String version = "1.3";
        String wikiNamespace = "wiki:subwiki";
        InstallRequest expectedInstallRequest = new InstallRequest();
        List<String> jobId =
            ExtensionRequest.getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, extensionId, wikiNamespace);
        expectedInstallRequest.setId(jobId);
        DocumentReference userReference = new DocumentReference("wiki", "Alice", "WebHome");
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(userReference);
        expectedInstallRequest.setProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE, userReference);
        expectedInstallRequest.setExtensionProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE,
            userReference.toString());
        expectedInstallRequest.addExtension(new ExtensionId(extensionId, version));
        expectedInstallRequest.addNamespace(wikiNamespace);

        this.xarExtensionScriptService.repairInstalledExtension(extensionId, version, wiki);
        ArgumentCaptor<InstallRequest> installRequestArgumentCaptor = ArgumentCaptor.forClass(InstallRequest.class);
        verify(this.jobExecutor).execute(eq(RepairXarJob.JOBTYPE), installRequestArgumentCaptor.capture());
        InstallRequest installRequest = installRequestArgumentCaptor.getValue();
        assertEquals(expectedInstallRequest.getId(), installRequest.getId());
        assertEquals(expectedInstallRequest.getProperties(), installRequest.getProperties());
    }

    @Test
    void diff() throws JobException
    {
        when(this.authorization.hasAccess(Right.PROGRAM)).thenReturn(true);
        String wiki = "subwiki";
        String feature = "fakefeature";
        String wikiNamespace = "wiki:subwiki";
        InstallRequest expectedInstallRequest = new InstallRequest();
        List<String> jobId =
            ExtensionRequest.getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, feature, wikiNamespace);
        expectedInstallRequest.setId(jobId);
        expectedInstallRequest.addExtension(new ExtensionId(feature, (Version) null));
        expectedInstallRequest.addNamespace(wikiNamespace);

        this.xarExtensionScriptService.diff(feature, wiki);
        ArgumentCaptor<InstallRequest> installRequestArgumentCaptor = ArgumentCaptor.forClass(InstallRequest.class);
        verify(this.jobExecutor).execute(eq(DiffXarJob.JOB_TYPE), installRequestArgumentCaptor.capture());
        InstallRequest installRequest = installRequestArgumentCaptor.getValue();
        assertEquals(expectedInstallRequest.getId(), installRequest.getId());
        assertEquals(expectedInstallRequest.getProperties(), installRequest.getProperties());
    }
}
