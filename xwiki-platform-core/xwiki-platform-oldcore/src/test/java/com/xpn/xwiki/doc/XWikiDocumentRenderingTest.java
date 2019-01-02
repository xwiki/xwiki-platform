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

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.junit.Test;
import org.xwiki.display.internal.DisplayConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.internal.MockConfigurationSource;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityFactory;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Unit tests for {@link XWikiDocument}.
 *
 * @version $Id$
 */
public class XWikiDocumentRenderingTest extends AbstractBridgedXWikiComponentTestCase
{
    private static final String DOCWIKI = "xwiki";

    private static final String DOCSPACE = "Space";

    private static final String DOCNAME = "Page";

    private static final String DOCFULLNAME = DOCSPACE + "." + DOCNAME;

    private static final String CLASSNAME = DOCFULLNAME;

    private XWikiDocument document;

    private XWikiDocument translatedDocument;

    private Mock mockXWiki;

    private Mock mockXWikiVersioningStore;

    private Mock mockXWikiStoreInterface;

    private Mock mockXWikiRightService;

    private Mock mockDisplayConfiguration;

    private BaseClass baseClass;

    private BaseObject baseObject;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        // Mock xwiki.cfg
        getComponentManager().registerComponent(MockConfigurationSource.getDescriptor("xwikicfg"),
            getConfigurationSource());

