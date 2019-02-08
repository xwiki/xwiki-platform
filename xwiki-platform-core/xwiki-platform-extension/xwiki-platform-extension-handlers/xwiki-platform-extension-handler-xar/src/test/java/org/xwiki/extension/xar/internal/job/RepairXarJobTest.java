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

import org.junit.Before;
import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.test.AbstractExtensionHandlerTest;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.job.Job;
import org.xwiki.logging.LogLevel;
import org.xwiki.refactoring.internal.LinkRefactoring;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RepairXarJobTest extends AbstractExtensionHandlerTest
{
    private InstalledExtensionRepository xarExtensionRepository;

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.repositoryUtil.getComponentManager().registerMockComponent(WikiDescriptorManager.class);
    }

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.mocker.registerMockComponent(ContextualAuthorizationManager.class);

        // avoid dependency issue with refactoring listeners
        this.mocker.registerMockComponent(ModelBridge.class);
        this.mocker.registerMockComponent(LinkRefactoring.class);

        this.xarExtensionRepository =
            this.mocker.getInstance(InstalledExtensionRepository.class, XarExtensionHandler.TYPE);
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
