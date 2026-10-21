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
package org.xwiki.extension.xar.internal.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.bridge.event.WikiCopiedEvent;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.internal.local.DefaultLocalExtensionRepository;
import org.xwiki.extension.test.EmptyExtension;
import org.xwiki.extension.test.MockitoRepositoryUtilsExtension;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.observation.ObservationManager;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.internal.ReferenceUpdater;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ComponentTest
@AllComponents
@ExtendWith(MockitoRepositoryUtilsExtension.class)
class WikiEventListenerTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private DefaultLocalExtensionRepository localExtensionRepository;

    private InstalledExtensionRepository installedExtensionRepository;

    private ObservationManager observation;

    private LocalExtension localExtension1;

    private LocalExtension localExtensionDependency1;

    @AfterComponent
    void afterComponent() throws Exception
    {
        this.componentManager.registerMockComponent(WikiDescriptorManager.class);
    }

    @AfterComponent
    void addContextualAuthorizationManagerComponent() throws Exception
    {
        this.componentManager.registerMockComponent(ContextualAuthorizationManager.class);
        this.componentManager.registerMockComponent(AuthorizationManager.class);
    }

    @BeforeEach
    void setUp() throws Exception
    {
        // avoid dependency issue with refactoring listeners
        this.componentManager.registerMockComponent(ModelBridge.class);
        this.componentManager.registerMockComponent(ReferenceUpdater.class);

        this.localExtensionRepository =
            (DefaultLocalExtensionRepository) this.componentManager.getInstance(LocalExtensionRepository.class);
        this.installedExtensionRepository = this.componentManager.getInstance(InstalledExtensionRepository.class);
        this.observation = this.componentManager.getInstance(ObservationManager.class);

        // Extensions

        EmptyExtension extension = new EmptyExtension(new ExtensionId("id", "version"), "xar");
        EmptyExtension extensionDependency = new EmptyExtension(new ExtensionId("dependency", "version"), "xar");
        extension.addDependency(new DefaultExtensionDependency(extensionDependency.getId().getId(),
            new DefaultVersionConstraint(null, extensionDependency.getId().getVersion())));

        this.localExtensionDependency1 = this.localExtensionRepository.storeExtension(extensionDependency);
        this.localExtension1 = this.localExtensionRepository.storeExtension(extension);
    }

    @Test
    void copyOneExtension() throws InstallException
    {
        InstalledExtension extensionDependency1 =
            this.installedExtensionRepository.installExtension(this.localExtensionDependency1, "wiki:source", false);
        InstalledExtension extension1 =
            this.installedExtensionRepository.installExtension(this.localExtension1, "wiki:source", false);

        assertFalse(extension1.isInstalled("wiki:target"));
        assertFalse(extensionDependency1.isInstalled("wiki:target"));

        this.observation.notify(new WikiCopiedEvent("source", "target"), null, null);

        assertTrue(extension1.isInstalled("wiki:target"));
        assertTrue(extensionDependency1.isInstalled("wiki:target"));
    }
}
