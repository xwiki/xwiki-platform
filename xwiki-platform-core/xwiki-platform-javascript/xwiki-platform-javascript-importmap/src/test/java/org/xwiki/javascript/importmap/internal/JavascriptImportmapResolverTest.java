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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.webjars.WebJarsUrlFactory;
import org.xwiki.webjars.WebjarPathDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapParser.JAVASCRIPT_IMPORTMAP_PROPERTY;
import static org.xwiki.rendering.syntax.Syntax.HTML_5_0;

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

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private String wikiNamespace;

    @BeforeEach
    void setUp()
    {
        var wikiId = "wiki1";
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn(wikiId);
        this.wikiNamespace = new WikiNamespace(wikiId).serialize();
    }

    @Test
    void cacheIsUsedUntilCleared()
    {
        this.javascriptImportmapResolver.getBlock();
        // Get installed extension is used as a proxy to verify if the resolution logic is called.
        verify(this.installedExtensionRepository).getInstalledExtensions(this.wikiNamespace);
        this.javascriptImportmapResolver.getBlock();
        verify(this.installedExtensionRepository).getInstalledExtensions(this.wikiNamespace);
        this.javascriptImportmapResolver.clearCache();
        this.javascriptImportmapResolver.getBlock();
        verify(this.installedExtensionRepository, times(2)).getInstalledExtensions(this.wikiNamespace);
    }

    @Test
    void getBlockReturnsValidImportmapScript()
    {
        // Mock an extension with importmap property
        InstalledExtension extension = mock(InstalledExtension.class);
        when(extension.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY)).thenReturn("""
            {
                "vue": "org.webjars.npm:vue/index.js"
            }
            """);
        when(this.installedExtensionRepository.getInstalledExtensions(this.wikiNamespace))
            .thenReturn(List.of(extension));
        when(this.coreExtensionRepository.getCoreExtensions()).thenReturn(List.of());
        when(this.webJarsUrlFactory.url(new WebjarPathDescriptor("org.webjars.npm:vue", "index.js")))
            .thenReturn("/webjars/vue/1.2.3/index.js");

        Block result = this.javascriptImportmapResolver.getBlock();

        assertEquals(
            new RawBlock("<script type=\"importmap\">{\"imports\":{\"vue\":\"/webjars/vue/1.2.3/index.js\"}}</script>",
                HTML_5_0), result);
    }

    @Test
    void getBlockHandlesEmptyExtensions()
    {
        when(this.installedExtensionRepository.getInstalledExtensions(this.wikiNamespace))
            .thenReturn(List.of());
        when(this.coreExtensionRepository.getCoreExtensions()).thenReturn(List.of());

        Block result = this.javascriptImportmapResolver.getBlock();

        assertEquals(new RawBlock("<script type=\"importmap\">{\"imports\":{}}</script>",
            HTML_5_0), result);
    }

    @Test
    void getBlockHandlesExtensionsWithoutImportmapProperty()
    {
        InstalledExtension extension = mock(InstalledExtension.class);
        when(extension.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY)).thenReturn(null);
        when(this.installedExtensionRepository.getInstalledExtensions(this.wikiNamespace))
            .thenReturn(List.of(extension));
        when(this.coreExtensionRepository.getCoreExtensions()).thenReturn(List.of());

        Block result = this.javascriptImportmapResolver.getBlock();

        assertEquals(new RawBlock("<script type=\"importmap\">{\"imports\":{}}</script>",
            HTML_5_0), result);
    }

    @Test
    void getBlockHandlesMultipleExtensionsWithSameImport()
    {
        // First extension with vue import
        InstalledExtension extension1 = mock(InstalledExtension.class);
        when(extension1.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY)).thenReturn(
            """
                {
                    "vue":"org.webjars.npm:vue/index.js"
                }""");

        // Second extension with same vue import (should not cause conflict)
        InstalledExtension extension2 = mock(InstalledExtension.class);
        when(extension2.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY)).thenReturn("""
            {
                "vue":"org.webjars.npm:vue/index.js"
            }""");

        when(this.installedExtensionRepository.getInstalledExtensions(this.wikiNamespace))
            .thenReturn(List.of(extension1, extension2));
        when(this.coreExtensionRepository.getCoreExtensions()).thenReturn(List.of());
        when(this.webJarsUrlFactory.url(new WebjarPathDescriptor("org.webjars.npm:vue", "index.js")))
            .thenReturn("/webjars/vue/1.2.3/vue.js");

        Block result = this.javascriptImportmapResolver.getBlock();

        assertEquals(
            new RawBlock("<script type=\"importmap\">{\"imports\":{\"vue\":\"/webjars/vue/1.2.3/vue.js\"}}</script>",
                HTML_5_0), result);
    }

    @Test
    void getBlockHandlesCoreExtensions()
    {
        when(this.installedExtensionRepository.getInstalledExtensions(this.wikiNamespace))
            .thenReturn(List.of());

        // Mock a core extension with importmap property
        CoreExtension coreExtension = mock(CoreExtension.class);
        when(coreExtension.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY)).thenReturn(
            """
                {
                    "lodash":"org.webjars.npm:lodash/index.js"
                }
                """);
        when(this.coreExtensionRepository.getCoreExtensions()).thenReturn(List.of(coreExtension));
        when(this.webJarsUrlFactory.url(new WebjarPathDescriptor("org.webjars.npm:lodash", "index.js")))
            .thenReturn("/webjars/lodash/4.17.21/lodash.js");

        Block result = this.javascriptImportmapResolver.getBlock();

        assertEquals(
            new RawBlock(
                "<script type=\"importmap\">{\"imports\":{\"lodash\":\"/webjars/lodash/4.17.21/lodash.js\"}}</script>",
                HTML_5_0), result);
    }

    @Test
    void getBlockHandlesEagerLoading()
    {
        // Mock an extension with importmap property containing eager loading
        InstalledExtension extension = mock(InstalledExtension.class);
        when(extension.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY)).thenReturn("""
            {
                "vue": {
                    "webjarId": "org.webjars.npm:vue",
                    "path": "index.js",
                    "eager": true
                }
            }
            """);
        when(this.installedExtensionRepository.getInstalledExtensions(this.wikiNamespace))
            .thenReturn(List.of(extension));
        when(this.coreExtensionRepository.getCoreExtensions()).thenReturn(List.of());
        when(this.webJarsUrlFactory.url(new WebjarPathDescriptor("org.webjars.npm:vue", "index.js")))
            .thenReturn("/webjars/vue/1.2.3/index.js");

        Block result = this.javascriptImportmapResolver.getBlock();

        assertEquals(
            new RawBlock(
                """
                    <script type=\"importmap\">{"imports":{"vue":"/webjars/vue/1.2.3/index.js"}}</script>
                    <script type="module" src="/webjars/vue/1.2.3/index.js"></script>""",
                HTML_5_0), result);
    }

    @Test
    void getBlockHandlesAnonymousLoading()
    {
        // Mock an extension with importmap property containing anonymous loading
        InstalledExtension extension = mock(InstalledExtension.class);
        when(extension.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY)).thenReturn("""
            {
                "vue": {
                    "webjarId": "org.webjars.npm:vue",
                    "path": "index.js",
                    "anonymous": true
                }
            }
            """);
        when(this.installedExtensionRepository.getInstalledExtensions(this.wikiNamespace))
            .thenReturn(List.of(extension));
        when(this.coreExtensionRepository.getCoreExtensions()).thenReturn(List.of());
        when(this.webJarsUrlFactory.url(new WebjarPathDescriptor("org.webjars.npm:vue", "index.js")))
            .thenReturn("/webjars/vue/1.2.3/index.js");

        Block result = this.javascriptImportmapResolver.getBlock();

        assertEquals(
            new RawBlock(
                "<script type=\"importmap\">{\"imports\":{}}</script>",
                HTML_5_0), result);
    }

    @Test
    void getBlockHandlesEagerAndAnonymousLoading()
    {
        // Mock an extension with importmap property containing both eager and anonymous loading
        InstalledExtension extension = mock(InstalledExtension.class);
        when(extension.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY)).thenReturn("""
            {
                "vue": {
                    "webjarId": "org.webjars.npm:vue",
                    "path": "index.js",
                    "eager": true,
                    "anonymous": true
                }
            }
            """);
        when(this.installedExtensionRepository.getInstalledExtensions(this.wikiNamespace))
            .thenReturn(List.of(extension));
        when(this.coreExtensionRepository.getCoreExtensions()).thenReturn(List.of());
        when(this.webJarsUrlFactory.url(new WebjarPathDescriptor("org.webjars.npm:vue", "index.js")))
            .thenReturn("/webjars/vue/1.2.3/index.js");

        Block result = this.javascriptImportmapResolver.getBlock();

        assertEquals(
            new RawBlock(
                """
                    <script type=\"importmap\">{"imports":{}}</script>
                    <script type="module" src="/webjars/vue/1.2.3/index.js"></script>""",
                HTML_5_0), result);
    }

    @Test
    void getBlockLogsWarningOnMalformedImportmap()
    {
        InstalledExtension extension = mock(InstalledExtension.class);
        when(extension.getId()).thenReturn(new ExtensionId("ext1", "1.0"));
        when(extension.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY)).thenReturn("not a valid json");
        when(this.installedExtensionRepository.getInstalledExtensions(this.wikiNamespace))
            .thenReturn(List.of(extension));
        when(this.coreExtensionRepository.getCoreExtensions()).thenReturn(List.of());

        Block result = this.javascriptImportmapResolver.getBlock();

        assertEquals(new RawBlock("<script type=\"importmap\">{\"imports\":{}}</script>", HTML_5_0), result);
        assertEquals("Unable to read property [xwiki.extension.javascript.modules.importmap] for extension "
            + "[ext1/1.0]. Cause: [JsonParseException: Unrecognized token 'not': was expecting (JSON String, "
            + "Number, Array, Object or token 'null', 'true' or 'false')\n"
            + " at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, "
            + "column: 1]]", this.logCapture.getMessage(0));
    }

    @Test
    void getBlockLogsWarningOnConflictingImportmap()
    {
        InstalledExtension extension1 = mock(InstalledExtension.class);
        when(extension1.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY)).thenReturn("""
            {
                "vue": "org.webjars.npm:vue/index.js"
            }
            """);
        InstalledExtension extension2 = mock(InstalledExtension.class);
        when(extension2.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY)).thenReturn("""
            {
                "vue": "org.webjars.npm:vue/other.js"
            }
            """);
        when(this.installedExtensionRepository.getInstalledExtensions(this.wikiNamespace))
            .thenReturn(List.of(extension1, extension2));
        when(this.coreExtensionRepository.getCoreExtensions()).thenReturn(List.of());
        when(this.webJarsUrlFactory.url(new WebjarPathDescriptor("org.webjars.npm:vue", "index.js")))
            .thenReturn("/webjars/vue/1.2.3/index.js");
        when(this.webJarsUrlFactory.url(new WebjarPathDescriptor("org.webjars.npm:vue", "other.js")))
            .thenReturn("/webjars/vue/1.2.3/other.js");

        this.javascriptImportmapResolver.getBlock();

        assertEquals("Conflicting importmap resolution for key [vue]. Existing value: "
            + "[/webjars/vue/1.2.3/index.js], new value: [/webjars/vue/1.2.3/other.js]",
            this.logCapture.getMessage(0));
    }

    @Test
    void getBlockLogsWarningOnConflictingEager()
    {
        InstalledExtension extension1 = mock(InstalledExtension.class);
        when(extension1.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY)).thenReturn("""
            {
                "vue": {
                    "webjarId": "org.webjars.npm:vue",
                    "path": "index.js",
                    "eager": true,
                    "anonymous": true
                }
            }
            """);
        InstalledExtension extension2 = mock(InstalledExtension.class);
        when(extension2.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY)).thenReturn("""
            {
                "vue": {
                    "webjarId": "org.webjars.npm:vue",
                    "path": "other.js",
                    "eager": true,
                    "anonymous": true
                }
            }
            """);
        when(this.installedExtensionRepository.getInstalledExtensions(this.wikiNamespace))
            .thenReturn(List.of(extension1, extension2));
        when(this.coreExtensionRepository.getCoreExtensions()).thenReturn(List.of());
        when(this.webJarsUrlFactory.url(new WebjarPathDescriptor("org.webjars.npm:vue", "index.js")))
            .thenReturn("/webjars/vue/1.2.3/index.js");
        when(this.webJarsUrlFactory.url(new WebjarPathDescriptor("org.webjars.npm:vue", "other.js")))
            .thenReturn("/webjars/vue/1.2.3/other.js");

        this.javascriptImportmapResolver.getBlock();

        assertEquals("Conflicting eager resolution for key [vue]. Existing value: "
            + "[/webjars/vue/1.2.3/index.js], new value: [/webjars/vue/1.2.3/other.js]",
            this.logCapture.getMessage(0));
    }
}
