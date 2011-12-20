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
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.ExtensionHandler;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.test.AbstractExtensionHandlerTest;
import org.xwiki.extension.test.ConfigurableDefaultCoreExtensionRepository;
import org.xwiki.extension.test.TestExtensionHandler;

public class UninstallJobTest extends AbstractExtensionHandlerTest
{
    private ExtensionId existingExtensionId;

    private ExtensionId existingExtensionDependencyId;

    private LocalExtension existingExtension;

    private LocalExtension existingExtensionDependency;

    private TestExtensionHandler handler;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // lookup

        this.handler = (TestExtensionHandler) getComponentManager().lookup(ExtensionHandler.class, "type");

        // resources

        this.existingExtensionId = new ExtensionId("existingextension", "version");
        this.existingExtensionDependencyId = new ExtensionId("existingextensiondependency", "version");

        this.existingExtension = (LocalExtension) this.localExtensionRepository.resolve(this.existingExtensionId);
        this.existingExtensionDependency =
            (LocalExtension) this.localExtensionRepository.resolve(this.existingExtensionDependencyId);
    }

    @Test
    public void testUninstall() throws Throwable
    {
        uninstall(this.existingExtensionId);

        Assert.assertFalse(this.handler.getExtensions().get(null).contains(this.existingExtension));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(this.existingExtensionId.getId(), null));

        Assert.assertTrue(this.handler.getExtensions().get(null).contains(this.existingExtensionDependency));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(
            this.existingExtensionDependencyId.getId(), null));
    }

    @Test
    public void testUninstallWithBackwarDepencency() throws Throwable
    {
        uninstall(this.existingExtensionDependencyId);

        Assert.assertFalse(this.handler.getExtensions().get(null).contains(this.existingExtension));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(this.existingExtensionId.getId(), null));

        Assert.assertFalse(this.handler.getExtensions().get(null).contains(this.existingExtensionDependency));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(
            this.existingExtensionDependencyId.getId(), null));
    }

    @Test
    public void testUninstallTwice() throws Throwable
    {
        uninstall(this.existingExtensionId);

        try {
            uninstall(this.existingExtensionId);
        } catch (UninstallException expected) {
            // expected
        }
    }

    @Test
    public void testUninstallFromNamespace() throws Throwable
    {
        // prepare

        uninstall(this.existingExtensionDependencyId);
        install(this.existingExtensionId, "namespace1");
        install(this.existingExtensionId, "namespace2");

        // actual test

        uninstall(this.existingExtensionId, "namespace1");

        Assert.assertFalse(this.handler.getExtensions().get("namespace1").contains(this.existingExtension));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(this.existingExtensionId.getId(),
            "namespace1"));

        Assert.assertTrue(this.handler.getExtensions().get("namespace2").contains(this.existingExtension));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(this.existingExtensionId.getId(),
            "namespace2"));
    }

    @Test
    public void testUninstallFromNamespaceWithBackwarDepencency() throws Throwable
    {
        // prepare

        uninstall(this.existingExtensionDependencyId);
        install(this.existingExtensionId, "namespace1");
        install(this.existingExtensionId, "namespace2");

        // actual test

        uninstall(this.existingExtensionDependencyId, "namespace1");

        Assert.assertFalse(this.handler.getExtensions().get("namespace1").contains(this.existingExtension));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(this.existingExtensionId.getId(),
            "namespace1"));
        Assert.assertFalse(this.handler.getExtensions().get("namespace1").contains(this.existingExtensionDependency));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(this.existingExtensionDependencyId.getId(),
            "namespace1"));

        Assert.assertTrue(this.handler.getExtensions().get("namespace2").contains(this.existingExtension));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(this.existingExtensionId.getId(),
            "namespace2"));
        Assert.assertTrue(this.handler.getExtensions().get("namespace2").contains(this.existingExtension));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(this.existingExtensionId.getId(),
            "namespace2"));
    }

    @Test
    public void testUninstallFromAllNamespaces() throws Throwable
    {
        // prepare

        uninstall(this.existingExtensionDependencyId);
        install(this.existingExtensionId, "namespace1");
        install(this.existingExtensionId, "namespace2");

        // actual test

        uninstall(this.existingExtensionId);

        Assert.assertFalse(this.handler.getExtensions().get("namespace1").contains(this.existingExtension));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(this.existingExtensionId.getId(),
            "namespace1"));

        Assert.assertFalse(this.handler.getExtensions().get("namespace2").contains(this.existingExtension));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(this.existingExtensionId.getId(),
            "namespace2"));
    }
}
