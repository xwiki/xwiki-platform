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

import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.groovy.internal.DefaultGroovyConfiguration;
import org.xwiki.groovy.internal.GroovyScriptEngineFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.async.internal.AsyncMacro;
import org.xwiki.rendering.async.internal.block.DefaultBlockAsyncRenderer;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.box.DefaultBoxMacro;
import org.xwiki.rendering.internal.macro.context.ContextMacro;
import org.xwiki.rendering.internal.macro.context.ContextMacroDocument;
import org.xwiki.rendering.internal.macro.groovy.GroovyMacro;
import org.xwiki.rendering.internal.macro.message.InfoMessageMacro;
import org.xwiki.rendering.internal.macro.script.PermissionCheckerListener;
import org.xwiki.rendering.internal.macro.script.source.ScriptMacroContentWikiSourceFactory;
import org.xwiki.rendering.internal.macro.source.DefaultMacroWikiContentSourceFactory;
import org.xwiki.rendering.internal.macro.source.MacroContentSourceReferenceConverter;
import org.xwiki.rendering.internal.macro.toc.DefaultTocEntriesResolver;
import org.xwiki.rendering.internal.macro.toc.DefaultTocTreeBuilderFactory;
import org.xwiki.rendering.internal.macro.toc.TocMacro;
import org.xwiki.rendering.macro.script.MacroPermissionPolicy;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Page Test for {@code ExtensionCode.ProjectSheet}.
 *
 * @version $Id$
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
    DefaultMacroWikiContentSourceFactory.class,
    MacroContentSourceReferenceConverter.class,
    ScriptMacroContentWikiSourceFactory.class,
    DefaultTocEntriesResolver.class,
    DefaultTocTreeBuilderFactory.class,
    GroovyMacro.class,
    GroovyScriptEngineFactory.class,
    InfoMessageMacro.class,
    PermissionCheckerListener.class,
    TestNoScriptMacro.class,
    TocMacro.class
})
class ProjectSheetPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final DocumentReference PROJECT_SHEET =
        new DocumentReference(WIKI_NAME, "ExtensionCode", "ProjectSheet");

    private static final DocumentReference PROJECT_CLASS =
        new DocumentReference(WIKI_NAME, "ExtensionCode", "ProjectClass");

    private static final DocumentReference EXTENSION_AUTHORS_DISPLAYER =
        new DocumentReference(WIKI_NAME, "ExtensionCode", "ExtensionAuthorsDisplayer");

    /**
     * Closes the markup the value could be inserted in, then calls a macro making the test fail when it's executed.
     */
    private static final String WIKI_SYNTAX = "]]}}}|(%%){{noscript /}}";

    private static final String WEBSITE = "https://www.example.org/a/long/path/]]{{noscript /}}";

    private static final String SCRIPT = "{{async}}{{groovy}}println(\"%s\"){{/groovy}}{{/async}}";

    @MockComponent
    @Named("groovy")
    private MacroPermissionPolicy groovyMacroPermissionPolicy;

    private XWikiDocument projectDocument;

    private XWikiDocument projectSheetDocument;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.initializeMandatoryDocuments(this.context);
        loadPage(EXTENSION_AUTHORS_DISPLAYER);
        loadPage(PROJECT_CLASS);
        this.projectSheetDocument = loadPage(PROJECT_SHEET);

        // Script macros are allowed only outside of restricted contexts.
        when(this.groovyMacroPermissionPolicy.hasPermission(any(), any())).thenAnswer(
            i -> !((MacroTransformationContext) i.getArgument(1)).getTransformationContext().isRestricted());

        this.projectDocument =
            this.xwiki.getDocument(new DocumentReference(WIKI_NAME, "Test", "Project"), this.context);
        BaseObject projectObject = this.projectDocument.newXObject(PROJECT_CLASS, this.context);
        projectObject.setStringValue("id", "org.example:project");
        projectObject.setStringValue("name", "Project");
        projectObject.setStringValue("summary", WIKI_SYNTAX);
        projectObject.setStringValue("icon", WIKI_SYNTAX);
        projectObject.setStringValue("website", WEBSITE);
        projectObject.setStringValue("licenseName", WIKI_SYNTAX);
        projectObject.setStringListValue("authors", List.of("Author"));
        projectObject.setLargeStringValue("description", SCRIPT.formatted("DESCRIPTION_EXECUTED"));
        projectObject.setLargeStringValue("entryPoints", SCRIPT.formatted("ENTRY_POINTS_EXECUTED"));
        this.context.setDoc(this.projectDocument);
    }

    @Test
    void propertiesAreDisplayedAsTheirValue() throws Exception
    {
        Document document = renderHTMLPage(this.projectSheetDocument);

        // The noscript macro of the values isn't executed, otherwise its error log would make the test fail.
        String text = document.text();
        assertFalse(text.contains("DESCRIPTION_EXECUTED"), text);
        assertFalse(text.contains("ENTRY_POINTS_EXECUTED"), text);
        // The summary and the license name.
        assertEquals(2, StringUtils.countMatches(text, WIKI_SYNTAX), text);
        // The website is too long to be displayed entirely.
        Element websiteLink = document.selectFirst(".extensionInfo a[href=\"%s\"]".formatted(WEBSITE));
        assertNotNull(websiteLink, document::html);
        assertEquals(WEBSITE.substring(0, 40) + "...", websiteLink.text(), websiteLink::outerHtml);
    }
}
