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
package org.xwiki.extension.xar.internal.repository;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.test.ConfigurableDefaultCoreExtensionRepository;
import org.xwiki.extension.test.RepositoryUtils;
import org.xwiki.test.jmock.AbstractComponentTestCase;

public class XarInstalledExtensionRepositoryTest extends AbstractComponentTestCase
{
    private XarInstalledExtensionRepository installedExtensionRepository;

    private RepositoryUtils repositoryUtil;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil = new RepositoryUtils(getComponentManager(), getMockery());
        this.repositoryUtil.setup();

        // lookup

        this.installedExtensionRepository =
            getComponentManager().getInstance(InstalledExtensionRepository.class, "xar");
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        registerComponent(ConfigurableDefaultCoreExtensionRepository.class);
    }

    // Tests

    @Test
    public void testInit() throws ResolveException
    {
        Assert.assertTrue(this.installedExtensionRepository.countExtensions() == 1);

        Assert.assertNotNull(this.installedExtensionRepository.getInstalledExtension(new ExtensionId(
            "xarinstalledextension", "1.0")));

        Assert
            .assertNotNull(this.installedExtensionRepository.resolve(new ExtensionId("xarinstalledextension", "1.0")));
    }
}
