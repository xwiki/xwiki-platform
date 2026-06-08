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
package org.xwiki.repository.server.ui;

import javax.inject.Named;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.groovy.internal.DefaultGroovyConfiguration;
import org.xwiki.groovy.internal.GroovyScriptEngineFactory;
import org.xwiki.logging.logback.internal.DefaultLoggerManager;
import org.xwiki.logging.script.LoggingScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.internal.DefaultObservationManager;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.async.internal.AsyncMacro;
import org.xwiki.rendering.async.internal.block.DefaultBlockAsyncRenderer;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.groovy.GroovyMacro;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.rendering.internal.macro.message.SuccessMessageMacro;
import org.xwiki.rendering.internal.macro.script.PermissionCheckerListener;
import org.xwiki.rendering.internal.macro.source.DefaultMacroWikiContentSourceFactory;
import org.xwiki.rendering.macro.script.MacroPermissionPolicy;
import org.xwiki.repository.script.RepositoryScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Page test for {@code ExtensionCode.ImportExtension}.
 *
 * @version $Id$
 */
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    DefaultLoggerManager.class,
    DefaultMacroWikiContentSourceFactory.class,
    DefaultObservationManager.class,
    LoggingScriptService.class,
    PermissionCheckerListener.class,
    TestNoScriptMacro.class,
    // Async
    AsyncMacro.class,
    DefaultBlockAsyncRenderer.class,
    // Groovy
    DefaultGroovyConfiguration.class,
    GroovyMacro.class,
    GroovyScriptEngineFactory.class,
    // Messages
    ErrorMessageMacro.class,
    SuccessMessageMacro.class
})
class ImportExtensionPageTest extends PageTest
{
    private static final DocumentReference IMPORT_EXTENSION_REFERENCE =
        new DocumentReference("xwiki", "ExtensionCode", "ImportExtension");

    private static final DocumentReference REPOSITORY_CODE_REFEFERENCE =
        new DocumentReference("xwiki", "ExtensionCode", "RepositoryCode");

    @MockComponent
    @Named("groovy")
    private MacroPermissionPolicy groovyMacroPermissionPolicy;

    private RepositoryScriptService repositoryScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.initializeMandatoryDocuments(this.context);
        loadPage(REPOSITORY_CODE_REFEFERENCE);

        this.repositoryScriptService =
            this.componentManager.registerMockComponent(ScriptService.class, "repository",
                RepositoryScriptService.class, false);

        // Allow groovy execution when the rendering context is not restricted (content from trusted authors).
        when(this.groovyMacroPermissionPolicy.hasPermission(any(), any())).thenReturn(true);
    }

    @Test
    void importFailureRendersErrorSafely() throws Exception
    {
        // Simulate the XWIKI-24410 attack: wiki macro injection via repositoryId ends up calling noscript.
        String maliciousRepositoryId =
            "{{async async=\"true\" cached=\"false\"}}{{noscript /}}{{/async}}";
        String errorMessage =
            "Can't find any registered repository with id [" + maliciousRepositoryId + "]";

        Exception importError = new RuntimeException(errorMessage, new RuntimeException("cause message"));
        when(this.repositoryScriptService.importExtension(any(), any())).thenReturn(null);
        when(this.repositoryScriptService.getLastError()).thenReturn(importError);

        this.request.put("importExtension", "true");
        this.request.put("extensionId", "some-ext");
        this.request.put("repositoryId", maliciousRepositoryId);

        Document document = renderHTMLPage(IMPORT_EXTENSION_REFERENCE);

        String expectedStart = "Failed to import extension: java.lang.RuntimeException: "
            + "Can't find any registered repository with id "
            + "[{{async async=\"true\" cached=\"false\"}}{{noscript /}}{{/async}}]";
        String actualStack = document.select(".box.errormessage").text();
        assertTrue(actualStack.startsWith(expectedStart),
            "[%s] is expected to start with [%s]".formatted(actualStack, expectedStart));
    }
}
