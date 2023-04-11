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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import org.xwiki.extension.test.MockitoRepositoryUtilsRule;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.observation.ObservationManager;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.internal.ReferenceUpdater;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

@AllComponents
public class WikiEventListenerTest
{
    @Rule
    public MockitoRepositoryUtilsRule repositoryUtil = new MockitoRepositoryUtilsRule();

    private DefaultLocalExtensionRepository localExtensionRepository;

    private InstalledExtensionRepository installedExtensionRepository;

    private WikiDescriptorManager wikiDescriptorManager;

    private ObservationManager observation;

    private LocalExtension localExtension1;

    private LocalExtension localExtensionDependency1;

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.wikiDescriptorManager =
            this.repositoryUtil.getComponentManager().registerMockComponent(WikiDescriptorManager.class);
    }

    @Before
    public void setUp() throws Exception
    {
        // avoid dependency issue with refactoring listeners
        this.repositoryUtil.getComponentManager().registerMockComponent(ModelBridge.class);
        this.repositoryUtil.getComponentManager().registerMockComponent(ReferenceUpdater.class);

        this.localExtensionRepository =
            this.repositoryUtil.getComponentManager().getInstance(LocalExtensionRepository.class);
        this.installedExtensionRepository =
            this.repositoryUtil.getComponentManager().getInstance(InstalledExtensionRepository.class);

        this.observation = this.repositoryUtil.getComponentManager().getInstance(ObservationManager.class);

        // Extensions

        EmptyExtension extension = new EmptyExtension(new ExtensionId("id", "version"), "xar");
        EmptyExtension extensionDependency = new EmptyExtension(new ExtensionId("dependency", "version"), "xar");
        extension.addDependency(new DefaultExtensionDependency(extensionDependency.getId().getId(),
            new DefaultVersionConstraint(null, extensionDependency.getId().getVersion())));

        this.localExtensionDependency1 = this.localExtensionRepository.storeExtension(extensionDependency);
        this.localExtension1 = this.localExtensionRepository.storeExtension(extension);
    }

    @AfterComponent
    public void addContextualAuthorizationManagerComponent() throws Exception
    {
        this.repositoryUtil.getComponentManager().registerMockComponent(ContextualAuthorizationManager.class);
        this.repositoryUtil.getComponentManager().registerMockComponent(AuthorizationManager.class);
    }

    @Test
    public void testCopyOneExtension() throws InstallException
    {
        InstalledExtension extensionDependency1 =
            this.installedExtensionRepository.installExtension(this.localExtensionDependency1, "wiki:source", false);
        InstalledExtension extension1 =
            this.installedExtensionRepository.installExtension(this.localExtension1, "wiki:source", false);

        Assert.assertFalse(extension1.isInstalled("wiki:target"));
        Assert.assertFalse(extensionDependency1.isInstalled("wiki:target"));

        this.observation.notify(new WikiCopiedEvent("source", "target"), null, null);

        Assert.assertTrue(extension1.isInstalled("wiki:target"));
        Assert.assertTrue(extensionDependency1.isInstalled("wiki:target"));
    }
}
