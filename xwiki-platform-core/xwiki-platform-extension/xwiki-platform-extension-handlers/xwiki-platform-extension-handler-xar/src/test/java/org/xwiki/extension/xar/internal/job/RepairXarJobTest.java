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
package org.xwiki.extension.xar.internal.job;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.test.AbstractExtensionHandlerTest;
import org.xwiki.extension.test.MockitoRepositoryUtilsExtension;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.job.Job;
import org.xwiki.logging.LogLevel;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.internal.ReferenceUpdater;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@OldcoreTest
@ExtendWith(MockitoRepositoryUtilsExtension.class)
public class RepairXarJobTest extends AbstractExtensionHandlerTest
{
    private InstalledExtensionRepository xarExtensionRepository;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @AfterComponent
    @Override
    public void afterComponent() throws Exception
    {
        this.componentManager.registerMockComponent(WikiDescriptorManager.class);
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception
    {
        super.setUp();

        this.componentManager.registerMockComponent(ContextualAuthorizationManager.class);

        // Some listeners (e.g. InstalledExtensionDocumentListener) use the old core API.
        Provider<XWikiContext> xcontextProvider =
            this.componentManager.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.oldcore.getXWikiContext());

        // avoid dependency issue with refactoring listeners
        this.componentManager.registerMockComponent(ModelBridge.class);
        this.componentManager.registerMockComponent(ReferenceUpdater.class);

        this.xarExtensionRepository =
            this.componentManager.getInstance(InstalledExtensionRepository.class, XarExtensionHandler.TYPE);
    }

    protected Job repair(ExtensionId extensionId, String[] namespaces, LogLevel failFrom) throws Throwable
    {
        return install(RepairXarJob.JOBTYPE, extensionId, namespaces, failFrom);
    }

    @Test
    public void testRepairOnRoot() throws Throwable
    {
        ExtensionId extensionId = new ExtensionId("test", "1.0");

        repair(extensionId, null, LogLevel.WARN);

        InstalledExtension installedExtension = this.xarExtensionRepository.resolve(extensionId);

        assertTrue(installedExtension.isValid(null));

        installedExtension = this.xarExtensionRepository.resolve(new ExtensionId("dependency", "1.0"));

        assertTrue(installedExtension.isValid(null));
    }

    @Test
    public void testRepairOnWiki() throws Throwable
    {
        ExtensionId extensionId = new ExtensionId("test", "1.0");

        repair(extensionId, new String[] { "wiki:wiki1" }, LogLevel.WARN);

        InstalledExtension installedExtension = this.xarExtensionRepository.resolve(extensionId);

        assertTrue(installedExtension.isValid(null));

        installedExtension = this.xarExtensionRepository.resolve(new ExtensionId("dependency", "1.0"));

        assertTrue(installedExtension.isValid(null));
    }

    @Test
    public void testRepairInvalidOnRoot() throws Throwable
    {
        ExtensionId extensionId = new ExtensionId("invalid", "1.0");

        repair(extensionId, null, LogLevel.ERROR);

        InstalledExtension installedExtension = this.xarExtensionRepository.resolve(extensionId);

        assertFalse(installedExtension.isValid(null));
    }
}
