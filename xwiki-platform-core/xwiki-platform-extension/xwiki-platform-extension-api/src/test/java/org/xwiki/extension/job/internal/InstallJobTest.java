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
package org.xwiki.extension.job.internal;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.TestResources;
import org.xwiki.extension.handler.ExtensionHandler;
import org.xwiki.extension.test.AbstractExtensionHandlerTest;
import org.xwiki.extension.test.TestExtensionHandler;

public class InstallJobTest extends AbstractExtensionHandlerTest
{
    private TestExtensionHandler handler;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // lookup

        this.handler = (TestExtensionHandler) getComponentManager().lookup(ExtensionHandler.class, "type");
    }

    @Test
    public void testInstallOnRoot() throws Throwable
    {
        install(TestResources.REMOTE_WITHRANDCDEPENDENCIES_ID, null);

        LocalExtension installedExtension =
            this.localExtensionRepository.getInstalledExtension(TestResources.REMOTE_WITHRANDCDEPENDENCIES_ID.getId(),
                null);
        Assert.assertNotNull(installedExtension);
        Assert.assertTrue(this.handler.getExtensions().get(null).contains(installedExtension));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(
            TestResources.REMOTE_WITHRANDCDEPENDENCIES_ID.getId(), "namespace"));

        installedExtension =
            this.localExtensionRepository.getInstalledExtension(TestResources.REMOTE_SIMPLE_ID.getId(), null);
        Assert.assertNotNull(installedExtension);
        Assert.assertTrue(this.handler.getExtensions().get(null).contains(installedExtension));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(
            TestResources.REMOTE_SIMPLE_ID.getId(), "namespace"));
    }

    @Test
    public void testInstallOnNamespace() throws Throwable
    {
        install(TestResources.REMOTE_WITHRANDCDEPENDENCIES_ID, "namespace");

        LocalExtension installedExtension =
            this.localExtensionRepository.getInstalledExtension(TestResources.REMOTE_WITHRANDCDEPENDENCIES_ID.getId(),
                "namespace");
        Assert.assertNotNull(installedExtension);
        Assert.assertTrue(this.handler.getExtensions().get("namespace").contains(installedExtension));

        installedExtension =
            this.localExtensionRepository.getInstalledExtension(TestResources.REMOTE_SIMPLE_ID.getId(), "namespace");
        Assert.assertNotNull(installedExtension);
        Assert.assertTrue(this.handler.getExtensions().get("namespace").contains(installedExtension));
    }
}
