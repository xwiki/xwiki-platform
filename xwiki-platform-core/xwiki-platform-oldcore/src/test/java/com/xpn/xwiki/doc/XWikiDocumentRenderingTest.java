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
package com.xpn.xwiki.doc;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.display.internal.DisplayConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.internal.DefaultVelocityManager;
import org.xwiki.velocity.internal.VelocityExecutionContextInitializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiDocument}'s rendering methods
 * ({@code getRenderedTitle()} and {@code getRenderedContent()}).
 *
 * @version $Id$
 */
@OldcoreTest
@AllComponents
public class XWikiDocumentRenderingTest
{
    private static final String DOCWIKI = "xwiki";

    private static final String DOCSPACE = "Space";

    private static final String DOCNAME = "Page";

    private static final String DOCFULLNAME = DOCSPACE + "." + DOCNAME;

    private static final String CLASSNAME = DOCFULLNAME;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private DisplayConfiguration displayConfiguration;

    private XWikiDocument document;

    private BaseClass baseClass;

    private BaseObject baseObject;

    private XWiki xwiki;

    @BeforeEach
    public void setup() throws Exception
    {
        this.xwiki = this.oldcore.getSpyXWiki();

        // Setup display configuration.
        this.displayConfiguration = this.componentManager.registerMockComponent(DisplayConfiguration.class);
        when(this.displayConfiguration.getDocumentDisplayerHint()).thenReturn("default");
        when(this.displayConfiguration.getTitleHeadingDepth()).thenReturn(2);

        DocumentReference documentReference = new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME);
        this.document = new XWikiDocument(documentReference);
        this.document.setSyntax(Syntax.XWIKI_2_1);
        this.document.setNew(false);

        this.oldcore.getXWikiContext().setDoc(this.document);

        this.baseClass = this.document.getXClass();
        this.baseClass.addTextField("string", "String", 30);
        this.baseClass.addTextAreaField("area", "Area", 10, 10);
        this.baseClass.addTextAreaField("puretextarea", "Pure text area", 10, 10);
        // set the text areas an non interpreted content
        ((TextAreaClass) this.baseClass.getField("puretextarea")).setContentType("puretext");
        this.baseClass.addPasswordField("passwd", "Password", 30);
        this.baseClass.addBooleanField("boolean", "Boolean", "yesno");
        this.baseClass.addNumberField("int", "Int", 10, "integer");
        this.baseClass.addStaticListField("stringlist", "StringList", "value1, value2");

        doReturn(this.baseClass).when(this.xwiki).getXClass(any(), any());

        this.baseObject = this.document.newObject(CLASSNAME, this.oldcore.getXWikiContext());
        this.baseObject.setStringValue("string", "string");
        this.baseObject.setLargeStringValue("area", "area");
        this.baseObject.setStringValue("passwd", "passwd");
        this.baseObject.setIntValue("boolean", 1);
        this.baseObject.setIntValue("int", 42);
        this.baseObject.setStringListValue("stringlist", Arrays.asList("VALUE1", "VALUE2"));

        this.oldcore.getXWikiContext().put("isInRenderingEngine", true);

