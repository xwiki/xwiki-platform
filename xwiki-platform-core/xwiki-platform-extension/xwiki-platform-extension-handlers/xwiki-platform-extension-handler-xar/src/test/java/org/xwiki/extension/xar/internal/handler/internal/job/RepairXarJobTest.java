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
package org.xwiki.extension.xar.internal.handler.internal.job;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.test.AbstractExtensionHandlerTest;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.job.RepairXarJob;
import org.xwiki.job.Job;
import org.xwiki.logging.LogLevel;

public class RepairXarJobTest extends AbstractExtensionHandlerTest
{
    private InstalledExtensionRepository xarExtensionRepository;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.xarExtensionRepository =
            this.mocker.getInstance(InstalledExtensionRepository.class, XarExtensionHandler.TYPE);
    }

    protected Job repair(ExtensionId extensionId, String[] namespaces, LogLevel failFrom) throws Throwable
    {
        return install(RepairXarJob.JOBTYPE, extensionId, namespaces, failFrom);
    }

    @Test
    public void testRepair() throws Throwable
    {
        ExtensionId extensionId = new ExtensionId("test", "1.0");

        repair(extensionId, null, LogLevel.WARN);

        InstalledExtension installedExtension = this.xarExtensionRepository.resolve(extensionId);

        Assert.assertTrue(installedExtension.isValid(null));

        installedExtension = this.xarExtensionRepository.resolve(new ExtensionId("dependency", "1.0"));

        Assert.assertTrue(installedExtension.isValid(null));
    }

    @Test
    public void testRepairInvalid() throws Throwable
    {
        ExtensionId extensionId = new ExtensionId("invalid", "1.0");

        repair(extensionId, null, LogLevel.ERROR);

        InstalledExtension installedExtension = this.xarExtensionRepository.resolve(extensionId);

        Assert.assertFalse(installedExtension.isValid(null));
    }
}
