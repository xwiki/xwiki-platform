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
package org.xwiki.extension.distribution.internal;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link OrphanedExtensionsResolver}.
 *
 * @version $Id$
 */
@ComponentTest
class OrphanedExtensionsResolverTest
{
    private static final String MAIN_WIKI = "xwiki";

    private static final String SUB_WIKI = "subwiki";

    private static final String MAIN_NAMESPACE = "wiki:" + MAIN_WIKI;

    private static final String SUB_NAMESPACE = "wiki:" + SUB_WIKI;

    @InjectMockComponents
    private OrphanedExtensionsResolver resolver;

    @MockComponent
    private InstalledExtensionRepository installedRepository;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    // Not needed anymore by any extension, on the root namespace
    private InstalledExtension rootOrphan;

    // Still needed by an extension of the sub wiki, on the root namespace
    private InstalledExtension rootDependency;

    // Not needed anymore by any extension, on the sub wiki
    private InstalledExtension wikiOrphan;

    // Still needed by another extension, on the sub wiki
    private InstalledExtension wikiDependency;

    // Explicitly installed by the user on the root namespace
    private InstalledExtension rootTopExtension;

    @BeforeEach
    void beforeEach() throws ResolveException
    {
        when(this.wikiDescriptorManager.isMainWiki(MAIN_WIKI)).thenReturn(true);

        this.rootOrphan = mockInstalledExtension("rootorphan", true, true);
        this.rootDependency = mockInstalledExtension("rootdependency", true, true);
        this.wikiOrphan = mockInstalledExtension("wikiorphan", false, true);
        this.wikiDependency = mockInstalledExtension("wikidependency", false, true);
        this.rootTopExtension = mockInstalledExtension("roottop", true, false);

        InstalledExtension backwardDependency = mock();
        when(this.installedRepository.getBackwardDependencies(this.rootOrphan.getId(), true)).thenReturn(Map.of());
        when(this.installedRepository.getBackwardDependencies(this.rootDependency.getId(), true))
            .thenReturn(Map.of(SUB_NAMESPACE, List.of(backwardDependency)));
        when(this.installedRepository.getBackwardDependencies(this.wikiOrphan.getId().getId(), SUB_NAMESPACE, true))
            .thenReturn(List.of());
        when(this.installedRepository.getBackwardDependencies(this.wikiDependency.getId().getId(), SUB_NAMESPACE,
            true)).thenReturn(List.of(backwardDependency));

        // Put the orphans first to make sure the result of an extension does not leak into the next ones
        when(this.installedRepository.getInstalledExtensions(MAIN_NAMESPACE))
            .thenReturn(List.of(this.rootOrphan, this.rootDependency, this.rootTopExtension));
        when(this.installedRepository.getInstalledExtensions(SUB_NAMESPACE)).thenReturn(List.of(this.wikiOrphan,
            this.rootOrphan, this.rootDependency, this.wikiDependency, this.rootTopExtension));
    }

    private InstalledExtension mockInstalledExtension(String id, boolean root, boolean dependency)
    {
        InstalledExtension extension = mock();
        when(extension.getId()).thenReturn(new ExtensionId(id, "1.0"));
        when(extension.isInstalled(null)).thenReturn(root);
        when(extension.isDependency(MAIN_NAMESPACE)).thenReturn(dependency);
        when(extension.isDependency(SUB_NAMESPACE)).thenReturn(dependency);

        return extension;
    }

    @Test
    void getOrphanedExtensionsOnSubWiki()
    {
        // Extensions installed on the root namespace are only handled from the main wiki
        assertEquals(List.of(this.wikiOrphan), this.resolver.getOrphanedExtensions(SUB_WIKI));
    }

    @Test
    void getOrphanedExtensionsOnMainWiki()
    {
        assertEquals(List.of(this.rootOrphan), this.resolver.getOrphanedExtensions(MAIN_WIKI));
    }

    @Test
    void getOrphanedExtensionsWhenBackwardDependenciesCannotBeResolved() throws ResolveException
    {
        when(this.installedRepository.getBackwardDependencies(this.wikiOrphan.getId().getId(), SUB_NAMESPACE, true))
            .thenThrow(new ResolveException("error"));

        assertEquals(List.of(), this.resolver.getOrphanedExtensions(SUB_WIKI));

        assertEquals("Failed to resolve backward dependencies for extension id [wikiorphan] on namespace "
            + "[wiki:subwiki]. Root cause is [ResolveException: error]", this.logCapture.getMessage(0));
    }
}
