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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.test.MockitoRepositoryUtilsRule;
import org.xwiki.job.Job;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.util.XWikiStubContextProvider;

@AllComponents
public class ExtensionManagerScriptServiceTest
{
    private MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    private MockitoOldcoreRule xwikiBridge = new MockitoOldcoreRule(this.mocker);

    @Rule
    public MockitoRepositoryUtilsRule repositoryUtil = new MockitoRepositoryUtilsRule(this.mocker, this.xwikiBridge);

    private XWiki mockXWiki;

    private Map<String, BaseClass> classes = new HashMap<String, BaseClass>();

    private DocumentReference contextUser;

    private ExtensionManagerScriptService scriptService;

    @Before
    public void before() throws Exception
    {
        // mock

        this.mockXWiki = mock(XWiki.class);

        this.xwikiBridge.getXWikiContext().setWiki(this.mockXWiki);
        this.xwikiBridge.getXWikiContext().setWikiId("xwiki");
        this.contextUser =
            new DocumentReference(this.xwikiBridge.getXWikiContext().getWikiId(), "XWiki", "ExtensionUser");

        // classes

        BaseClass styleSheetClass = new BaseClass();
        this.classes.put("StyleSheetExtension", styleSheetClass);

        // checking

        this.xwikiBridge.getXWikiContext().setUserReference(this.contextUser);

        ((XWikiStubContextProvider) this.mocker.getInstance(XWikiStubContextProvider.class))
            .initialize(this.xwikiBridge.getXWikiContext());

        // lookup

        this.scriptService = this.mocker.getInstance(ScriptService.class, "extension");
    }

    // tools

    private Job install(String id, String version, String namespace) throws Throwable
    {
        Job job = this.scriptService.install(id, version, namespace);
        if (job == null) {
            throw this.scriptService.getLastError();
        }

        job.join();

        List<LogEvent> errors = job.getStatus().getLog().getLogsFrom(LogLevel.WARN);
        if (!errors.isEmpty()) {
            throw errors.get(0).getThrowable();
        }

        return job;
    }

    private Job uninstall(String id, String namespace) throws Throwable
    {
        Job job = this.scriptService.uninstall(id, namespace);
        if (job == null) {
            throw this.scriptService.getLastError();
        }

        job.join();

        List<LogEvent> errors = job.getStatus().getLog().getLogsFrom(LogLevel.WARN);
        if (!errors.isEmpty()) {
            throw errors.get(0).getThrowable();
        }

        return job;
    }

    // Tests

    // install

    @Test
    public void testInstallOnRoot() throws Throwable
    {
        when(
            this.xwikiBridge.getMockAuthorizationManager().hasAccess(Right.PROGRAM,
                new DocumentReference("xwiki", "XWiki", "ExtensionUser"), null)).thenReturn(true);

        install("extension", "version", null);
    }

    @Test
    public void testInstallOnNamespace() throws Throwable
    {
        when(
            this.xwikiBridge.getMockAuthorizationManager().hasAccess(Right.PROGRAM,
                new DocumentReference("xwiki", "XWiki", "ExtensionUser"), null)).thenReturn(true);

        install("extension", "version", "namespace");
    }

    @Test(expected = InstallException.class)
    public void testInstallOnRootWithoutProgrammingRigths() throws Throwable
    {
        when(
            this.xwikiBridge.getMockAuthorizationManager().hasAccess(Right.PROGRAM,
                new DocumentReference("xwiki", "XWiki", "ExtensionUser"), null)).thenReturn(false);

        install("extension", "version", null);
    }

    @Test(expected = InstallException.class)
    public void testInstallOnNamespaceWithoutProgrammingRigths() throws Throwable
    {
        when(
            this.xwikiBridge.getMockAuthorizationManager().hasAccess(Right.PROGRAM,
                new DocumentReference("xwiki", "XWiki", "ExtensionUser"), null)).thenReturn(false);

        install("extension", "version", "namespace");
    }

    // uninstall

    @Test
    public void testUninstallFromRoot() throws Throwable
    {
        when(
            this.xwikiBridge.getMockAuthorizationManager().hasAccess(Right.PROGRAM,
                new DocumentReference("xwiki", "XWiki", "ExtensionUser"), null)).thenReturn(true);

        uninstall("installedonroot", null);
    }

    @Test
    public void testUninstallOnNamespace() throws Throwable
    {
        when(
            this.xwikiBridge.getMockAuthorizationManager().hasAccess(Right.PROGRAM,
                new DocumentReference("xwiki", "XWiki", "ExtensionUser"), null)).thenReturn(true);

        uninstall("installedonnamespace", "namespace");
    }

    @Test(expected = UninstallException.class)
    public void testUninstallOnRootWithoutProgrammingRigths() throws Throwable
    {
        when(
            this.xwikiBridge.getMockAuthorizationManager().hasAccess(Right.PROGRAM,
                new DocumentReference("xwiki", "XWiki", "ExtensionUser"), null)).thenReturn(false);

        uninstall("installedonroot", null);
    }

    @Test(expected = UninstallException.class)
    public void testUninstallOnNamespaceWithoutProgrammingRigths() throws Throwable
    {
        when(
            this.xwikiBridge.getMockAuthorizationManager().hasAccess(Right.PROGRAM,
                new DocumentReference("xwiki", "XWiki", "ExtensionUser"), null)).thenReturn(false);

        uninstall("installedonnamespace", "namespace");
    }

    @Test
    public void testGet()
    {
        Assert.assertNotNull(this.scriptService.get(CoreExtensionScriptService.ID));
        Assert.assertNotNull(this.scriptService.get(LocalExtensionScriptService.ID));
        Assert.assertNotNull(this.scriptService.get(InstalledExtensionScriptService.ID));
    }
}
