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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.test.MockitoRepositoryUtilsExtension;
import org.xwiki.job.Job;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.util.XWikiStubContextProvider;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@OldcoreTest
@AllComponents
@ExtendWith(MockitoRepositoryUtilsExtension.class)
class ExtensionManagerScriptServiceTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private XWiki mockXWiki;

    private Map<String, BaseClass> classes = new HashMap<>();

    private DocumentReference contextUser;

    private ExtensionManagerScriptService scriptService;

    @BeforeEach
    void before() throws Exception
    {
        // mock

        this.mockXWiki = mock(XWiki.class);

        this.oldcore.mockQueryManager();

        this.oldcore.getXWikiContext().setWiki(this.mockXWiki);
        this.oldcore.getXWikiContext().setWikiId("xwiki");
        this.contextUser = new DocumentReference(
            this.oldcore.getXWikiContext().getWikiId(), "XWiki", "ExtensionUser");

        // classes

        BaseClass styleSheetClass = new BaseClass();
        this.classes.put("StyleSheetExtension", styleSheetClass);

        // checking

        this.oldcore.getXWikiContext().setUserReference(this.contextUser);

        ((XWikiStubContextProvider) this.oldcore.getMocker().getInstance(XWikiStubContextProvider.class))
            .initialize(this.oldcore.getXWikiContext());

        // lookup

        this.scriptService = this.oldcore.getMocker().getInstance(ScriptService.class, "extension");
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
            throw errors.getFirst().getThrowable();
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
            throw errors.getFirst().getThrowable();
        }

        return job;
    }

    // Tests

    // install

    @Test
    void testInstallOnRoot()
    {
        assertDoesNotThrow(() -> install("extension", "version", null));
    }

    @Test
    void testInstallOnNamespace()
    {
        assertDoesNotThrow(() -> install("extension", "version", "namespace"));
    }

    @Test
    void testOverwriteAllowedNamespaces() throws Throwable
    {
        InstallRequest installRequest =
            this.scriptService.createInstallRequest("extension", "version", "namespace");

        // Indicate all extensions of type "test" should be installed on root
        ((ScriptExtensionRewriter) installRequest.getRewriter()).installExtensionTypeOnRootNamespace("test");

        // Allow redirect on root
        installRequest.setRootModificationsAllowed(true);

        Job job = this.scriptService.install(installRequest);
        if (job == null) {
            throw this.scriptService.getLastError();
        }

        job.join();

        List<LogEvent> errors = job.getStatus().getLog().getLogsFrom(LogLevel.WARN);
        if (!errors.isEmpty()) {
            throw errors.getFirst().getThrowable();
        }

        // Validate

        InstalledExtensionRepository repository =
            this.oldcore.getMocker().getInstance(InstalledExtensionRepository.class);

        assertNotNull(repository.getInstalledExtension("extension", null));
    }

    @Test
    void testInstallOnRootWithoutProgrammingRights() throws Throwable
    {
        doThrow(AccessDeniedException.class).when(this.oldcore.getMockAuthorizationManager())
            .checkAccess(Right.PROGRAM, new DocumentReference("xwiki", "XWiki", "ExtensionUser"), null);

        assertThrows(InstallException.class, () -> install("extension", "version", null));
    }

    @Test
    void testInstallOnNamespaceWithoutProgrammingRights() throws Throwable
    {
        doThrow(AccessDeniedException.class).when(this.oldcore.getMockAuthorizationManager())
            .checkAccess(Right.PROGRAM, new DocumentReference("xwiki", "XWiki", "ExtensionUser"), null);

        assertThrows(InstallException.class, () -> install("extension", "version", "namespace"));
    }

    // uninstall

    @Test
    void testUninstallFromRoot()
    {
        assertDoesNotThrow(() -> uninstall("installedonroot", null));
    }

    @Test
    void testUninstallOnNamespace()
    {
        assertDoesNotThrow(() -> uninstall("installedonnamespace", "namespace"));
    }

    @Test
    void testUninstallOnRootWithoutProgrammingRights() throws Throwable
    {
        doThrow(AccessDeniedException.class).when(this.oldcore.getMockAuthorizationManager())
            .checkAccess(Right.PROGRAM, new DocumentReference("xwiki", "XWiki", "ExtensionUser"), null);

        assertThrows(UninstallException.class, () -> uninstall("installedonroot", null));
    }

    @Test
    void testUninstallOnNamespaceWithoutProgrammingRights() throws Throwable
    {
        doThrow(AccessDeniedException.class).when(this.oldcore.getMockAuthorizationManager())
            .checkAccess(Right.PROGRAM, new DocumentReference("xwiki", "XWiki", "ExtensionUser"), null);

        assertThrows(UninstallException.class, () -> uninstall("installedonnamespace", "namespace"));
    }

    @Test
    void testGet()
    {
        assertNotNull(this.scriptService.get(CoreExtensionScriptService.ID));
        assertNotNull(this.scriptService.get(LocalExtensionScriptService.ID));
        assertNotNull(this.scriptService.get(InstalledExtensionScriptService.ID));
    }
}