        when(this.oldcore.getMockAuthorizationManager().hasAccess(any(), any(), any())).thenReturn(true);
        when(this.oldcore.getMockDocumentAuthorizationManager().hasAccess(any(), any(), any(), any())).thenReturn(true);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(any())).thenAnswer(invocationOnMock -> {
            if (List.of(Right.SCRIPT, Right.PROGRAM).contains(invocationOnMock.getArgument(0))) {
                RenderingContext renderingContext = this.oldcore.getMocker().getInstance(RenderingContext.class);
                return !renderingContext.isRestricted();
            } else {
                return true;
            }
        });
        when(this.xwiki.getRightService().hasProgrammingRights(any())).thenReturn(true);

        this.componentManager
            .registerComponent(ConfigurationSource.class, "xwikicfg", this.oldcore.getConfigurationSource());
    }

    @Test
    public void getRenderedContentWithCurrentDocumentVariableIsInjectedBeforeRendering() throws Exception
    {
        // Verifies we can access the doc variable from a groovy macro.
        this.document.setContent("{{groovy}}print(doc);{{/groovy}}");

        assertEquals("<p>Space.Page</p>", this.document.getRenderedContent(this.oldcore.getXWikiContext()));
    }

    @Test
    public void getRenderedTitleWhenTitleIsSet()
    {
        // Plain title
        this.document.setTitle("title");
        assertEquals("title", this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));

        // Title with wiki syntax that should not be evaluated
        this.document.setTitle("**title**");
        assertEquals("**title**", this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));

        // Title with HTML syntax that should not be evaluated
        this.document.setTitle("<strong>ti<em>tle</strong>");
        // The title is parsed as plain text after the Velocity code is evaluated so the HTML have no meaning.
        assertEquals("&lt;strong&gt;ti&lt;em&gt;tle&lt;/strong&gt;",
            this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));
        assertEquals("<strong>ti<em>tle</strong>",
            this.document.getRenderedTitle(Syntax.PLAIN_1_0, this.oldcore.getXWikiContext()));

        // Title with velocity that should be evaluated
        this.document.setTitle("#set($key = \"title\")$key");
        assertEquals("title", this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));
    }

    @Test
    void getRenderedTitleRestricted()
    {
        this.document.setRestricted(true);
        // Title with velocity that shouldn't be evaluated
        String title = "#set($key = \"title\")$key";
        this.document.setTitle(title);
        assertEquals(title, this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));
    }

    @Test
    public void getRenderedTitleInHTMLWhenExtractedFromContent()
    {
        // Configure XWiki to extract title from content
        this.oldcore.getConfigurationSource().setProperty("xwiki.title.compatibility", "1");

        this.document.setContent(
            "content not in section\n" + "= header 1=\nheader 1 content\n" + "== header 2==\nheader 2 content");
        assertEquals("header 1", this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));

        this.document.setContent(
            "content not in section\n" + "= **header 1**=\nheader 1 content\n" + "== header 2==\nheader 2 content");
        assertEquals("<strong>header 1</strong>",
            this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));

        this.document.setContent(
            "content not in section\n" + "= [[Space.Page]]=\nheader 1 content\n" + "== header 2==\nheader 2 content");
        assertEquals("<span class=\"wikiexternallink\"><a href=\"Space.Page\">"
                + "<span class=\"wikigeneratedlinkcontent\">Space.Page</span></a></span>",
            this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));

        this.document.setContent("content not in section\n" + "= #set($var ~= \"value\")=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");
        assertEquals("#set($var = \"value\")",
            this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));

        this.document.setContent("content not in section\n"
            + "= {{groovy}}print \"value\"{{/groovy}}=\nheader 1 content\n" + "== header 2==\nheader 2 content");
        assertEquals("value", this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));

        this.document.setContent("content not in section\n=== header 3===");
        assertEquals("Page", this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));
    }

    @Test
    void getRenderedTitleWhenRestricted()
    {
        // Configure XWiki to extract title from content
        this.oldcore.getConfigurationSource().setProperty("xwiki.title.compatibility", "1");
        this.document.setRestricted(true);

        this.document.setContent("content not in section\n"
            + "= {{groovy}}print \"value\"{{/groovy}}=\nheader 1 content\n" + "== header 2==\nheader 2 content");
        assertThat(this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()),
            startsWith("<span class=\"xwikirenderingerror\">Failed to execute the [groovy] macro."));
    }

    @Test
    public void getRenderedTitleInPlainWhenExtractedFromContent()
    {
        this.document.setContent(
            "content not in section\n" + "= **header 1**=\nheader 1 content\n" + "== header 2==\nheader 2 content");
        assertEquals("Page", this.document.getRenderedTitle(Syntax.PLAIN_1_0, this.oldcore.getXWikiContext()));

        // Configure XWiki to extract title from content
        this.oldcore.getConfigurationSource().setProperty("xwiki.title.compatibility", "1");

        this.document.setContent(
            "content not in section\n" + "= **header 1**=\nheader 1 content\n" + "== header 2==\nheader 2 content");
        assertEquals("header 1", this.document.getRenderedTitle(Syntax.PLAIN_1_0, this.oldcore.getXWikiContext()));

        this.document.setContent("content not in section\n"
            + "= {{groovy}}print \"value\"{{/groovy}}=\nheader 1 content\n" + "== header 2==\nheader 2 content");
        assertEquals("value", this.document.getRenderedTitle(Syntax.PLAIN_1_0, this.oldcore.getXWikiContext()));
    }

    @Test
    public void getRenderedTitleWhenNoTitleAndNoContentSet()
    {
        assertEquals("Page", this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));
    }

    /**
     * Make sure title extracted from content is protected from cycles
     */
    @Test
    public void getRenderedTitleWhenRecursive()
    {
        // Configure XWiki to extract title from content
        this.oldcore.getConfigurationSource().setProperty("xwiki.title.compatibility", "1");

        this.document.setContent("= {{groovy}}print doc.getDisplayTitle(){{/groovy}}");

        assertEquals("Page", this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));
    }

    @Test
    public void getRenderedTitleWhenMatchingTitleHeaderDepth()
    {
        // Configure XWiki to extract title from content
        this.oldcore.getConfigurationSource().setProperty("xwiki.title.compatibility", "1");

        this.document.setContent("=== level3");

        // Overwrite the title heading depth.
        when(this.displayConfiguration.getTitleHeadingDepth()).thenReturn(3);

        assertEquals("level3", this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));
    }

    @Test
    public void getRenderedTitleWhenNotMatchingTitleHeaderDepth()
    {
        this.document.setContent("=== level3");

        assertEquals("Page", this.document.getRenderedTitle(Syntax.XHTML_1_0, this.oldcore.getXWikiContext()));
    }

    /**
     * See XWIKI-5277 for details.
     */
    @Test
    public void getRenderedContentCleansVelocityMacroCache() throws Exception
    {
        // Make sure we start not in the rendering engine since this is what happens in real: a document is
        // called by a template thus outside of the rendering engine.
        this.oldcore.getXWikiContext().remove("isInRenderingEngine");

        // We display a text area since we know that rendering a text area will call getRenderedContent inside our top
        // level getRenderedContent call, thus testing that velocity macros are not removed during nested calls to
        // getRenderedContent.
        this.baseObject.setLargeStringValue("area", "{{velocity}}#macro(testmacrocache)ok#end{{/velocity}}");
        this.document.setContent("{{velocity}}$doc.display(\"area\")#testmacrocache{{/velocity}}");

        // We need to put the current doc in the Velocity Context since it's normally set before the rendering is
        // called in the execution flow.
        VelocityManager originalVelocityManager = this.componentManager.getInstance(VelocityManager.class);
        VelocityContext vcontext = originalVelocityManager.getVelocityContext();
        vcontext.put("doc", new Document(this.document, this.oldcore.getXWikiContext()));

        // Use the commons version of the VelocityManager to bypass skin APIs that we would need to mock otherwise.
        this.componentManager.registerComponent(DefaultVelocityManager.class);
        this.oldcore.getExecutionContext().setProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID,
            vcontext);

        // Verify that the macro located inside the TextArea has been taken into account when executing the doc's
        // content.
        assertEquals("<p>ok</p>", this.document.getRenderedContent(this.oldcore.getXWikiContext()));
    }

    @Test
    public void getRenderedContentWithAndWithoutTranslations() throws Exception
    {
        this.document.setContent("**bold**");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        // Check the content from the default document
        assertEquals("<p><strong>bold</strong></p>", this.document.getRenderedContent(this.oldcore.getXWikiContext()));

        // Create a translation and set the current language to be that of the translation to verify that the rendered
        // content is that of the translated document
        // Note that this also verifies that the translation can have a different syntax than the default doc.
        XWikiDocument translatedDocument = new XWikiDocument(this.document.getDocumentReference(), Locale.FRENCH);
        translatedDocument.setContent("//italic//");
        translatedDocument.setSyntax(Syntax.XWIKI_1_0);
        translatedDocument.setNew(false);

        doReturn(Locale.FRENCH.toString()).when(this.xwiki).getLanguagePreference(any());
        doReturn(translatedDocument).when(this.xwiki).getDocument(
            eq(new DocumentReference(translatedDocument.getDocumentReference(), translatedDocument.getLocale())),
            any());
        assertEquals("<p><em>italic</em></p>", this.document.getRenderedContent(this.oldcore.getXWikiContext()));

        assertEquals("<p><strong>bold</strong></p>", this.document.displayDocument(this.oldcore.getXWikiContext()));
    }

    @Test
    public void getRenderedContentIsForcingCurrentDocumentAsTheSecurityDocument() throws Exception
    {
        // Remove whatever security document there is, to prove that a new security document is forced (it's set as
        // the current document).
        this.oldcore.getXWikiContext().remove("sdoc");

        this.document.setContent("{{velocity}}$xcontext.sdoc{{/velocity}}");

        // Verifies that a security document is always set,  independently of what was set before the execution of
        // getRenderedContent().
        assertEquals("<p>Space.Page</p>", this.document.getRenderedContent(this.oldcore.getXWikiContext()));
    }

    @Test
    void getRenderedContentSetsRestrictedRendering() throws Exception
    {
        XWikiDocument otherDocument = new XWikiDocument(new DocumentReference("otherwiki", "otherspace", "otherpage"));
        otherDocument.setContentAuthorReference(new DocumentReference("otherwiki", "XWiki", "othercontentauthor"));
        XWikiDocument sdoc = new XWikiDocument(new DocumentReference("callerwiki", "callerspace", "callerpage"));
        Document apiDocument = this.document.newDocument(this.oldcore.getXWikiContext());

        String content = "{{velocity}}test{{/velocity}}";

        this.document.setRestricted(true);
        this.document.setContent(content);
        this.oldcore.getXWikiContext().setDoc(null);

        // Verify that the Velocity macro is not executed.
        assertThat(this.document.getRenderedContent(this.oldcore.getXWikiContext()),
            startsWith("<div class=\"xwikirenderingerror\">Failed to execute the [velocity] macro."));

        this.document.setRestricted(false);

        assertEquals("<p>test</p>", this.document.getRenderedContent(this.oldcore.getXWikiContext()));

        this.oldcore.getXWikiContext().setDoc(otherDocument);

        assertEquals("<p>test</p>", apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString()));

        otherDocument.setRestricted(true);

        assertThat(apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString()),
            startsWith("<div class=\"xwikirenderingerror\">Failed to execute the [velocity] macro."));

        this.oldcore.getXWikiContext().put("sdoc", sdoc);
        assertEquals("<p>test</p>", apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString()));

        sdoc.setRestricted(true);

        assertThat(apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString()),
            startsWith("<div class=\"xwikirenderingerror\">Failed to execute the [velocity] macro."));
    }

    @Test
    public void getRenderedContentTextWithSourceSyntaxSpecified()
    {
        this.document.setSyntax(Syntax.XWIKI_1_0);

        assertEquals("<p><strong>bold</strong></p>",
            this.document.getRenderedContent("**bold**", "xwiki/2.0", this.oldcore.getXWikiContext()));
    }

    @Test
    public void getRenderedContentTextRights() throws Exception
    {
        XWikiDocument otherDocument = new XWikiDocument(new DocumentReference("otherwiki", "otherspace", "otherpage"));
        otherDocument.setContentAuthorReference(new DocumentReference("otherwiki", "XWiki", "othercontentauthor"));
        XWikiDocument sdoc = new XWikiDocument(new DocumentReference("callerwiki", "callerspace", "callerpage"));
        sdoc.setContentAuthorReference(new DocumentReference("callerwiki", "XWiki", "calleruser"));
        Document apiDocument = this.document.newDocument(this.oldcore.getXWikiContext());

        this.oldcore.getXWikiContext().setDoc(null);

        String content =
            "{{velocity}}$xcontext.sdoc.contentAuthorReference $xcontext.doc $xcontext.doc.contentAuthorReference"
                + "{{/velocity}}";

        this.document.setContentAuthorReference(new DocumentReference("authorwiki", "XWiki", "contentauthor"));

        assertEquals("<p>$xcontext.sdoc.contentAuthorReference Space.Page authorwiki:XWiki.contentauthor</p>",
            this.document.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), this.oldcore.getXWikiContext()));

        assertEquals("<p>$xcontext.sdoc.contentAuthorReference Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString()));

        assertEquals("<p>$xcontext.sdoc.contentAuthorReference Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), Syntax.XHTML_1_0.toIdString()));

        assertEquals("<p>otherwiki:XWiki.othercontentauthor Space.Page authorwiki:XWiki.contentauthor</p>",
            this.document.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), false, otherDocument,
                this.oldcore.getXWikiContext()));

        this.oldcore.getXWikiContext().setDoc(otherDocument);

        assertEquals("<p>$xcontext.sdoc.contentAuthorReference Space.Page authorwiki:XWiki.contentauthor</p>",
            this.document.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), this.oldcore.getXWikiContext()));

        assertEquals("<p>otherwiki:XWiki.othercontentauthor Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString()));

        assertEquals("<p>otherwiki:XWiki.othercontentauthor Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), Syntax.XHTML_1_0.toIdString()));

        this.oldcore.getXWikiContext().put("sdoc", sdoc);
        this.oldcore.getXWikiContext().setDoc(this.document);

        assertEquals("<p>callerwiki:XWiki.calleruser Space.Page authorwiki:XWiki.contentauthor</p>",
            this.document.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), this.oldcore.getXWikiContext()));

        assertEquals("<p>callerwiki:XWiki.calleruser Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString()));

        assertEquals("<p>callerwiki:XWiki.calleruser Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), Syntax.XHTML_1_0.toIdString()));

        this.oldcore.getXWikiContext().setDoc(otherDocument);

        assertEquals("<p>$xcontext.sdoc.contentAuthorReference Space.Page authorwiki:XWiki.contentauthor</p>",
            this.document.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), this.oldcore.getXWikiContext()));

        assertEquals("<p>callerwiki:XWiki.calleruser Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString()));

        assertEquals("<p>callerwiki:XWiki.calleruser Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), Syntax.XHTML_1_0.toIdString()));
    }
}
