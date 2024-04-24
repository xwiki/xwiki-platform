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

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.groovy.internal.DefaultGroovyConfiguration;
import org.xwiki.groovy.internal.GroovyScriptEngineFactory;
import org.xwiki.logging.logback.internal.DefaultLoggerManager;
import org.xwiki.logging.script.LoggingScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.internal.DefaultObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.async.internal.AsyncMacro;
import org.xwiki.rendering.async.internal.block.DefaultBlockAsyncRenderer;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.box.DefaultBoxMacro;
import org.xwiki.rendering.internal.macro.context.ContextMacro;
import org.xwiki.rendering.internal.macro.context.ContextMacroDocument;
import org.xwiki.rendering.internal.macro.groovy.GroovyMacro;
import org.xwiki.rendering.internal.macro.script.PermissionCheckerListener;
import org.xwiki.rendering.internal.macro.source.DefaultMacroWikiContentSourceFactory;
import org.xwiki.rendering.internal.macro.toc.DefaultTocEntriesResolver;
import org.xwiki.rendering.internal.macro.toc.DefaultTocTreeBuilderFactory;
import org.xwiki.rendering.internal.macro.toc.TocMacro;
import org.xwiki.rendering.macro.script.MacroPermissionPolicy;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page Test for {@code ExtensionCode.ExtensionSheet}.
 *
 * @version $Id$
 * @since 15.10.9
 * @since 16.3.0
 */
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    AsyncMacro.class,
    ContextMacroDocument.class,
    ContextMacro.class,
    DefaultBlockAsyncRenderer.class,
    DefaultBoxMacro.class,
    DefaultGroovyConfiguration.class,
    DefaultLoggerManager.class,
    DefaultMacroWikiContentSourceFactory.class,
    DefaultObservationManager.class,
    DefaultTocEntriesResolver.class,
    DefaultTocTreeBuilderFactory.class,
    GroovyMacro.class,
    GroovyScriptEngineFactory.class,
    LoggingScriptService.class,
    PermissionCheckerListener.class,
    TestNoScriptMacro.class,
    TocMacro.class,
})
public class ExtensionSheetPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final DocumentReference EXTENSION_SHEET =
        new DocumentReference(WIKI_NAME, "ExtensionCode", "ExtensionSheet");

    private static final DocumentReference EXTENSION_CLASS =
        new DocumentReference(WIKI_NAME, "ExtensionCode", "ExtensionClass");

    private static final DocumentReference EXTENSION_DEPENDENCY_CLASS =
        new DocumentReference(WIKI_NAME, "ExtensionCode", "ExtensionDependencyClass");

    private static final DocumentReference EXTENSION_VERSION_CLASS =
        new DocumentReference(WIKI_NAME, "ExtensionCode", "ExtensionVersionClass");

    private static final DocumentReference EXTENSION_AUTHORS_DISPLAYER =
        new DocumentReference(WIKI_NAME, "ExtensionCode", "ExtensionAuthorsDisplayer");

    private static final DocumentReference TEST_PAGE =
        new DocumentReference(WIKI_NAME, "Test", "TestDocument");

    private XWikiDocument testPageDocument;

    private XWikiDocument extensionSheetDocument;

    @MockComponent
    @Named("groovy")
    private MacroPermissionPolicy groovyMacroPermissionPolicy;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.initializeMandatoryDocuments(this.context);
        loadPage(EXTENSION_AUTHORS_DISPLAYER);
        loadPage(EXTENSION_DEPENDENCY_CLASS);
        loadPage(EXTENSION_VERSION_CLASS);
        loadPage(EXTENSION_CLASS);
        loadPage(EXTENSION_SHEET);

        String testString = "]]}}}{{async}}{{velocity}}{{noscript /}}{{/velocity}}{{/async}}";
        String groovyTestString = """
                {{async}}
                {{groovy}}
                services.logging.getLogger("Groovy").error("SHOULD NOT BE CALLED!")
                {{/groovy}}
                {{/async}}
            """;

        this.extensionSheetDocument = this.xwiki.getDocument(EXTENSION_SHEET, this.context);

        this.testPageDocument = this.xwiki.getDocument(TEST_PAGE, this.context);
        this.testPageDocument.setTitle("Extension Test");
        BaseObject extensionObject =
            this.testPageDocument.newXObject(EXTENSION_CLASS, this.context);
        extensionObject.setStringValue("id", testString);
        extensionObject.setStringValue("name", testString);
        extensionObject.setStringValue("description", groovyTestString);
        extensionObject.setStringValue("summary", testString);
        extensionObject.setStringValue("icon", testString);
        extensionObject.setStringValue("type", testString);
        extensionObject.setStringValue("category", testString);
        extensionObject.setStringValue("installedCount", testString);
        extensionObject.setStringValue("licenseName", testString);
        extensionObject.setStringValue("issueManagementURL", testString);
        extensionObject.setStringValue("installation", groovyTestString);
        extensionObject.setStringValue("lastVersion", testString);
        extensionObject.setStringListValue("authors", List.of(testString));

        BaseObject extensionVersionObject = this.testPageDocument.newXObject(EXTENSION_VERSION_CLASS, this.context);
        extensionVersionObject.setStringValue("version", testString);
        extensionVersionObject.setStringValue("notes", groovyTestString);

        BaseObject extensionDependencyObject =
            this.testPageDocument.newXObject(EXTENSION_DEPENDENCY_CLASS, this.context);
        extensionDependencyObject.setStringValue("extensionVersion", testString);
        extensionDependencyObject.setStringValue("id", testString);
        extensionDependencyObject.setStringValue("constraint", testString);

        // Mock the database.
        Query query = mock(Query.class);
        QueryManagerScriptService queryManagerScriptService =
            this.componentManager.registerMockComponent(ScriptService.class, "query", QueryManagerScriptService.class,
                false);
        when(queryManagerScriptService.xwql(any())).thenReturn(query);
        when(query.bindValue(any(), any())).thenReturn(query);
        when(query.execute()).thenReturn(List.of());

        // Mock restricted contexts.
        when(this.groovyMacroPermissionPolicy.hasPermission(any(), any())).thenAnswer(i ->
            !((MacroTransformationContext) i.getArgument(1)).getTransformationContext().isRestricted());
    }

    @Test
    void display() throws Exception
    {
        this.context.setDoc(this.testPageDocument);
        // The only expectation is to not get any error log due to macro executions.
        renderHTMLPage(this.extensionSheetDocument);
    }
}
