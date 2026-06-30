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

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.test.MockitoRepositoryUtilsExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ComponentTest
@AllComponents
@ExtendWith(MockitoRepositoryUtilsExtension.class)
class XarInstalledExtensionRepositoryTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private XarInstalledExtensionRepository installedExtensionRepository;

    @AfterComponent
    void afterComponent() throws Exception
    {
        this.componentManager.registerMockComponent(WikiDescriptorManager.class);
    }

    @BeforeEach
    void setUp() throws Exception
    {
        this.installedExtensionRepository =
            this.componentManager.getInstance(InstalledExtensionRepository.class, "xar");
    }

    // Tests

    @Test
    void init() throws ResolveException, SearchException
    {
        assertEquals(1, this.installedExtensionRepository.countExtensions());

        XarInstalledExtension xarInstalledExtension =
            this.installedExtensionRepository.resolve(new ExtensionId("xarinstalledextension", "1.0"));
        assertNotNull(xarInstalledExtension);

        assertNotNull(
            this.installedExtensionRepository.getInstalledExtension(new ExtensionId("xarinstalledextension", "1.0")));
        assertNotNull(this.installedExtensionRepository.getInstalledExtension("xarinstalledextension", null));
        assertNull(this.installedExtensionRepository.getInstalledExtension("notexisting", null));

        assertEquals(1, this.installedExtensionRepository.getInstalledExtensions().size());
        assertEquals(1, this.installedExtensionRepository.getInstalledExtensions(null).size());

        assertEquals(1, this.installedExtensionRepository.search("xarinstalledextension", 0, -1).getSize());
        assertEquals(1, this.installedExtensionRepository.search(null, 0, -1).getSize());
        assertEquals(1, this.installedExtensionRepository
            .searchInstalledExtensions("xarinstalledextension", null, 0, -1).getSize());
        assertEquals(1, this.installedExtensionRepository.searchInstalledExtensions(null, null, 0, -1).getSize());

        assertEquals(List.of(xarInstalledExtension), this.installedExtensionRepository
            .getXarInstalledExtensions(new DocumentReference("xwiki", "space", "page")));
        assertEquals(List.of(xarInstalledExtension), this.installedExtensionRepository
            .getXarInstalledExtensions(new DocumentReference("wiki2", "space", "page")));
        assertEquals(List.of(xarInstalledExtension), this.installedExtensionRepository
            .getXarInstalledExtensions(new DocumentReference("xwiki", "space", "page", Locale.ROOT)));
        assertEquals(0, this.installedExtensionRepository
            .getXarInstalledExtensions(new DocumentReference("xwiki", "space", "page", Locale.ENGLISH)).size());
    }
}
