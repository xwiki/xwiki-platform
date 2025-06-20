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
package org.xwiki.administration;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.xwiki.administration.api.ConfigurableObjectEvaluator;
import org.xwiki.evaluation.internal.DefaultObjectEvaluator;
import org.xwiki.evaluation.internal.VelocityObjectPropertyEvaluator;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.rendering.internal.macro.message.WarningMessageMacro;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.user.UserReferenceComponentList;
import org.xwiki.user.internal.converter.DocumentUserReferenceConverter;
import org.xwiki.user.internal.document.DocumentUserReference;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.DocumentReferenceConverter;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Page test of {@code XWiki.ConfigurableClass}.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@SecurityScriptServiceComponentList
@UserReferenceComponentList
@ComponentList({
    TestNoScriptMacro.class,
    TranslationMacro.class,
    ErrorMessageMacro.class,
    WarningMessageMacro.class,
    DocumentUserReferenceConverter.class,
    DocumentReferenceConverter.class,
    DefaultObjectEvaluator.class,
    VelocityObjectPropertyEvaluator.class,
    ConfigurableObjectEvaluator.class
})
class ConfigurableClassPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final String SPACE_NAME = "XWiki";

    private static final DocumentReference CONFIGURABLE_CLASS =
        new DocumentReference(WIKI_NAME, SPACE_NAME, "ConfigurableClass");

    private static final DocumentReference CONFIGURABLE_CLASS_MACROS =
        new DocumentReference(WIKI_NAME, SPACE_NAME, "ConfigurableClassMacros");

    private static final DocumentReference MY_SECTION =
        new DocumentReference(WIKI_NAME, SPACE_NAME, "]],{{noscript /}}");

    private static final String MY_SECTION_SERIALIZED = "XWiki.]],{{noscript /}}";

    private static final String CONFIG_CLASS_NAME = "TestClass";

    private static final String WEB_HOME = "WebHome";

    @Mock
    private QueryManagerScriptService queryService;

    @Mock
    private ScriptQuery query;

    private AuthorExecutor authorExecutor;

    private VelocityEngine velocityEngine;

    @BeforeEach
    void setUp() throws Exception
    {
        // Load the macros page so it can be included.
        loadPage(CONFIGURABLE_CLASS_MACROS);

        // Mock the query.
        this.oldcore.getMocker().registerComponent(ScriptService.class, "query", this.queryService);
        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.addFilter(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.bindValues(any(List.class))).thenReturn(this.query);

        this.authorExecutor = this.componentManager.registerMockComponent(AuthorExecutor.class, true);

        // Spy Velocity Engine.
        VelocityManager velocityManager = this.componentManager.getInstance(VelocityManager.class);
        this.velocityEngine = velocityManager.getVelocityEngine();
        this.velocityEngine = spy(this.velocityEngine);
        velocityManager = spy(velocityManager);
        this.componentManager.registerComponent(VelocityManager.class, velocityManager);
        when(velocityManager.getVelocityEngine()).thenReturn(this.velocityEngine);

        when(this.authorExecutor.call(any(), any(), any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            return callable.call();
        });
    }

    @Test
    void escapeHeadingForError() throws Exception
    {
        this.request.put("section", "other");
        when(this.query.execute()).thenReturn(List.of(MY_SECTION_SERIALIZED)).thenReturn(List.of());

        XWikiDocument mySectionDoc = new XWikiDocument(MY_SECTION);
        this.xwiki.saveDocument(mySectionDoc, this.context);

        Document htmlPage = renderHTMLPage(CONFIGURABLE_CLASS);
        assertEquals(String.format("admin.customize %s:", MY_SECTION_SERIALIZED),
            htmlPage.selectFirst("h2").text());
    }

    @Test
    void escapeHeading() throws Exception
    {
        this.request.put("section", "other");
        when(this.query.execute()).thenReturn(List.of(MY_SECTION_SERIALIZED)).thenReturn(List.of());
        when(this.oldcore.getMockRightService()
            .hasAccessLevel(eq("edit"), any(), any(), any())).thenReturn(true);

        XWikiDocument mySectionDoc = new XWikiDocument(MY_SECTION);
        BaseObject object = mySectionDoc.newXObject(CONFIGURABLE_CLASS, this.context);
        object.setStringValue("displayInCategory", "other");
        object.setStringValue("displayInSection", "other");
        object.setStringValue("heading", "{{noscript /}}");
        object.set("scope", "WIKI+ALL_SPACES", this.context);
        this.xwiki.saveDocument(mySectionDoc, this.context);

        Document htmlPage = renderHTMLPage(CONFIGURABLE_CLASS);
        assertEquals("{{noscript /}}", htmlPage.selectFirst("h2").text());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void checkScriptRight(boolean hasScript) throws Exception
    {
        this.request.put("section", "other");
        when(this.query.execute()).thenReturn(List.of(MY_SECTION_SERIALIZED)).thenReturn(List.of());
        when(this.oldcore.getMockRightService()
            .hasAccessLevel(eq("edit"), any(), any(), any())).thenReturn(true);

        DocumentReference configClassRef = new DocumentReference("xwiki", SPACE_NAME, CONFIG_CLASS_NAME);
        XWikiDocument configClassDoc = new XWikiDocument(configClassRef);
        BaseClass configClass = new BaseClass();
        configClass.addTextField("test", "Test", 10);
        configClassDoc.setXClass(configClass);
        this.xwiki.saveDocument(configClassDoc, this.context);

        XWikiDocument mySectionDoc = new XWikiDocument(MY_SECTION);
        BaseObject configObject = mySectionDoc.newXObject(configClassRef, this.context);
        BaseObject object = mySectionDoc.newXObject(CONFIGURABLE_CLASS, this.context);
        object.setStringValue("displayInCategory", "other");
        object.setStringValue("displayInSection", "other");
        object.setStringValue("configurationClass", SPACE_NAME + "." + CONFIG_CLASS_NAME);
        String originalHeading = "$appName {{noscript /}}";
        object.setStringValue("heading", originalHeading);
        String originalLinkPrefix = "\"prefix/$appName/{{noscript /}}/";
        object.setStringValue("linkPrefix", originalLinkPrefix);
        object.set("scope", "WIKI+ALL_SPACES", this.context);
        DocumentReference userReference = new DocumentReference(WIKI_NAME, SPACE_NAME, "Admin");
        mySectionDoc.getAuthors().setEffectiveMetadataAuthor(new DocumentUserReference(userReference, true));
        this.xwiki.saveDocument(mySectionDoc, this.context);
        when(this.oldcore.getMockDocumentAuthorizationManager().hasAccess(Right.SCRIPT, EntityType.DOCUMENT,
            userReference, mySectionDoc.getDocumentReference())).thenReturn(hasScript);

        Document htmlPage = renderHTMLPage(CONFIGURABLE_CLASS);
        verify(this.oldcore.getMockDocumentAuthorizationManager()).hasAccess(Right.SCRIPT, EntityType.DOCUMENT,
            userReference, MY_SECTION);

        String expectedHeading;
        String expectedLink;
        if (hasScript) {
            expectedHeading = String.format("%s {{noscript /}}", MY_SECTION_SERIALIZED);
            expectedLink = String.format("\"prefix/%s/{{noscript /}}/test", MY_SECTION_SERIALIZED);
            verify(this.authorExecutor).call(any(), eq(userReference), eq(MY_SECTION));
            verify(this.velocityEngine).evaluate(any(), any(), any(), eq(originalHeading));
            verify(this.velocityEngine).evaluate(any(), any(), any(), eq(originalLinkPrefix));
        } else {
            expectedHeading = originalHeading;
            expectedLink = originalLinkPrefix + "test";
            verify(this.velocityEngine, never()).evaluate(any(), any(), any(), eq(originalHeading));
            verify(this.velocityEngine, never()).evaluate(any(), any(), any(), eq(originalLinkPrefix));
        }

        assertEquals(expectedHeading, Objects.requireNonNull(htmlPage.selectFirst("h2")).text());
        assertEquals(expectedLink, Objects.requireNonNull(htmlPage.selectFirst("a")).attr("href"));
    }

    @Test
    void escapeNonViewableSections() throws Exception
    {
        // Create a new section document.
        XWikiDocument mySectionDoc = new XWikiDocument(MY_SECTION);
        this.xwiki.saveDocument(mySectionDoc, this.context);

        when(this.oldcore.getMockRightService()
            .hasAccessLevel(eq("view"), any(), eq("xwiki:" + MY_SECTION_SERIALIZED), any())).thenReturn(false);

        // Make sure the section document is returned by the query.
        when(this.query.execute()).thenReturn(List.of(MY_SECTION_SERIALIZED)).thenReturn(List.of());

        DocumentReference docRef = new DocumentReference(WIKI_NAME, "\">{{/html}}{{noscript /}}", WEB_HOME);
        XWikiDocument contextDoc = new XWikiDocument(docRef);
        this.xwiki.saveDocument(contextDoc, this.context);
        this.context.setDoc(contextDoc);

        XWikiDocument doc = loadPage(CONFIGURABLE_CLASS);
        Document htmlPage = renderHTMLPage(doc);
        String errorMessage = Objects.requireNonNull(htmlPage.selectFirst("div.errormessage p")).text();
        assertEquals(String.format("xe.admin.configurable.noViewAccessSomeApplications [[%s]]", MY_SECTION_SERIALIZED),
            errorMessage);
    }

    @Test
    void escapeSectionLink() throws Exception
    {
        // Create a new section document.
        XWikiDocument mySectionDoc = new XWikiDocument(MY_SECTION);
        BaseObject object = mySectionDoc.newXObject(CONFIGURABLE_CLASS, this.context);
        object.setStringValue("displayInCategory", "other");
        object.setStringValue("displayInSection", "other");
        object.set("scope", "WIKI+ALL_SPACES", this.context);
        this.xwiki.saveDocument(mySectionDoc, this.context);

        // Make sure the section document is returned by the query and the user has access to edit.
        when(this.query.execute()).thenReturn(List.of(MY_SECTION_SERIALIZED)).thenReturn(List.of());
        when(this.oldcore.getMockRightService()
            .hasAccessLevel(eq("edit"), any(), any(), any())).thenReturn(true);

        // Set a new document with space ">{{/html}}{{noscript /}} as context document to check escaping of the
        // current space.
        String spaceName = "\">{{/html}}{{noscript /}}";
        DocumentReference docRef = new DocumentReference(WIKI_NAME, spaceName, WEB_HOME);
        XWikiDocument contextDoc = new XWikiDocument(docRef);
        this.xwiki.saveDocument(contextDoc, this.context);
        this.context.setDoc(contextDoc);

        XWikiDocument doc = loadPage(CONFIGURABLE_CLASS);
        Document htmlPage = renderHTMLPage(doc);
        String link = Objects.requireNonNull(htmlPage.selectFirst("li.other a")).attr("href");
        URI uri = new URI(link);
        // Parse the query parameters and check the space name.
        URLEncodedUtils.parse(uri, StandardCharsets.UTF_8).stream()
            .filter(pair -> pair.getName().equals("space"))
            .findFirst()
            .ifPresentOrElse(pair -> assertEquals(spaceName, pair.getValue()), () -> fail("No space parameter in URL"));
    }

    @Test
    void escapeClassNameForMissingObject() throws Exception
    {
        // Create a new document named "/}}{{noscript /}}.WebHome
        String spaceName = "\"/}}{{noscript /}}";
        String referenceSerialized = spaceName + "." + WEB_HOME;
        DocumentReference documentReference = new DocumentReference(WIKI_NAME, spaceName, WEB_HOME);
        XWikiDocument doc = new XWikiDocument(documentReference);
        doc.getXClass().addTextField("field", "My Field", 30);
        BaseObject object = doc.newXObject(CONFIGURABLE_CLASS, this.context);
        object.setStringValue("displayInCategory", "other");
        object.setStringValue("displayInSection", "other");
        object.setStringValue("scope", "WIKI+ALL_SPACES");
        object.setStringValue("configurationClass", referenceSerialized);
        this.xwiki.saveDocument(doc, this.context);

        when(this.query.execute()).thenReturn(List.of(referenceSerialized)).thenReturn(List.of());
        when(this.oldcore.getMockRightService()
            .hasAccessLevel(eq("edit"), any(), any(), any())).thenReturn(true);

        this.request.put("section", "other");

        XWikiDocument contextDoc = new XWikiDocument(new DocumentReference(WIKI_NAME, SPACE_NAME, WEB_HOME));
        this.xwiki.saveDocument(contextDoc, this.context);
        this.context.setDoc(contextDoc);

        XWikiDocument configurableClassDoc = loadPage(CONFIGURABLE_CLASS);
        Document htmlPage = renderHTMLPage(configurableClassDoc);

        String expectedMessage =
            String.format("xe.admin.configurable.noObjectOfConfigurationClassFound [%s, %s]", referenceSerialized,
                referenceSerialized);
        assertEquals(expectedMessage, Objects.requireNonNull(htmlPage.selectFirst("div.errormessage")).text());
    }
}
