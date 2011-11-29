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
package org.xwiki.extension.jar;

import java.io.File;
import java.util.HashMap;

import org.junit.Test;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.jar.internal.handler.JarExtensionClassLoader;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.test.AbstractExtensionHandlerTest;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.WikiReference;

import junit.framework.Assert;
import packagefile.jarextension.DefaultTestComponent;
import packagefile.jarextension.TestComponent;
import packagefile.jarextensionwithdeps.DefaultTestComponentWithDeps;
import packagefile.jarextensionwithdeps.TestComponentWithDeps;

public class JarExtensionHandlerTest extends AbstractExtensionHandlerTest
{
    private LocalExtensionRepository localExtensionRepository;
    
    private ComponentManagerManager componentManagerManager;

    private ClassLoader testApplicationClassloader;

    private JarExtensionClassLoader jarExtensionClassLoader;

    private Execution execution;

    private ModelContext modelContext;

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // Override the system ClassLoader to isolate class loading of extensions from the current ClassLoader
        // (which already contains the extensions)
        registerComponent(TestJarExtensionClassLoader.class);
        registerMockComponent(AttachmentReferenceResolver.class, "current");
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.testApplicationClassloader = Thread.currentThread().getContextClassLoader();

        // lookup
        this.localExtensionRepository = getComponentManager().lookup(LocalExtensionRepository.class);
        this.componentManagerManager = getComponentManager().lookup(ComponentManagerManager.class);
        this.jarExtensionClassLoader = getComponentManager().lookup(JarExtensionClassLoader.class);
        this.modelContext = getComponentManager().lookup(ModelContext.class);
        this.execution = getComponentManager().lookup(Execution.class);
    }

    /**
     * @return the root extension class loader
     */
    private ClassLoader getExtensionClassloader() {
        return getExtensionClassloader(null);
    }

    /**
     * @param namespace the namespace to be used
     * @return the extension class loader for the current namespace
     */
    private ClassLoader getExtensionClassloader(String namespace) {
        ClassLoader extensionLoader = this.jarExtensionClassLoader.getURLClassLoader(namespace,false);
        if (extensionLoader == null) {
            extensionLoader = ((TestJarExtensionClassLoader)this.jarExtensionClassLoader).getSystemClassLoader();
        }
        return extensionLoader;
    }

    /**
     * Check that the extension is properly reported to be installed in all namespaces.
     *
     * @param localExtension the local extension to check
     */
    private void checkInstallStatus(LocalExtension localExtension)
    {
        checkInstallStatus(localExtension, null);
    }

    /**
     * Check that the extension is properly reported to be installed in the given namespace.
     *
     * @param localExtension the local extension to check
     * @param namespace the namespace where it has been installed
     */
    private void checkInstallStatus(LocalExtension localExtension, String namespace)
    {
        // check extension status
        Assert.assertNotNull(localExtension);
        Assert.assertNotNull(localExtension.getFile());
        Assert.assertTrue(new File(localExtension.getFile().getAbsolutePath()).exists());
        Assert.assertTrue(localExtension.isInstalled(namespace));
        if (namespace != null) {
            Assert.assertFalse(localExtension.isInstalled(null));
        }

        // check repository status
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension(localExtension.getId().getId(),
            namespace));
        if (namespace != null) {
            Assert.assertNull(this.localExtensionRepository.getInstalledExtension(localExtension.getId().getId(),
                null));
        }
    }

    /**
     * Check that an extension is effectively available in all namespace and that the global component
     * manager provide the expected default implementation.
     *
     * @param role the role expected to be provided by the extension
     * @param implementation the implementation expected for the given role
     * @param <T> the role class
     * @return the effective role class in the extension class loader
     * @throws Exception on error
     */
    private <T> Class<?> checkJarExtensionAvailability(Class<T> role, Class<? extends T> implementation)
        throws Exception
    {
        return checkJarExtensionAvailability(role, implementation, null);
    }

    /**
     * Check that an extension is effectively available in the given namespace and that the corresponding component
     * manager provide the expected default implementation.
     *
     * @param role the role expected to be provided by the extension
     * @param implementation the implementation expected for the given role
     * @param namespace the namespace where the extension is expected to installed
     * @param <T> the role class
     * @return the effective role class in the extension class loader
     * @throws Exception on error
     */
    private <T> Class<?> checkJarExtensionAvailability(Class<T> role, Class<? extends T> implementation,
        String namespace)
        throws Exception
    {
        ClassLoader extensionLoader = getExtensionClassloader(namespace);
        Assert.assertNotNull(extensionLoader);
        Assert.assertNotSame(this.testApplicationClassloader, extensionLoader);

        Class<?> loadedRole =  Class.forName(role.getName(), true, extensionLoader);
        // Ensure the loaded role does not came from the application classloader (a check to validate the test)
        Assert.assertNotSame(role, loadedRole);

        if (namespace != null) {
            try {
                this.jarExtensionClassLoader.getURLClassLoader(null,false).loadClass(role.getName());
                Assert.fail("the interface should not be in the root class loader");
            } catch(ClassNotFoundException expected) {
                //expected
            }
        }

        // check components managers
        Class loadedClass = null;
        if (namespace != null) {
            loadedClass = this.componentManagerManager.getComponentManager(namespace, false)
                .lookup(loadedRole).getClass();

            try {
                getComponentManager().lookup(loadedRole);
                Assert.fail("the component should not be in the root component manager");
            } catch (ComponentLookupException expected) {
                // expected
            }
        } else {
            loadedClass = getComponentManager().lookup(loadedRole).getClass();
        }
        Assert.assertEquals(implementation.getName(),loadedClass.getName());
        Assert.assertNotSame(implementation,loadedClass);
        return loadedRole;
    }

    /**
     * Check that the extension is properly reported to be not installed in all namespace.
     *
     * @param localExtension the local extension to check
     */
    private void ckeckUninstallStatus(LocalExtension localExtension)
    {
        ckeckUninstallStatus(localExtension, null);
    }

    /**
     * Check that the extension is properly reported to be not installed in the given namespace.
     *
     * @param localExtension the local extension to check
     * @param namespace the namespace where it should not be installed
     */
    private void ckeckUninstallStatus(LocalExtension localExtension, String namespace)
    {
        // check extension status
        Assert.assertFalse(localExtension.isInstalled(namespace));
        Assert.assertFalse(localExtension.isInstalled(null));

        // check repository status
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(localExtension.getId().getId(),
            namespace));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(localExtension.getId().getId(),
            null));
    }

    /**
     * Check that an extension is effectively not available in all namespace and that the corresponding component
     * manager does not provide an implementation.
     *
     * @param role the role expected to not be provide
     * @throws Exception on error
     */
    private void checkJarExtensionUnavailability(Class<?> role) throws Exception
    {
        checkJarExtensionUnavailability(role, null);
    }

    /**
     * Check that an extension is effectively not available in the given namespace and that the corresponding component
     * manager does not provide an implementation.
     *
     * @param role the role expected to not be provide
     * @param namespace the namespace where the extension is not expected to be installed
     * @throws Exception on error
     */
    private void checkJarExtensionUnavailability(Class<?> role, String namespace) throws Exception
    {
        ClassLoader extensionLoader = getExtensionClassloader(namespace);

        try {
            Class<?> loadedRole = Class.forName(role.getName(), true, extensionLoader);
            // check components managers
            getComponentManager().lookup(loadedRole);
            Assert.fail("the extension has not been uninstalled, component found!");
        } catch (ComponentLookupException unexpected) {
            Assert.fail("the extension has not been uninstalled, role found!");
        } catch (ClassNotFoundException expected) {
            // expected
        }
    }

    /**
     * Execute the jar handler ExecutionContextInitializer and check it set the given extension class loader.
     *
     * @param extensionLoader the extension class loader expected to be set in the thread context
     * @throws Throwable on error
     */
    private void checkJarHandlerContextInitializer(ClassLoader extensionLoader) throws Throwable
    {
        checkJarHandlerContextInitializer(extensionLoader, null);
    }

    /**
     * Execute the jar handler ExecutionContextInitializer and check it set the given extension class loader.
     *
     * @param extensionLoader the extension class loader expected to be set in the thread context
     * @param namespace the namespace that should be installed in the simulated context
     * @throws Throwable on error
     */
    private void checkJarHandlerContextInitializer(ClassLoader extensionLoader, String namespace) throws Throwable
    {
        // reset to application class loader since initializer is normally used on startup
        Thread.currentThread().setContextClassLoader(this.testApplicationClassloader);

        // execute initializer in context
        if (namespace != null) {
            // Simulate the context for the initializer
            this.execution.getContext().setProperty("xwikicontext", new HashMap());
            this.modelContext.setCurrentEntityReference(new WikiReference(namespace));
            getComponentManager().lookup(ExecutionContextInitializer.class, "jarextension").initialize(null);
            // Drop simulated context
            this.execution.getContext().removeProperty("xwikicontext");
        } else {
            getComponentManager().lookup(ExecutionContextInitializer.class, "jarextension").initialize(null);
        }

        // check switch has been properly done
        if (extensionLoader != null) {
            Assert.assertSame(extensionLoader, Thread.currentThread().getContextClassLoader());
            Assert.assertNotSame(this.testApplicationClassloader, Thread.currentThread().getContextClassLoader());
        } else {
            Assert.assertSame(this.testApplicationClassloader, Thread.currentThread().getContextClassLoader());
        }

        Thread.currentThread().setContextClassLoader(this.testApplicationClassloader);
    }

    @Test
    public void testInstallAndUninstallExtension() throws Throwable
    {
        final ExtensionId extensionId = new ExtensionId("org.xwiki.test:test-extension", "test");

        // actual install test
        LocalExtension localExtension = install(extensionId);

        checkInstallStatus(localExtension);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature", null));

        Class<?> extensionRole1 = checkJarExtensionAvailability(TestComponent.class, DefaultTestComponent.class);

        checkJarHandlerContextInitializer(getExtensionClassloader());

        // try to install again
        try {
            install(extensionId);
            Assert.fail("installExtension should have failed");
        } catch (InstallException expected) {
            // expected
        }

        // actual uninstall test
        localExtension = uninstall(extensionId);

        ckeckUninstallStatus(localExtension);

        checkJarExtensionUnavailability(TestComponent.class);

        // actual reinstall test
        localExtension = install(extensionId);

        checkInstallStatus(localExtension);

        Class<?> extensionRole2 = checkJarExtensionAvailability(TestComponent.class, DefaultTestComponent.class);
        Assert.assertNotSame(extensionRole1, extensionRole2);
    }

    @Test
    public void testInstallAndUninstallExtensionOnANamespace() throws Throwable
    {
        final ExtensionId extensionId = new ExtensionId("org.xwiki.test:test-extension", "test");
        final String namespace = "namespace";
        
        // actual install test
        LocalExtension localExtension = install(extensionId, namespace);

        checkInstallStatus(localExtension, namespace);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature", namespace));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension("feature", null));

        checkJarExtensionAvailability(TestComponent.class, DefaultTestComponent.class, namespace);

        checkJarHandlerContextInitializer(getExtensionClassloader(namespace), namespace);

        try {
            install(extensionId, namespace);
            Assert.fail("installExtension should have failed");
        } catch (InstallException expected) {
            // expected
        }

        // actual uninstall test
        localExtension = uninstall(extensionId);

        ckeckUninstallStatus(localExtension, namespace);

        checkJarExtensionUnavailability(TestComponent.class, namespace);
    }

    @Test
    public void testInstallAndUninstallExtensionWithDependency() throws Throwable
    {
        final ExtensionId extensionId = new ExtensionId("org.xwiki.test:test-extension-with-deps", "test");

        // actual install test
        LocalExtension localExtension = install(extensionId);

        checkInstallStatus(localExtension);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", null));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature", null));

        Class<?> extensionRole1 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class);
        Class<?> extensionDep1 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);

        // actual uninstall test
        localExtension = uninstall(extensionId);

        ckeckUninstallStatus(localExtension);

        checkJarExtensionUnavailability(TestComponentWithDeps.class);
        Class<?> extensionDep2 = checkJarExtensionAvailability(TestComponent.class, DefaultTestComponent.class);
        Assert.assertNotSame(extensionDep1, extensionDep2);

        // actual reinstall test
        localExtension = install(extensionId);

        checkInstallStatus(localExtension);

        Class<?> extensionRole2 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class);
        Class<?> extensionDep3 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);
        Assert.assertNotSame(extensionRole1, extensionRole2);
        Assert.assertSame(extensionDep2, extensionDep3);
    }

    @Test
    public void testInstallAndUninstallExtensionWithDependencyOnANamespace() throws Throwable
    {
        final ExtensionId extensionId = new ExtensionId("org.xwiki.test:test-extension-with-deps", "test");
        final String namespace = "namespace";

        // actual install test
        LocalExtension localExtension = install(extensionId, namespace);

        checkInstallStatus(localExtension, namespace);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", namespace));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", null));

        checkJarExtensionAvailability(TestComponentWithDeps.class, DefaultTestComponentWithDeps.class, namespace);
        checkJarExtensionAvailability(TestComponent.class, DefaultTestComponent.class, namespace);

        // actual uninstall test
        localExtension = uninstall(extensionId);

        ckeckUninstallStatus(localExtension, namespace);

        checkJarExtensionUnavailability(TestComponentWithDeps.class, namespace);
        checkJarExtensionAvailability(TestComponent.class, DefaultTestComponent.class, namespace);
    }

    @Test
    public void testInstallEntensionAndUninstallDependency() throws Throwable
    {
        final ExtensionId extensionId = new ExtensionId("org.xwiki.test:test-extension-with-deps", "test");
        final ExtensionId dependencyId = new ExtensionId("org.xwiki.test:test-extension", "test");

        // actual install test
        LocalExtension localExtension = install(extensionId);

        checkInstallStatus(localExtension);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", null));
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature", null));

        Class<?> extensionRole1 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class);
        Class<?> extensionDep1 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);

        // actual uninstall test
        localExtension = uninstall(dependencyId);

        ckeckUninstallStatus(localExtension);

        checkJarExtensionUnavailability(TestComponent.class);
        checkJarExtensionUnavailability(TestComponentWithDeps.class);

        // actual reinstall test
        localExtension = install(extensionId);

        checkInstallStatus(localExtension);

        Class<?> extensionRole2 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class);
        Class<?> extensionDep2 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);
        Assert.assertNotSame(extensionRole1, extensionRole2);
        Assert.assertNotSame(extensionDep1, extensionDep2);
    }

    @Test
    public void testInstallExtensionAndUninstallDependencyOnANamespace() throws Throwable
    {
        final ExtensionId extensionId = new ExtensionId("org.xwiki.test:test-extension-with-deps", "test");
        final ExtensionId dependencyId = new ExtensionId("org.xwiki.test:test-extension", "test");
        final String namespace = "namespace";

        // actual install test
        LocalExtension localExtension = install(extensionId, namespace);

        checkInstallStatus(localExtension, namespace);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", namespace));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", null));

        checkJarExtensionAvailability(TestComponentWithDeps.class, DefaultTestComponentWithDeps.class, namespace);
        checkJarExtensionAvailability(TestComponent.class, DefaultTestComponent.class, namespace);

        // actual uninstall test
        localExtension = uninstall(dependencyId);

        ckeckUninstallStatus(localExtension, namespace);

        checkJarExtensionUnavailability(TestComponentWithDeps.class, namespace);
        checkJarExtensionUnavailability(TestComponentWithDeps.class);
    }

    @Test
    public void testInstallDependencyInstallExtensionOnANamespaceAndUninstallExtension() throws Throwable
    {
        final ExtensionId extensionId = new ExtensionId("org.xwiki.test:test-extension-with-deps", "test");
        final ExtensionId dependencyId = new ExtensionId("org.xwiki.test:test-extension", "test");
        final String namespace = "namespace";

        // actual install test
        LocalExtension localExtension = install(dependencyId);

        checkInstallStatus(localExtension);
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature", null));
        Class<?> extensionDep1 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);

        // actual install test
        localExtension = install(extensionId, namespace);

        checkInstallStatus(localExtension, namespace);
        checkInstallStatus((LocalExtension)this.localExtensionRepository.resolve(dependencyId));

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", namespace));
        Class<?> extensionRole1 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class, namespace);
        Class<?> extensionDep2 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);
         Assert.assertSame(extensionDep1, extensionDep2);

        // actual uninstall test
        localExtension = uninstall(extensionId, namespace);

        ckeckUninstallStatus(localExtension, namespace);
        checkInstallStatus((LocalExtension)this.localExtensionRepository.resolve(dependencyId));

        checkJarExtensionUnavailability(TestComponentWithDeps.class, namespace);
        Class<?> extensionDep3 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);
        Assert.assertSame(extensionDep1, extensionDep3);

        // actual reinstall test
        localExtension = install(extensionId, namespace);

        checkInstallStatus(localExtension, namespace);

        Class<?> extensionRole2 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class, namespace);
        Class<?> extensionDep4 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);
        Assert.assertNotSame(extensionRole1, extensionRole2);
        Assert.assertSame(extensionDep1, extensionDep4);
    }

    @Test
    public void testInstallDependencyInstallExtensionOnANamespaceAndUninstallDependency() throws Throwable
    {
        final ExtensionId extensionId = new ExtensionId("org.xwiki.test:test-extension-with-deps", "test");
        final ExtensionId dependencyId = new ExtensionId("org.xwiki.test:test-extension", "test");
        final String namespace = "namespace";

        // actual install test
        LocalExtension localExtension = install(dependencyId);

        checkInstallStatus(localExtension);
        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature", null));
        Class<?> extensionDep1 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);

        // actual install test
        localExtension = install(extensionId, namespace);

        checkInstallStatus(localExtension, namespace);
        checkInstallStatus((LocalExtension)this.localExtensionRepository.resolve(dependencyId));

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", namespace));
        Class<?> extensionRole1 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class, namespace);
        Class<?> extensionDep2 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);
         Assert.assertSame(extensionDep1, extensionDep2);

        // actual uninstall test
        localExtension = uninstall(dependencyId);

        ckeckUninstallStatus(localExtension);
        ckeckUninstallStatus((LocalExtension)this.localExtensionRepository.resolve(extensionId), namespace);

        checkJarExtensionUnavailability(TestComponent.class);
        checkJarExtensionUnavailability(TestComponentWithDeps.class, namespace);

        // actual reinstall test
        localExtension = install(extensionId, namespace);

        checkInstallStatus(localExtension, namespace);
        checkInstallStatus((LocalExtension)this.localExtensionRepository.resolve(dependencyId), namespace);

        Class<?> extensionRole2 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class, namespace);
        Class<?> extensionDep3 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class, namespace);
        Assert.assertNotSame(extensionRole1, extensionRole2);
        Assert.assertNotSame(extensionDep1, extensionDep3);
    }

    @Test
    public void testMultipleInstallOnANamespaceAndUninstall() throws Throwable
    {
        final ExtensionId extensionId = new ExtensionId("org.xwiki.test:test-extension-with-deps", "test");
        final String namespace1 = "namespace1";
        final String namespace2 = "namespace2";

        // actual install test
        LocalExtension localExtension = install(extensionId, namespace1);

        checkInstallStatus(localExtension, namespace1);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", namespace1));
        Class<?> extensionRole1 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class, namespace1);
        Class<?> extensionDep1 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class, namespace1);

        // actual install test
        localExtension = install(extensionId, namespace2);

        checkInstallStatus(localExtension, namespace2);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", namespace2));
        Class<?> extensionRole2 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class, namespace2);
        Class<?> extensionDep2 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class, namespace2);
        Assert.assertNotSame(extensionRole1, extensionRole2);
        Assert.assertNotSame(extensionDep1, extensionDep2);

        // actual uninstall test
        localExtension = uninstall(extensionId, namespace1);

        ckeckUninstallStatus(localExtension);

        checkJarExtensionUnavailability(TestComponentWithDeps.class, namespace1);
        Class<?> extensionDep3 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class, namespace1);
        Assert.assertNotSame(extensionDep1, extensionDep3);

        Class<?> extensionRole3 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class, namespace2);
        Class<?> extensionDep4 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class, namespace2);
        Assert.assertSame(extensionRole2, extensionRole3);
        Assert.assertSame(extensionDep2, extensionDep4);

        // actual uninstall test
        localExtension = uninstall(extensionId, namespace2);

        ckeckUninstallStatus(localExtension);

        checkJarExtensionUnavailability(TestComponentWithDeps.class, namespace2);
        Class<?> extensionDep5 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class, namespace2);
        Assert.assertNotSame(extensionDep2, extensionDep5);

        Class<?> extensionDep6 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class, namespace1);
        Assert.assertSame(extensionDep3, extensionDep6);
    }

    @Test
    public void testMultipleInstallOnANamespaceWithGlobalDependencyAndUninstall() throws Throwable
    {
        final ExtensionId extensionId = new ExtensionId("org.xwiki.test:test-extension-with-deps", "test");
        final ExtensionId dependencyId = new ExtensionId("org.xwiki.test:test-extension", "test");
        final String namespace1 = "namespace1";
        final String namespace2 = "namespace2";

        // install global deps
        LocalExtension localExtension = install(dependencyId);

        checkInstallStatus(localExtension);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature", null));
        Class<?> extensionDep1 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);

        // actual install test
        localExtension = install(extensionId, namespace1);

        checkInstallStatus(localExtension, namespace1);
        checkInstallStatus((LocalExtension)this.localExtensionRepository.resolve(dependencyId));

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", namespace1));
        Class<?> extensionRole1 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class, namespace1);
        Class<?> extensionDep2 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);
        Assert.assertSame(extensionDep1, extensionDep2);

        // actual install test
        localExtension = install(extensionId, namespace2);

        checkInstallStatus(localExtension, namespace2);
        checkInstallStatus((LocalExtension)this.localExtensionRepository.resolve(dependencyId));

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", namespace2));
        Class<?> extensionRole2 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class, namespace2);
        Class<?> extensionDep3 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);
        Assert.assertNotSame(extensionRole1, extensionRole2);
        Assert.assertSame(extensionDep1, extensionDep3);

        // actual uninstall test
        localExtension = uninstall(extensionId, namespace1);

        ckeckUninstallStatus(localExtension);
        checkInstallStatus((LocalExtension)this.localExtensionRepository.resolve(dependencyId));

        checkJarExtensionUnavailability(TestComponentWithDeps.class, namespace1);

        Class<?> extensionRole3 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class, namespace2);
        Class<?> extensionDep4 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);
        Assert.assertSame(extensionRole2, extensionRole3);
        Assert.assertSame(extensionDep1, extensionDep4);

        // actual uninstall test
        localExtension = uninstall(extensionId, namespace2);

        ckeckUninstallStatus(localExtension);
        checkInstallStatus((LocalExtension)this.localExtensionRepository.resolve(dependencyId));

        checkJarExtensionUnavailability(TestComponentWithDeps.class, namespace2);
        Class<?> extensionDep5 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);
        Assert.assertSame(extensionDep1, extensionDep5);
    }

    @Test
    public void testMultipleInstallOnANamespaceWithGlobalDependencyAndUninstallDependency() throws Throwable
    {
        final ExtensionId extensionId = new ExtensionId("org.xwiki.test:test-extension-with-deps", "test");
        final ExtensionId dependencyId = new ExtensionId("org.xwiki.test:test-extension", "test");
        final String namespace1 = "namespace1";
        final String namespace2 = "namespace2";

        // install global deps
        LocalExtension localExtension = install(dependencyId);

        checkInstallStatus(localExtension);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature", null));
        Class<?> extensionDep1 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);

        localExtension = install(extensionId, namespace1);

        checkInstallStatus(localExtension, namespace1);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", namespace1));
        Class<?> extensionRole1 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class, namespace1);
        Class<?> extensionDep2 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);
        Assert.assertSame(extensionDep1, extensionDep2);

        localExtension = install(extensionId, namespace2);

        checkInstallStatus(localExtension, namespace2);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature-with-deps", namespace2));
        Class<?> extensionRole2 = checkJarExtensionAvailability(TestComponentWithDeps.class,
            DefaultTestComponentWithDeps.class, namespace2);
        Class<?> extensionDep3 = checkJarExtensionAvailability(TestComponent.class,
            DefaultTestComponent.class);
        Assert.assertNotSame(extensionRole1, extensionRole2);
        Assert.assertSame(extensionDep1, extensionDep3);

        // actual uninstall test
        localExtension = uninstall(dependencyId);

        ckeckUninstallStatus(localExtension);
        ckeckUninstallStatus((LocalExtension)this.localExtensionRepository.resolve(extensionId), namespace1);
        ckeckUninstallStatus((LocalExtension)this.localExtensionRepository.resolve(extensionId), namespace2);

        checkJarExtensionUnavailability(TestComponent.class);
        checkJarExtensionUnavailability(TestComponentWithDeps.class, namespace1);
        checkJarExtensionUnavailability(TestComponentWithDeps.class, namespace2);
    }
}
