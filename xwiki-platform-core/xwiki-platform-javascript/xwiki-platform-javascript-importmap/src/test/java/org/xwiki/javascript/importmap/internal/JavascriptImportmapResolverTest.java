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
package org.xwiki.javascript.importmap.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.webjars.WebJarsUrlFactory;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link JavascriptImportmapResolver}.
 *
 * @version $Id$
 * @since 18.0.0RC1
 */
@ComponentTest
class JavascriptImportmapResolverTest
{
    @InjectMockComponents
    private JavascriptImportmapResolver javascriptImportmapResolver;

    @MockComponent
    private InstalledExtensionRepository installedExtensionRepository;

    @MockComponent
    private CoreExtensionRepository coreExtensionRepository;

    @MockComponent
    private WebJarsUrlFactory webJarsUrlFactory;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;


    @Test
    void cacheIsUsedUntilCleared()
    {
        var wikiId = "wiki1";
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn(wikiId);
        var wikiNamespace = new WikiNamespace(wikiId).serialize();
        this.javascriptImportmapResolver.getBlock();
        // Get installed extension is used as a proxy to verify if the resolution logic is called.
        verify(this.installedExtensionRepository).getInstalledExtensions(wikiNamespace);
        this.javascriptImportmapResolver.getBlock();
        verify(this.installedExtensionRepository).getInstalledExtensions(wikiNamespace);
        this.javascriptImportmapResolver.clearCache();
        this.javascriptImportmapResolver.getBlock();
        verify(this.installedExtensionRepository, times(2)).getInstalledExtensions(wikiNamespace);
    }
}