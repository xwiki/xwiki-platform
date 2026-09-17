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

import javax.inject.Provider;

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
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.job.DiffXarJob;
import org.xwiki.extension.xar.internal.job.RepairXarJob;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentVersionReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @MockComponent
    private AuthorizationManager genericAuthorization;

    @MockComponent
    private Packager packager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    private ExecutionContext executionContext = new ExecutionContext();

    private XWikiContext xcontext = mock(XWikiContext.class);

    private DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");

    private DocumentReference authorReference = new DocumentReference("wiki", "XWiki", "Author");

    private DocumentReference userReference = new DocumentReference("wiki", "XWiki", "User");

    @BeforeEach
    public void setup()
    {
        when(execution.getContext()).thenReturn(executionContext);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getAuthorReference()).thenReturn(this.authorReference);
        when(this.xcontext.getUserReference()).thenReturn(this.userReference);
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

    @Test
    void reset() throws Exception
    {
        assertTrue(this.xarExtensionScriptService.reset(this.documentReference, null));

        verify(this.packager).resetWithContextUser(this.documentReference);
    }

    @Test
    void resetWhenAuthorIsNotAllowed() throws Exception
    {
        AccessDeniedException expectedException =
            new AccessDeniedException(Right.EDIT, this.authorReference, this.documentReference);
        doThrow(expectedException).when(this.genericAuthorization).checkAccess(Right.EDIT, this.authorReference,
            this.documentReference);

        assertFalse(this.xarExtensionScriptService.reset(this.documentReference, null));

        assertSame(expectedException, this.xarExtensionScriptService.getLastError());
        verify(this.packager, never()).resetWithContextUser(this.documentReference);
    }

    @Test
    void resetWhenContextUserIsNotAllowed() throws Exception
    {
        // The author of the script asking for the reset is allowed, but the user browsing it is not.
        AccessDeniedException expectedException =
            new AccessDeniedException(Right.EDIT, this.userReference, this.documentReference);
        doThrow(expectedException).when(this.genericAuthorization).checkAccess(Right.EDIT, this.userReference,
            this.documentReference);

        assertFalse(this.xarExtensionScriptService.reset(this.documentReference, null));

        assertSame(expectedException, this.xarExtensionScriptService.getLastError());
        verify(this.packager, never()).resetWithContextUser(this.documentReference);
    }

    @Test
    void resetWithExtensionIdWhenContextUserIsNotAllowed() throws Exception
    {
        DocumentVersionReference versionReference =
            new DocumentVersionReference(this.documentReference, new ExtensionId("extension", "1.0"));
        AccessDeniedException expectedException =
            new AccessDeniedException(Right.EDIT, this.userReference, versionReference);
        doThrow(expectedException).when(this.genericAuthorization).checkAccess(Right.EDIT, this.userReference,
            versionReference);

        assertFalse(this.xarExtensionScriptService.reset(this.documentReference,
            new ExtensionId("extension", "1.0"), null));

        assertSame(expectedException, this.xarExtensionScriptService.getLastError());
        verify(this.packager, never()).resetWithContextUser(versionReference);
    }
}
