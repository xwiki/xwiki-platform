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

import java.util.Arrays;
import java.util.Locale;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.test.MockitoRepositoryUtilsRule;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@AllComponents
public class XarInstalledExtensionRepositoryTest
{
    protected MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    @Rule
    public MockitoRepositoryUtilsRule repositoryUtil = new MockitoRepositoryUtilsRule(this.mocker);

    private XarInstalledExtensionRepository installedExtensionRepository;

    @Before
    public void setUp() throws Exception
    {
        this.installedExtensionRepository = this.mocker.getInstance(InstalledExtensionRepository.class, "xar");
    }

    // Tests

    @Test
    public void testInit() throws ResolveException, SearchException
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

        assertEquals(Arrays.asList(xarInstalledExtension), this.installedExtensionRepository
            .getXarInstalledExtensions(new DocumentReference("xwiki", "space", "page")));
        assertEquals(Arrays.asList(xarInstalledExtension), this.installedExtensionRepository
            .getXarInstalledExtensions(new DocumentReference("wiki2", "space", "page")));
        assertEquals(Arrays.asList(xarInstalledExtension), this.installedExtensionRepository
            .getXarInstalledExtensions(new DocumentReference("xwiki", "space", "page", Locale.ROOT)));
        assertEquals(0, this.installedExtensionRepository
            .getXarInstalledExtensions(new DocumentReference("xwiki", "space", "page", Locale.ENGLISH)).size());
    }
}
