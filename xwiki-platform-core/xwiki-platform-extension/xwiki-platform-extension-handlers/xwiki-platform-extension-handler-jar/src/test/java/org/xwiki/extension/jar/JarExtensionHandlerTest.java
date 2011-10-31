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

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.test.AbstractExtensionHandlerTest;
import org.xwiki.model.reference.AttachmentReferenceResolver;

import packagefile.jarextension.TestComponent;

public class JarExtensionHandlerTest extends AbstractExtensionHandlerTest
{
    private ExtensionId testArtifactId;

    private LocalExtensionRepository localExtensionRepository;

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        registerMockComponent(AttachmentReferenceResolver.class, "current");
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // lookup

        this.localExtensionRepository = getComponentManager().lookup(LocalExtensionRepository.class);

        this.testArtifactId = new ExtensionId("org.xwiki.test:test-extension", "test");
    }

    @Test
    public void testInstallAndUninstallExtension() throws Throwable
    {
        // /////////////////////////////////////////////////////////////////////
        // install
        // /////////////////////////////////////////////////////////////////////
        // actual test
        LocalExtension localExtension = install(this.testArtifactId);

        Assert.assertNotNull(localExtension);
        Assert.assertNotNull(localExtension.getFile());
        Assert.assertTrue(localExtension.getFile().exists());
        Assert.assertTrue(localExtension.isInstalled(null));

        getComponentManager().lookup(TestComponent.class);

        // lookup registered component

        try {
            install(this.testArtifactId);
            Assert.fail("installExtension should have failed");
        } catch (InstallException expected) {
            // expected
        }

        getComponentManager().lookup(ExecutionContextInitializer.class, "jarextension").initialize(null);

        Assert.assertNotNull(this.localExtensionRepository.getInstalledExtension("feature", null));

        // /////////////////////////////////////////////////////////////////////
        // uninstall
        // /////////////////////////////////////////////////////////////////////

        localExtension = uninstall(this.testArtifactId);

        Assert.assertFalse(localExtension.isInstalled(null));
        Assert.assertNull(this.localExtensionRepository.getInstalledExtension(this.testArtifactId.getId(), null));

        try {
            getComponentManager().lookup(TestComponent.class);
            Assert.fail("the extension has not been uninstalled");
        } catch (ComponentLookupException expected) {
            // expected
        }

        // /////////////////////////////////////////////////////////////////////
        // install extension that has just been uninstalled
        // /////////////////////////////////////////////////////////////////////

        localExtension = install(this.testArtifactId);

        Assert.assertTrue(localExtension.isInstalled(null));
    }
}
