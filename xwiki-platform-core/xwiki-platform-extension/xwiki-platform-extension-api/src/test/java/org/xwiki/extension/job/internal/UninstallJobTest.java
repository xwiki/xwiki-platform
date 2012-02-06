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
import org.xwiki.extension.TestResources;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.ExtensionHandler;
import org.xwiki.extension.test.AbstractExtensionHandlerTest;
import org.xwiki.extension.test.TestExtensionHandler;

public class UninstallJobTest extends AbstractExtensionHandlerTest
{
    private TestResources resources;

    private TestExtensionHandler handler;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // lookup

        this.handler = (TestExtensionHandler) getComponentManager().lookup(ExtensionHandler.class, "type");

        // resources

        this.resources = new TestResources();
        resources.init(this.localExtensionRepository);
    }

    @Test
    public void testUninstall() throws Throwable
    {
        uninstall(TestResources.INSTALLED_ID, null);

        Assert.assertFalse(this.handler.getExtensions().get(null).contains(this.resources.installed));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(TestResources.INSTALLED_ID.getId(), null));

        Assert.assertTrue(this.handler.getExtensions().get(null).contains(this.resources.installedDependency));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(
            TestResources.INSTALLED_DEPENDENCY_ID.getId(), null));
    }

    @Test
    public void testUninstallWithBackwarDepencency() throws Throwable
    {
        uninstall(TestResources.INSTALLED_DEPENDENCY_ID, null);

        Assert.assertFalse(this.handler.getExtensions().get(null).contains(this.resources.installed));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(TestResources.INSTALLED_ID.getId(), null));

        Assert.assertFalse(this.handler.getExtensions().get(null).contains(this.resources.installedDependency));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(TestResources.INSTALLED_DEPENDENCY_ID.getId(),
            null));
    }

    @Test
    public void testUninstallTwice() throws Throwable
    {
        uninstall(TestResources.INSTALLED_ID, null);

        try {
            uninstall(TestResources.INSTALLED_ID, null);
        } catch (UninstallException expected) {
            // expected
        }
    }

    @Test
    public void testUninstallFromNamespace() throws Throwable
    {
        // prepare

        uninstall(TestResources.INSTALLED_DEPENDENCY_ID, null);
        install(TestResources.INSTALLED_ID, "namespace1");
        install(TestResources.INSTALLED_ID, "namespace2");

        // actual test

        uninstall(TestResources.INSTALLED_ID, "namespace1");

        Assert.assertFalse(this.handler.getExtensions().get("namespace1").contains(this.resources.installed));
        Assert
            .assertNull(this.localExtensionRepository.getInstalledExtension(TestResources.INSTALLED_ID.getId(), "namespace1"));

        Assert.assertTrue(this.handler.getExtensions().get("namespace2").contains(this.resources.installed));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(TestResources.INSTALLED_ID.getId(),
            "namespace2"));
    }

    @Test
    public void testUninstallFromNamespaceWithBackwarDepencency() throws Throwable
    {
        // prepare

        uninstall(TestResources.INSTALLED_DEPENDENCY_ID, null);
        install(TestResources.INSTALLED_ID, "namespace1");
        install(TestResources.INSTALLED_ID, "namespace2");

        // actual test

        uninstall(TestResources.INSTALLED_DEPENDENCY_ID, "namespace1");

        Assert.assertFalse(this.handler.getExtensions().get("namespace1").contains(this.resources.installed));
        Assert
            .assertNull(this.localExtensionRepository.getInstalledExtension(TestResources.INSTALLED_ID.getId(), "namespace1"));
        Assert.assertFalse(this.handler.getExtensions().get("namespace1").contains(this.resources.installedDependency));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(TestResources.INSTALLED_DEPENDENCY_ID.getId(),
            "namespace1"));

        Assert.assertTrue(this.handler.getExtensions().get("namespace2").contains(this.resources.installed));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(TestResources.INSTALLED_ID.getId(),
            "namespace2"));
        Assert.assertTrue(this.handler.getExtensions().get("namespace2").contains(this.resources.installed));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(TestResources.INSTALLED_ID.getId(),
            "namespace2"));
    }

    @Test
    public void testUninstallFromAllNamespaces() throws Throwable
    {
        // prepare

        uninstall(TestResources.INSTALLED_DEPENDENCY_ID, null);
        install(TestResources.INSTALLED_ID, "namespace1");
        install(TestResources.INSTALLED_ID, "namespace2");

        // actual test

        uninstall(TestResources.INSTALLED_ID, null);

        Assert.assertFalse(this.handler.getExtensions().get("namespace1").contains(this.resources.installed));
        Assert
            .assertNull(this.localExtensionRepository.getInstalledExtension(TestResources.INSTALLED_ID.getId(), "namespace1"));

        Assert.assertFalse(this.handler.getExtensions().get("namespace2").contains(this.resources.installed));
        Assert
            .assertNull(this.localExtensionRepository.getInstalledExtension(TestResources.INSTALLED_ID.getId(), "namespace2"));
    }
}