        DocumentReference documentReference = new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME);
        this.document = new XWikiDocument(documentReference);
        this.document.setSyntax(Syntax.XWIKI_2_1);
        this.document.setLanguage("en");
        this.document.setDefaultLanguage("en");
        this.document.setNew(false);

        getContext().setDoc(this.document);

        this.translatedDocument = new XWikiDocument();
        this.translatedDocument.setSyntax(Syntax.XWIKI_2_1);
        this.translatedDocument.setLanguage("fr");
        this.translatedDocument.setNew(false);

        getContext().put("isInRenderingEngine", true);

        this.mockXWiki = mock(XWiki.class);
        this.mockXWiki.stubs().method("Param").will(returnValue(null));

        this.mockXWikiVersioningStore = mock(XWikiVersioningStoreInterface.class);
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(returnValue(null));

        this.mockXWikiRightService = mock(XWikiRightService.class);
        this.mockXWikiRightService.stubs().method("hasProgrammingRights").will(returnValue(true));

        this.mockXWikiStoreInterface = mock(XWikiStoreInterface.class);
        this.mockXWikiStoreInterface.stubs().method("search").will(returnValue(new ArrayList<XWikiDocument>()));

        this.document.setStore((XWikiStoreInterface) this.mockXWikiStoreInterface.proxy());

        this.mockXWiki.stubs().method("getVersioningStore").will(returnValue(this.mockXWikiVersioningStore.proxy()));
        this.mockXWiki.stubs().method("getStore").will(returnValue(this.mockXWikiStoreInterface.proxy()));
        this.mockXWiki.stubs().method("getRightService").will(returnValue(this.mockXWikiRightService.proxy()));
        this.mockXWiki.stubs().method("getDocument").will(returnValue(this.document));
        this.mockXWiki.stubs().method("getDocumentReference").will(returnValue(documentReference));
        this.mockXWiki.stubs().method("getLanguagePreference").will(returnValue("en"));
        this.mockXWiki.stubs().method("exists").will(returnValue(false));
        // Called from MessageToolVelocityContextInitializer.
        this.mockXWiki.stubs().method("prepareResources");
        // The next 3 stubs are needed to properly initialize the Velocity engine.
        this.mockXWiki.stubs().method("getSkin").will(returnValue("default"));
        this.mockXWiki.stubs().method("getSkinFile").will(returnValue(null));
        this.mockXWiki.stubs().method("Param").with(eq("xwiki.render.velocity.macrolist")).will(returnValue(""));
        this.mockXWiki.stubs().method("exists").will(returnValue(false));
        this.mockXWiki.stubs().method("evaluateTemplate").will(returnValue(""));

        getContext().setWiki((XWiki) this.mockXWiki.proxy());

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

        this.mockXWiki.stubs().method("getXClass").will(returnValue(this.baseClass));

        this.baseObject = this.document.newObject(CLASSNAME, getContext());
        this.baseObject.setStringValue("string", "string");
        this.baseObject.setLargeStringValue("area", "area");
        this.baseObject.setStringValue("passwd", "passwd");
        this.baseObject.setIntValue("boolean", 1);
        this.baseObject.setIntValue("int", 42);
        this.baseObject.setStringListValue("stringlist", Arrays.asList("VALUE1", "VALUE2"));
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // Setup display configuration.
        this.mockDisplayConfiguration = registerMockComponent(DisplayConfiguration.class);
        this.mockDisplayConfiguration.stubs().method("getDocumentDisplayerHint").will(returnValue("default"));
        this.mockDisplayConfiguration.stubs().method("getTitleHeadingDepth").will(returnValue(2));
    }

    public void testCurrentDocumentVariableIsInjectedBeforeRendering() throws XWikiException
    {
        // Verifies we can access the doc variable from a groovy macro.
        this.document.setContent("{{groovy}}print(doc);{{/groovy}}");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("<p>Space.Page</p>", this.document.getRenderedContent(getContext()));
    }

    public void testGetRenderedTitleWithTitle()
    {
        this.document.setSyntax(Syntax.XWIKI_2_0);

        this.document.setTitle("title");

        assertEquals("title", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setTitle("**title**");

        assertEquals("**title**", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setTitle("<strong>ti<em>tle</strong>");

        // The title is parsed as plain text after the Velocity code is evaluated so the HTML have no meaning.
        assertEquals("&lt;strong&gt;ti&lt;em&gt;tle&lt;/strong&gt;",
            this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));
        assertEquals("<strong>ti<em>tle</strong>", this.document.getRenderedTitle(Syntax.PLAIN_1_0, getContext()));

        this.document.setTitle("#set($key = \"title\")$key");

        assertEquals("title", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));
    }

    public void testGetRenderedTitleWithoutTitleHTML()
    {
        this.document.setSyntax(Syntax.XWIKI_2_0);

        this.document.setContent(
            "content not in section\n" + "= header 1=\nheader 1 content\n" + "== header 2==\nheader 2 content");

        getConfigurationSource().setProperty("xwiki.title.compatibility", "1");

        assertEquals("header 1", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setContent(
            "content not in section\n" + "= **header 1**=\nheader 1 content\n" + "== header 2==\nheader 2 content");

        assertEquals("<strong>header 1</strong>", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setContent(
            "content not in section\n" + "= [[Space.Page]]=\nheader 1 content\n" + "== header 2==\nheader 2 content");

        this.mockXWiki.stubs().method("getURL").will(returnValue("/reference"));

        assertEquals("<span class=\"wikicreatelink\"><a href=\"/reference\"><span class=\"wikigeneratedlinkcontent\">"
            + "Page" + "</span></a></span>", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setContent("content not in section\n" + "= #set($var ~= \"value\")=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("#set($var = \"value\")", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setContent("content not in section\n"
            + "= {{groovy}}print \"value\"{{/groovy}}=\nheader 1 content\n" + "== header 2==\nheader 2 content");

        assertEquals("value", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));

        this.document.setContent("content not in section\n=== header 3===");

        assertEquals("Page", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));
    }

    public void testGetRenderedTitleWithoutTitlePLAIN()
    {
        this.document.setSyntax(Syntax.XWIKI_2_0);

        this.document.setContent(
            "content not in section\n" + "= **header 1**=\nheader 1 content\n" + "== header 2==\nheader 2 content");

        assertEquals("Page", this.document.getRenderedTitle(Syntax.PLAIN_1_0, getContext()));

        getConfigurationSource().setProperty("xwiki.title.compatibility", "1");

        this.document.setContent(
            "content not in section\n" + "= **header 1**=\nheader 1 content\n" + "== header 2==\nheader 2 content");

        assertEquals("header 1", this.document.getRenderedTitle(Syntax.PLAIN_1_0, getContext()));

        this.document.setContent("content not in section\n"
            + "= {{groovy}}print \"value\"{{/groovy}}=\nheader 1 content\n" + "== header 2==\nheader 2 content");

        assertEquals("value", this.document.getRenderedTitle(Syntax.PLAIN_1_0, getContext()));
    }

    public void testGetRenderedTitleNoTitleAndContent()
    {
        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("Page", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));
    }

    /**
     * Make sure title extracted from content is protected from cycles
     */
    public void testGetRenderedTitleRecursive()
    {
        this.document.setSyntax(Syntax.XWIKI_2_0);
        this.document.setContent("= {{groovy}}print doc.getDisplayTitle(){{/groovy}}");

        assertEquals("Page", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));
    }

    /**
     * See XWIKI-5277 for details.
     */
    public void testGetRenderedContentCleansVelocityMacroCache() throws Exception
    {
        // Make sure we start not in the rendering engine since this is what happens in real: a document is
        // called by a template thus outside of the rendering engine.
        getContext().remove("isInRenderingEngine");

        // We display a text area since we know that rendering a text area will call getRenderedContent inside our top
        // level getRenderedContent call, thus testing that velocity macros are not removed during nested calls to
        // getRenderedContent.
        this.baseObject.setLargeStringValue("area", "{{velocity}}#macro(testmacrocache)ok#end{{/velocity}}");
        this.document.setContent("{{velocity}}$doc.display(\"area\")#testmacrocache{{/velocity}}");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        // We need to put the current doc in the Velocity Context since it's normally set before the rendering is
        // called in the execution flow.
        VelocityManager originalVelocityManager = getComponentManager().getInstance(VelocityManager.class);
        VelocityContext vcontext = originalVelocityManager.getVelocityContext();
        vcontext.put("doc", new Document(this.document, getContext()));

        // Register a Mock for the VelocityManager to bypass skin APIs that we would need to mock otherwise.
        Mock mockVelocityManager = registerMockComponent(VelocityManager.class);
        mockVelocityManager.stubs().method("getCurrentVelocityContext").will(returnValue(vcontext));

        VelocityFactory velocityFactory = getComponentManager().getInstance(VelocityFactory.class);
        VelocityEngine vengine = velocityFactory.createVelocityEngine("default", new Properties());

        mockVelocityManager.stubs().method("getVelocityEngine").will(returnValue(vengine));
        mockVelocityManager.stubs().method("evaluate").will(new CustomStub("evaluate")
        {
            @Override
            public Object invoke(Invocation invocation) throws Throwable
            {
                return vengine.evaluate(vcontext, (Writer) invocation.parameterValues.get(0),
                    (String) invocation.parameterValues.get(1), (Reader) invocation.parameterValues.get(2));
            }
        });

        // $doc.display will ask for the syntax of the current document so we need to mock it.
        this.mockXWiki.stubs().method("getCurrentContentSyntaxId").with(eq("xwiki/2.0"), ANYTHING)
            .will(returnValue("xwiki/2.0"));

        // Verify that the macro located inside the TextArea has been taken into account when executing the doc's
        // content.
        assertEquals("<p>ok</p>", this.document.getRenderedContent(getContext()));
    }

    @Test
    public void testGetRenderedTitleWhenMatchingTitleHeaderDepth()
    {
        this.document.setContent("=== level3");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        getConfigurationSource().setProperty("xwiki.title.compatibility", "1");

        // Overwrite the title heading depth.
        this.mockDisplayConfiguration.stubs().method("getTitleHeadingDepth").will(returnValue(3));

        assertEquals("level3", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));
    }

    @Test
    public void testGetRenderedTitleWhenNotMatchingTitleHeaderDepth()
    {
        this.document.setContent("=== level3");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("Page", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));
    }

    public void testGetRenderedContent() throws XWikiException
    {
        this.document.setContent("**bold**");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("<p><strong>bold</strong></p>", this.document.getRenderedContent(getContext()));

        this.translatedDocument = new XWikiDocument(this.document.getDocumentReference(), Locale.FRENCH);
        this.translatedDocument.setContent("//italic//");
        this.translatedDocument.setSyntax(Syntax.XWIKI_1_0);
        this.translatedDocument.setNew(false);

        this.mockXWiki.stubs().method("getLanguagePreference").will(returnValue(Locale.FRENCH.toString()));
        this.mockXWiki.stubs().method("getDocument").with(eq(
            new DocumentReference(this.translatedDocument.getDocumentReference(), this.translatedDocument.getLocale())),
            ANYTHING).will(returnValue(this.translatedDocument));

        assertEquals("<p><em>italic</em></p>", this.document.getRenderedContent(getContext()));
    }

    public void testGetRenderedContentRights() throws XWikiException
    {
        getContext().remove("sdoc");

        this.document.setContent("{{velocity}}$xcontext.sdoc{{/velocity}}");
        this.document.setSyntax(Syntax.XWIKI_2_1);

        assertEquals("<p>Space.Page</p>", this.document.getRenderedContent(getContext()));
    }

    public void testGetRenderedContentTextWithSourceSyntax()
    {
        this.document.setSyntax(Syntax.XWIKI_1_0);

        assertEquals("<p><strong>bold</strong></p>",
            this.document.getRenderedContent("**bold**", "xwiki/2.0", getContext()));
    }

    public void testGetRenderedContentTextRights() throws XWikiException
    {
        XWikiDocument otherDocument = new XWikiDocument(new DocumentReference("otherwiki", "otherspace", "otherpage"));
        otherDocument.setContentAuthorReference(new DocumentReference("otherwiki", "XWiki", "othercontentauthor"));
        XWikiDocument sdoc = new XWikiDocument(new DocumentReference("callerwiki", "callerspace", "callerpage"));
        sdoc.setContentAuthorReference(new DocumentReference("callerwiki", "XWiki", "calleruser"));
        Document apiDocument = this.document.newDocument(getContext());

        getContext().setDoc(null);

        String content =
            "{{velocity}}" + "$xcontext.sdoc.contentAuthorReference $xcontext.doc $xcontext.doc.contentAuthorReference"
                + "{{/velocity}}";

        this.document.setContentAuthorReference(new DocumentReference("authorwiki", "XWiki", "contentauthor"));

        assertEquals("<p>$xcontext.sdoc.contentAuthorReference Space.Page authorwiki:XWiki.contentauthor</p>",
            this.document.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), getContext()));

        assertEquals("<p>$xcontext.sdoc.contentAuthorReference Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString()));

        assertEquals("<p>$xcontext.sdoc.contentAuthorReference Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), Syntax.XHTML_1_0.toIdString()));

        assertEquals("<p>otherwiki:XWiki.othercontentauthor Space.Page authorwiki:XWiki.contentauthor</p>",
            this.document.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), false, otherDocument,
                getContext()));

        getContext().setDoc(otherDocument);

        assertEquals("<p>$xcontext.sdoc.contentAuthorReference Space.Page authorwiki:XWiki.contentauthor</p>",
            this.document.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), getContext()));

        assertEquals("<p>otherwiki:XWiki.othercontentauthor Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString()));

        assertEquals("<p>otherwiki:XWiki.othercontentauthor Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), Syntax.XHTML_1_0.toIdString()));

        getContext().put("sdoc", sdoc);
        getContext().setDoc(this.document);

        assertEquals("<p>callerwiki:XWiki.calleruser Space.Page authorwiki:XWiki.contentauthor</p>",
            this.document.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), getContext()));

        assertEquals("<p>callerwiki:XWiki.calleruser Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString()));

        assertEquals("<p>callerwiki:XWiki.calleruser Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), Syntax.XHTML_1_0.toIdString()));

        getContext().setDoc(otherDocument);

        assertEquals("<p>$xcontext.sdoc.contentAuthorReference Space.Page authorwiki:XWiki.contentauthor</p>",
            this.document.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), getContext()));

        assertEquals("<p>callerwiki:XWiki.calleruser Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString()));

        assertEquals("<p>callerwiki:XWiki.calleruser Space.Page authorwiki:XWiki.contentauthor</p>",
            apiDocument.getRenderedContent(content, Syntax.XWIKI_2_1.toIdString(), Syntax.XHTML_1_0.toIdString()));

    }
}
