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
package org.xwiki.extension.script;

import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.job.history.ExtensionJobHistory;
import org.xwiki.extension.job.history.ExtensionJobHistoryRecord;
import org.xwiki.extension.job.history.ReplayRequest;
import org.xwiki.extension.job.history.internal.ReplayJob;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ExtensionHistoryScriptService}.
 * 
 * @version $Id$
 * @since 7.1RC1
 */
public class ExtensionHistoryScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<ExtensionHistoryScriptService> mocker =
        new MockitoComponentMockingRule<ExtensionHistoryScriptService>(ExtensionHistoryScriptService.class,
            ScriptService.class);

    private XWikiContext xcontext = mock(XWikiContext.class);

    private ExecutionContext executionContext = new ExecutionContext();

    private JobExecutor jobExecutor;

    private ContextualAuthorizationManager authorization;

    private DocumentAccessBridge documentAccessBridge;

    @Before
    public void configure() throws Exception
    {
        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);

        Execution execution = this.mocker.getInstance(Execution.class);
        when(execution.getContext()).thenReturn(this.executionContext);

        this.jobExecutor = this.mocker.getInstance(JobExecutor.class);
        this.authorization = this.mocker.getInstance(ContextualAuthorizationManager.class);
        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
    }

    @Test
    public void getRecords() throws Exception
    {
        InstallRequest devInstallReq = new InstallRequest();
        devInstallReq.addNamespace("wiki:dev");
        ExtensionJobHistoryRecord devInstall =
            new ExtensionJobHistoryRecord("install", devInstallReq, null, null, null);

        UninstallRequest devUninstallReq = new UninstallRequest();
        devUninstallReq.addNamespace("wiki:dev");
        ExtensionJobHistoryRecord devUninstall =
            new ExtensionJobHistoryRecord("uninstall", devUninstallReq, null, null, null);

        ExtensionJobHistoryRecord globalInstall =
            new ExtensionJobHistoryRecord("install", new InstallRequest(), null, null, null);

        ExtensionJobHistoryRecord globalUninstall =
            new ExtensionJobHistoryRecord("uninstall", new UninstallRequest(), null, null, null);

        InstallRequest draftsInstallReq = new InstallRequest();
        draftsInstallReq.addNamespace("wiki:drafts");
        ExtensionJobHistoryRecord draftsInstall =
            new ExtensionJobHistoryRecord("install", draftsInstallReq, null, null, null);

        List<ExtensionJobHistoryRecord> records = Arrays.asList(devInstall, globalInstall);

        ExtensionJobHistory history = this.mocker.getInstance(ExtensionJobHistory.class);
        ArgumentCaptor<Predicate<ExtensionJobHistoryRecord>> predicateCaptor =
            ArgumentCaptor.forClass((Class) Predicate.class);
        when(history.getRecords(predicateCaptor.capture(), eq("offsetRecordId"), eq(5))).thenReturn(records);

        when(this.xcontext.getWikiId()).thenReturn("dev");

        assertEquals(
            records,
            this.mocker.getComponentUnderTest().getRecords().fromThisWiki().ofType(Arrays.asList("install"))
                .list("offsetRecordId", 5));

        Predicate<ExtensionJobHistoryRecord> predicate = predicateCaptor.getValue();
        assertTrue(predicate.evaluate(devInstall));
        assertTrue(predicate.evaluate(globalInstall));
        assertFalse(predicate.evaluate(devUninstall));
        assertFalse(predicate.evaluate(globalUninstall));
        assertFalse(predicate.evaluate(draftsInstall));
    }

    @Test
    public void replayWithoutAdmin() throws Exception
    {
        InstallRequest installRequest = new InstallRequest();
        installRequest.addNamespace("wiki:dev");
        ExtensionJobHistoryRecord install = new ExtensionJobHistoryRecord("install", installRequest, null, null, null);
        List<ExtensionJobHistoryRecord> records = Arrays.asList(install);

        when(this.xcontext.getWikiId()).thenReturn("dev");
        when(this.authorization.hasAccess(Right.ADMIN, new WikiReference("dev"))).thenReturn(false);

        Job replayJob = mock(Job.class);
        ArgumentCaptor<ReplayRequest> requestCaptor = ArgumentCaptor.forClass(ReplayRequest.class);
        when(jobExecutor.execute(eq(ReplayJob.JOB_TYPE), requestCaptor.capture())).thenReturn(replayJob);

        assertSame(replayJob, this.mocker.getComponentUnderTest().replay(records));

        ReplayRequest request = requestCaptor.getValue();
        assertTrue(request.getRecords().isEmpty());
    }

    @Test
    public void replayWithAdminButNoPR() throws Exception
    {
        InstallRequest installRequest = new InstallRequest();
        installRequest.addNamespace("wiki:drafts");
        installRequest.setProperty("user.reference", new DocumentReference("drafts", "Users", "Alice"));
        ExtensionJobHistoryRecord install = new ExtensionJobHistoryRecord("install", installRequest, null, null, null);
        List<ExtensionJobHistoryRecord> records = Arrays.asList(install);

        when(this.xcontext.getWikiId()).thenReturn("dev");
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(
            new DocumentReference("dev", "Users", "Bob"));
        when(this.authorization.hasAccess(Right.ADMIN, new WikiReference("dev"))).thenReturn(true);
        when(this.authorization.hasAccess(Right.PROGRAM)).thenReturn(false);

        Job replayJob = mock(Job.class);
        ArgumentCaptor<ReplayRequest> requestCaptor = ArgumentCaptor.forClass(ReplayRequest.class);
        when(jobExecutor.execute(eq(ReplayJob.JOB_TYPE), requestCaptor.capture())).thenReturn(replayJob);

        assertSame(replayJob, this.mocker.getComponentUnderTest().replay(records));

        ReplayRequest request = requestCaptor.getValue();
        assertEquals(Arrays.asList(install), request.getRecords());

        assertEquals(Arrays.asList("wiki:dev"), install.getRequest().getNamespaces());
        assertEquals(this.documentAccessBridge.getCurrentUserReference(),
            install.getRequest().getProperty("user.reference"));
    }

    @Test
    public void replayWithPR() throws Exception
    {
        InstallRequest installRequest = new InstallRequest();
        installRequest.addNamespace("wiki:drafts");
        installRequest.setProperty("user.reference", new DocumentReference("drafts", "Users", "Alice"));
        ExtensionJobHistoryRecord install = new ExtensionJobHistoryRecord("install", installRequest, null, null, null);
        List<ExtensionJobHistoryRecord> records = Arrays.asList(install);

        when(this.xcontext.getWikiId()).thenReturn("dev");
        when(this.xcontext.getAction()).thenReturn("foo");
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(
            new DocumentReference("dev", "Users", "Bob"));
        when(this.authorization.hasAccess(Right.ADMIN, new WikiReference("dev"))).thenReturn(true);
        when(this.authorization.hasAccess(Right.PROGRAM)).thenReturn(true);

        Job replayJob = mock(Job.class);
        ArgumentCaptor<ReplayRequest> requestCaptor = ArgumentCaptor.forClass(ReplayRequest.class);
        when(jobExecutor.execute(eq(ReplayJob.JOB_TYPE), requestCaptor.capture())).thenReturn(replayJob);

        assertSame(replayJob, this.mocker.getComponentUnderTest().replay(records));

        ReplayRequest request = requestCaptor.getValue();
        assertTrue(StringUtils.join(request.getId(), '/').startsWith("extension/history/"));
        assertTrue(request.isInteractive());
        assertEquals(Arrays.asList(install), request.getRecords());
        assertEquals(this.xcontext.getWikiId(), request.getProperty("context.wiki"));
        assertEquals(this.xcontext.getAction(), request.getProperty("context.action"));
        assertEquals(this.documentAccessBridge.getCurrentUserReference(), request.getProperty("user.reference"));

        assertEquals(Arrays.asList("wiki:drafts"), install.getRequest().getNamespaces());
        assertEquals(new DocumentReference("drafts", "Users", "Alice"),
            install.getRequest().getProperty("user.reference"));
    }
}
