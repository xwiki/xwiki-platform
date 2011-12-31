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
package org.xwiki.extension.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.TestResources;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.test.ConfigurableDefaultCoreExtensionRepository;
import org.xwiki.extension.test.RepositoryUtil;
import org.xwiki.test.AbstractComponentTestCase;

public class DefaultLocalExtensionRepositoryTest extends AbstractComponentTestCase
{
    private LocalExtensionRepository localExtensionRepository;

    private RepositoryUtil repositoryUtil;

    private ExtensionRepositoryManager repositoryManager;

    private ExtensionId remoteExtensionId;

    private TestResources resources;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil =
            new RepositoryUtil(getClass().getSimpleName(), getConfigurationSource(), getComponentManager());
        this.repositoryUtil.setup();

        // lookup

        this.localExtensionRepository = getComponentManager().lookup(LocalExtensionRepository.class);

        this.repositoryManager = getComponentManager().lookup(ExtensionRepositoryManager.class);

        // resources

        this.resources = new TestResources();
        resources.init(this.localExtensionRepository);
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        registerComponent(ConfigurableDefaultCoreExtensionRepository.class);
    }

    @Test
    public void testInit()
    {
        Assert.assertEquals(2, this.localExtensionRepository.countExtensions());
    }

    @Test
    public void testGetLocalExtension()
    {
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension("unexistingextension", null));

        Extension extension =
            this.localExtensionRepository.getInstalledExtension(TestResources.INSTALLED_ID.getId(), null);

        Assert.assertNotNull(extension);
        Assert.assertEquals(TestResources.INSTALLED_ID, extension.getId());
        Assert.assertEquals("type", extension.getType());
        Assert.assertEquals(Arrays.asList(TestResources.INSTALLED_ID.getId() + "-feature"), new ArrayList<String>(
            extension.getFeatures()));
        Assert.assertEquals(TestResources.INSTALLED_DEPENDENCY_ID.getId(), extension.getDependencies().get(0).getId());
        Assert.assertEquals(TestResources.INSTALLED_DEPENDENCY_ID.getVersion(), extension.getDependencies().get(0)
            .getVersionConstraint().getVersion());
    }

    @Test
    public void testResolve() throws ResolveException
    {
        try {
            this.localExtensionRepository.resolve(new ExtensionId("unexistingextension", "version"));

            Assert.fail("Resolve should have failed");
        } catch (ResolveException expected) {
            // expected
        }

        try {
            this.localExtensionRepository.resolve(new ExtensionId(TestResources.INSTALLED_ID.getId(), "wrongversion"));

            Assert.fail("Resolve should have failed");
        } catch (ResolveException expected) {
            // expected
        }

        Extension extension = this.localExtensionRepository.resolve(TestResources.INSTALLED_ID);

        Assert.assertNotNull(extension);
        Assert.assertEquals(TestResources.INSTALLED_ID, extension.getId());
    }

    @Test
    public void testInstallTwice() throws ResolveException, InstallException
    {
        // Change status
        this.localExtensionRepository.installExtension(this.resources.installed, "namespace",
            !this.resources.installed.isDependency());

        // Try to install again with the same status
        try {
            this.localExtensionRepository.installExtension(this.resources.installed, "namespace",
                this.resources.installed.isDependency());
            Assert.fail("Install should have failed");
        } catch (InstallException expected) {
            // expected
        }
    }

    @Test
    public void testStoreExtensionAndInstall() throws ResolveException, LocalExtensionRepositoryException,
        InstallException
    {
        Extension extension = this.repositoryManager.resolve(TestResources.REMOTE_SIMPLE_ID);

        // store

        this.localExtensionRepository.storeExtension(extension);

        LocalExtension localExtension =
            (LocalExtension) this.localExtensionRepository.resolve(TestResources.REMOTE_SIMPLE_ID);

        Assert.assertEquals(TestResources.REMOTE_SIMPLE_ID, localExtension.getId());
        Assert.assertFalse(localExtension.isInstalled());

        // install

        this.localExtensionRepository.installExtension(localExtension, null, false);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(
            TestResources.REMOTE_SIMPLE_ID.getId(), null));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(
            TestResources.REMOTE_SIMPLE_ID.getId(), "namespace"));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(TestResources.REMOTE_SIMPLE_ID.getId()
            + "-feature", null));
    }

    @Test
    public void testUninsatllExtension() throws ResolveException, LocalExtensionRepositoryException,
        UninstallException, InstallException
    {
        // uninstall from root

        this.localExtensionRepository.uninstallExtension(this.resources.installed, null);
        this.localExtensionRepository.uninstallExtension(this.resources.installedDependency, null);

        // uninstall from namespace

        this.localExtensionRepository.installExtension(this.resources.installedDependency, "namespace", false);
        this.localExtensionRepository.installExtension(this.resources.installed, "namespace", false);
        this.localExtensionRepository.uninstallExtension(this.resources.installed, "namespace");
        this.localExtensionRepository.uninstallExtension(this.resources.installedDependency, "namespace");

        // uninstall from namespace with dependency on root

        this.localExtensionRepository.installExtension(this.resources.installedDependency, null, false);
        this.localExtensionRepository.installExtension(this.resources.installed, "namespace", false);
        this.localExtensionRepository.uninstallExtension(this.resources.installed, "namespace");
        this.localExtensionRepository.uninstallExtension(this.resources.installedDependency, null);
    }

    @Test
    public void testBackwardDependenciesAfterUninstall() throws ResolveException, UninstallException
    {
        this.localExtensionRepository.uninstallExtension(this.resources.installed, null);

        Assert.assertEquals(Collections.EMPTY_LIST,
            this.localExtensionRepository.getBackwardDependencies(TestResources.INSTALLED_DEPENDENCY_ID.getId(), null));
    }

    @Test
    public void testBackwardDependenciesWithExtensionAndDepOnRoot() throws ResolveException
    {
        Assert.assertEquals(
            Arrays.asList(this.resources.installed),
            new ArrayList(this.localExtensionRepository.getBackwardDependencies(
                TestResources.INSTALLED_DEPENDENCY_ID.getId(), null)));

        Assert.assertEquals(
            Arrays.asList(),
            new ArrayList(this.localExtensionRepository.getBackwardDependencies(
                TestResources.INSTALLED_DEPENDENCY_ID.getId(), "namespace")));

        Assert.assertEquals(
            Arrays.asList(),
            new ArrayList(this.localExtensionRepository.getBackwardDependencies(TestResources.INSTALLED_ID.getId(),
                null)));

        Map<String, Collection<LocalExtension>> map = new HashMap<String, Collection<LocalExtension>>();
        map.put(null, Arrays.asList(this.resources.installed));

        Assert.assertEquals(map,
            this.localExtensionRepository.getBackwardDependencies(TestResources.INSTALLED_DEPENDENCY_ID));

        Assert.assertEquals(Collections.EMPTY_MAP,
            this.localExtensionRepository.getBackwardDependencies(TestResources.INSTALLED_ID));
    }

    @Test
    public void testBackwardDependenciesWithExtensionOnNamespaceAndDepOnNamespace() throws InstallException,
        ResolveException, UninstallException
    {
        this.localExtensionRepository.uninstallExtension(this.resources.installed, null);

        this.localExtensionRepository.installExtension(this.resources.installed, "namespace", true);

        Assert.assertEquals(Collections.EMPTY_LIST,
            this.localExtensionRepository.getBackwardDependencies(TestResources.INSTALLED_DEPENDENCY_ID.getId(), null));

        Assert.assertEquals(Arrays.asList(this.resources.installed), this.localExtensionRepository
            .getBackwardDependencies(TestResources.INSTALLED_DEPENDENCY_ID.getId(), "namespace"));

        Assert.assertEquals(Collections.EMPTY_LIST,
            this.localExtensionRepository.getBackwardDependencies(TestResources.INSTALLED_ID.getId(), "namespace"));
    }

    @Test
    public void testBackwardDependenciesWithExtensionAndDepOnNamespace() throws InstallException, ResolveException,
        UninstallException
    {
        this.localExtensionRepository.uninstallExtension(this.resources.installed, null);
        this.localExtensionRepository.uninstallExtension(this.resources.installedDependency, null);

        this.localExtensionRepository.installExtension(this.resources.installedDependency, "namespace", true);
        this.localExtensionRepository.installExtension(this.resources.installed, "namespace", true);

        Assert.assertEquals(Arrays.asList(this.resources.installed), this.localExtensionRepository
            .getBackwardDependencies(TestResources.INSTALLED_DEPENDENCY_ID.getId(), "namespace"));

        Assert.assertEquals(Collections.EMPTY_LIST,
            this.localExtensionRepository.getBackwardDependencies(TestResources.INSTALLED_ID.getId(), "namespace"));
    }
}
