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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.velocity.VelocityContext;
import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.internal.MockConfigurationSource;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DocumentSection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.XWikiMessageTool;

/**
 * Unit tests for {@link XWikiDocument}.
 *
 * @version $Id$
 */
public class XWikiDocumentTest extends AbstractBridgedXWikiComponentTestCase
{
    private static final String DOCWIKI = "Wiki";

    private static final String DOCSPACE = "Space";

    private static final String DOCNAME = "Page";

    private static final String DOCFULLNAME = DOCSPACE + "." + DOCNAME;

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME);

    private static final String CLASSNAME = DOCFULLNAME;

    private static final DocumentReference CLASS_REFERENCE = DOCUMENT_REFERENCE;

    private XWikiDocument document;

    private XWikiDocument translatedDocument;

    private Mock mockXWiki;

    private Mock mockXWikiVersioningStore;

    private Mock mockXWikiStoreInterface;

    private Mock mockXWikiMessageTool;

    private Mock mockXWikiRightService;

    private Mock mockVelocityManager;

    private Mock mockVelocityEngine;

    private CustomStub velocityEngineEvaluateStub;

    private BaseClass baseClass;

    private BaseObject baseObject;

    private BaseObject baseObject2;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        DocumentReference documentReference = new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME);
        this.document = new XWikiDocument(documentReference);
        this.document.setSyntax(Syntax.XWIKI_1_0);
        this.document.setLanguage("en");
        this.document.setDefaultLanguage("en");
        this.document.setNew(false);

        this.translatedDocument = new XWikiDocument();
        this.translatedDocument.setSyntax(Syntax.XWIKI_2_0);
        this.translatedDocument.setLanguage("fr");
        this.translatedDocument.setNew(false);

        getContext().put("isInRenderingEngine", true);

        this.mockXWiki = mock(XWiki.class);

        this.mockXWikiVersioningStore = mock(XWikiVersioningStoreInterface.class);
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(returnValue(null));

        this.mockXWikiStoreInterface = mock(XWikiStoreInterface.class);
        this.document.setStore((XWikiStoreInterface) this.mockXWikiStoreInterface.proxy());

        this.mockXWikiMessageTool = mock(XWikiMessageTool.class,
            new Class[] { ResourceBundle.class, XWikiContext.class }, new Object[] { null, getContext() });
        this.mockXWikiMessageTool.stubs().method("get").will(returnValue("message"));

        this.mockXWikiRightService = mock(XWikiRightService.class);
        this.mockXWikiRightService.stubs().method("hasProgrammingRights").will(returnValue(true));

        this.mockXWiki.stubs().method("getVersioningStore").will(returnValue(this.mockXWikiVersioningStore.proxy()));
        this.mockXWiki.stubs().method("getStore").will(returnValue(this.mockXWikiStoreInterface.proxy()));
        this.mockXWiki.stubs().method("getDocument").will(returnValue(this.document));
        this.mockXWiki.stubs().method("getDocumentReference").will(returnValue(documentReference));
        this.mockXWiki.stubs().method("getLanguagePreference").will(returnValue("en"));
        this.mockXWiki.stubs().method("getSectionEditingDepth").will(returnValue(2L));
        this.mockXWiki.stubs().method("getRightService").will(returnValue(this.mockXWikiRightService.proxy()));
        this.mockXWiki.stubs().method("exists").will(returnValue(false));
        this.mockXWiki.stubs().method("evaluateTemplate").will(returnValue(""));

        getContext().setWiki((XWiki) this.mockXWiki.proxy());
        getContext().put("msg", this.mockXWikiMessageTool.proxy());

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

        this.mockXWiki.stubs().method("getClass").will(returnValue(this.baseClass));
        this.mockXWiki.stubs().method("getXClass").will(returnValue(this.baseClass));

        this.baseObject = this.document.newObject(CLASSNAME, getContext());
        this.baseObject.setStringValue("string", "string");
        this.baseObject.setLargeStringValue("area", "area");
        this.baseObject.setStringValue("passwd", "passwd");
        this.baseObject.setIntValue("boolean", 1);
        this.baseObject.setIntValue("int", 42);
        this.baseObject.setStringListValue("stringlist", Arrays.asList("VALUE1", "VALUE2"));

        this.baseObject2 = this.baseObject.clone();
        this.document.addXObject(this.baseObject2);

        this.mockXWikiStoreInterface.stubs().method("search").will(returnValue(new ArrayList<XWikiDocument>()));
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // Mock xwiki.cfg
        getComponentManager().registerComponent(MockConfigurationSource.getDescriptor("xwikicfg"),
            getConfigurationSource());

        // Setup the mock Velocity engine.
        this.mockVelocityManager = registerMockComponent(VelocityManager.class);
        this.mockVelocityEngine = mock(VelocityEngine.class);
        this.mockVelocityManager.stubs().method("getVelocityContext").will(returnValue(null));
        this.mockVelocityManager.stubs().method("getVelocityEngine").will(returnValue(this.mockVelocityEngine.proxy()));
        velocityEngineEvaluateStub = new CustomStub("Implements VelocityEngine.evaluate")
        {
            @Override
            public Object invoke(Invocation invocation) throws Throwable
            {
                // Output the given text without changes.
                StringWriter writer = (StringWriter) invocation.parameterValues.get(1);
                String text = (String) invocation.parameterValues.get(3);
                writer.append(text);
                return true;
            }
        };
        this.mockVelocityEngine.stubs().method("evaluate").will(velocityEngineEvaluateStub);
        this.mockVelocityEngine.stubs().method("startedUsingMacroNamespace");
        this.mockVelocityEngine.stubs().method("stoppedUsingMacroNamespace");
    }

    public void testGetUniqueLinkedPages10()
    {
        XWikiDocument contextDocument =
            new XWikiDocument(new DocumentReference("contextdocwiki", "contextdocspace", "contextdocpage"));
        getContext().setDoc(contextDocument);

        this.mockXWiki.stubs().method("exists").will(returnValue(true));

        this.document.setContent("[TargetPage][TargetLabel>TargetPage][TargetSpace.TargetPage]"
            + "[TargetLabel>TargetSpace.TargetPage?param=value#anchor][http://externallink][mailto:mailto][label>]");

        Set<String> linkedPages = this.document.getUniqueLinkedPages(getContext());

        assertEquals(new HashSet<String>(Arrays.asList("TargetPage", "TargetSpace.TargetPage")),
            new HashSet<String>(linkedPages));
    }

    public void testGetUniqueLinkedPages()
    {
        XWikiDocument contextDocument =
            new XWikiDocument(new DocumentReference("contextdocwiki", "contextdocspace", "contextdocpage"));
        getContext().setDoc(contextDocument);

        this.document.setContent("[[TargetPage]][[TargetLabel>>TargetPage]][[TargetSpace.TargetPage]]"
            + "[[TargetLabel>>TargetSpace.TargetPage?param=value#anchor]][[http://externallink]][[mailto:mailto]]"
            + "[[]][[#anchor]][[?param=value]][[targetwiki:TargetSpace.TargetPage]]");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        Set<String> linkedPages = this.document.getUniqueLinkedPages(getContext());

        assertEquals(new LinkedHashSet<String>(Arrays.asList("Space.TargetPage.WebHome",
            "TargetSpace.TargetPage.WebHome", "targetwiki:TargetSpace.TargetPage.WebHome")), linkedPages);
    }

    public void testGetSections10() throws XWikiException
    {
        this.document.setContent(
            "content not in section\n" + "1 header 1\nheader 1 content\n" + "1.1 header 2\nheader 2 content");

        List<DocumentSection> headers = this.document.getSections();

        assertEquals(2, headers.size());

        DocumentSection header1 = headers.get(0);
        DocumentSection header2 = headers.get(1);

        assertEquals("header 1", header1.getSectionTitle());
        assertEquals(23, header1.getSectionIndex());
        assertEquals(1, header1.getSectionNumber());
        assertEquals("1", header1.getSectionLevel());
        assertEquals("header 2", header2.getSectionTitle());
        assertEquals(51, header2.getSectionIndex());
        assertEquals(2, header2.getSectionNumber());
        assertEquals("1.1", header2.getSectionLevel());
    }

    public void testGetSections() throws XWikiException
    {
        this.document.setContent(
            "content not in section\n" + "= header 1=\nheader 1 content\n" + "== header 2==\nheader 2 content");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        List<DocumentSection> headers = this.document.getSections();

        assertEquals(2, headers.size());

        DocumentSection header1 = headers.get(0);
        DocumentSection header2 = headers.get(1);

        assertEquals("header 1", header1.getSectionTitle());
        assertEquals(-1, header1.getSectionIndex());
        assertEquals(1, header1.getSectionNumber());
        assertEquals("1", header1.getSectionLevel());
        assertEquals("header 2", header2.getSectionTitle());
        assertEquals(-1, header2.getSectionIndex());
        assertEquals(2, header2.getSectionNumber());
        assertEquals("1.1", header2.getSectionLevel());
    }

    public void testGetDocumentSection10() throws XWikiException
    {
        this.document.setContent(
            "content not in section\n" + "1 header 1\nheader 1 content\n" + "1.1 header 2\nheader 2 content");

        DocumentSection header1 = this.document.getDocumentSection(1);
        DocumentSection header2 = this.document.getDocumentSection(2);

        assertEquals("header 1", header1.getSectionTitle());
        assertEquals(23, header1.getSectionIndex());
        assertEquals(1, header1.getSectionNumber());
        assertEquals("1", header1.getSectionLevel());
        assertEquals("header 2", header2.getSectionTitle());
        assertEquals(51, header2.getSectionIndex());
        assertEquals(2, header2.getSectionNumber());
        assertEquals("1.1", header2.getSectionLevel());
    }

    public void testGetDocumentSection() throws XWikiException
    {
        this.document.setContent(
            "content not in section\n" + "= header 1=\nheader 1 content\n" + "== header 2==\nheader 2 content");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        DocumentSection header1 = this.document.getDocumentSection(1);
        DocumentSection header2 = this.document.getDocumentSection(2);

        assertEquals("header 1", header1.getSectionTitle());
        assertEquals(-1, header1.getSectionIndex());
        assertEquals(1, header1.getSectionNumber());
        assertEquals("1", header1.getSectionLevel());
        assertEquals("header 2", header2.getSectionTitle());
        assertEquals(-1, header2.getSectionIndex());
        assertEquals(2, header2.getSectionNumber());
        assertEquals("1.1", header2.getSectionLevel());
    }

    /**
     * Verify that if we have sections nested in groups, they are not taken into account when computing document
     * sections by number. See <a href="https://jira.xwiki.org/browse/XWIKI-6195">XWIKI-6195</a>.
     *
     * @since 5.0M1
     */
    public void testGetDocumentSectionWhenSectionInGroups() throws XWikiException
    {
        this.document.setContent(
            "= Heading1 =\n" + "para1\n" + "== Heading2 ==\n" + "para2\n" + "(((\n" + "== Heading3 ==\n" + "para3\n"
                + "(((\n" + "== Heading4 ==\n" + "para4\n" + ")))\n" + ")))\n" + "== Heading5 ==\n" + "para5\n");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        DocumentSection section = this.document.getDocumentSection(3);
        assertEquals("Heading5", section.getSectionTitle());
    }

    public void testGetContentOfSection10() throws XWikiException
    {
        this.document.setContent(
            "content not in section\n" + "1 header 1\nheader 1 content\n" + "1.1 header 2\nheader 2 content");

        String content1 = this.document.getContentOfSection(1);
        String content2 = this.document.getContentOfSection(2);

        assertEquals("1 header 1\nheader 1 content\n1.1 header 2\nheader 2 content", content1);
        assertEquals("1.1 header 2\nheader 2 content", content2);
    }

    public void testGetContentOfSection() throws XWikiException
    {
        this.document.setContent(
            "content not in section\n" + "= header 1=\nheader 1 content\n" + "== header 2==\nheader 2 content\n"
                + "=== header 3===\nheader 3 content\n" + "== header 4==\nheader 4 content");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        String content1 = this.document.getContentOfSection(1);
        String content2 = this.document.getContentOfSection(2);
        String content3 = this.document.getContentOfSection(3);

        assertEquals("= header 1 =\n\nheader 1 content\n\n== header 2 ==\n\nheader 2 content\n\n"
            + "=== header 3 ===\n\nheader 3 content\n\n== header 4 ==\n\nheader 4 content", content1);
        assertEquals("== header 2 ==\n\nheader 2 content\n\n=== header 3 ===\n\nheader 3 content", content2);
        assertEquals("== header 4 ==\n\nheader 4 content", content3);

        // Validate that third level header is not skipped anymore
        this.mockXWiki.stubs().method("getSectionEditingDepth").will(returnValue(3L));

        content3 = this.document.getContentOfSection(3);
        String content4 = this.document.getContentOfSection(4);

        assertEquals("=== header 3 ===\n\nheader 3 content", content3);
        assertEquals("== header 4 ==\n\nheader 4 content", content4);
    }

    public void testSectionSplit10() throws XWikiException
    {
        List<DocumentSection> sections;
        // Simple test
        this.document.setContent("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
            + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
        sections = this.document.getSections();
        assertEquals(3, sections.size());
        assertEquals("Section 1", sections.get(0).getSectionTitle());
        assertEquals(
            "1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n" + "Content of second section\n",
            this.document.getContentOfSection(1));
        assertEquals("1.1", sections.get(1).getSectionLevel());
        assertEquals("1.1 Subsection 2\nContent of second section\n", this.document.getContentOfSection(2));
        assertEquals(3, sections.get(2).getSectionNumber());
        assertEquals(80, sections.get(2).getSectionIndex());
        assertEquals("1 Section 3\nContent of section 3", this.document.getContentOfSection(3));
        // Test comments don't break the section editing
        this.document.setContent("1 Section 1\n" + "Content of first section\n" + "## 1.1 Subsection 2\n"
            + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
        sections = this.document.getSections();
        assertEquals(2, sections.size());
        assertEquals("Section 1", sections.get(0).getSectionTitle());
        assertEquals("1", sections.get(1).getSectionLevel());
        assertEquals(2, sections.get(1).getSectionNumber());
        assertEquals(83, sections.get(1).getSectionIndex());
        // Test spaces are ignored
        this.document.setContent("1 Section 1\n" + "Content of first section\n" + "   1.1    Subsection 2  \n"
            + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
        sections = this.document.getSections();
        assertEquals(3, sections.size());
        assertEquals("Subsection 2  ", sections.get(1).getSectionTitle());
        assertEquals("1.1", sections.get(1).getSectionLevel());
        // Test lower headings are ignored
        this.document.setContent("1 Section 1\n" + "Content of first section\n" + "1.1.1 Lower subsection\n"
            + "This content is not important\n" + "   1.1    Subsection 2  \n" + "Content of second section\n"
            + "1 Section 3\n" + "Content of section 3");
        sections = this.document.getSections();
        assertEquals(3, sections.size());
        assertEquals("Section 1", sections.get(0).getSectionTitle());
        assertEquals("Subsection 2  ", sections.get(1).getSectionTitle());
        assertEquals("1.1", sections.get(1).getSectionLevel());
        // Test blank lines are preserved
        this.document
            .setContent("\n\n1 Section 1\n\n\n" + "Content of first section\n\n\n" + "   1.1    Subsection 2  \n\n"
                + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
        sections = this.document.getSections();
        assertEquals(3, sections.size());
        assertEquals(2, sections.get(0).getSectionIndex());
        assertEquals("Subsection 2  ", sections.get(1).getSectionTitle());
        assertEquals(43, sections.get(1).getSectionIndex());
    }

    public void testUpdateDocumentSection10() throws XWikiException
    {
        List<DocumentSection> sections;
        // Fill the document
        this.document.setContent("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
            + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
        String content = this.document.updateDocumentSection(3, "1 Section 3\n" + "Modified content of section 3");
        assertEquals("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
            + "Content of second section\n" + "1 Section 3\n" + "Modified content of section 3", content);
        this.document.setContent(content);
        sections = this.document.getSections();
        assertEquals(3, sections.size());
        assertEquals("Section 1", sections.get(0).getSectionTitle());
        assertEquals(
            "1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n" + "Content of second section\n",
            this.document.getContentOfSection(1));
        assertEquals("1.1", sections.get(1).getSectionLevel());
        assertEquals("1.1 Subsection 2\nContent of second section\n", this.document.getContentOfSection(2));
        assertEquals(3, sections.get(2).getSectionNumber());
        assertEquals(80, sections.get(2).getSectionIndex());
        assertEquals("1 Section 3\nModified content of section 3", this.document.getContentOfSection(3));
    }

    public void testUpdateDocumentSection() throws XWikiException
    {
        this.document.setContent(
            "content not in section\n" + "= header 1=\nheader 1 content\n" + "== header 2==\nheader 2 content");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        // Modify section content
        String content1 = this.document.updateDocumentSection(2, "== header 2==\nmodified header 2 content");

        assertEquals(
            "content not in section\n\n= header 1 =\n\nheader 1 content\n\n== header 2 ==\n\nmodified header 2 content",
            content1);

        String content2 = this.document.updateDocumentSection(1,
            "= header 1 =\n\nmodified also header 1 content\n\n== header 2 ==\n\nheader 2 content");

        assertEquals(
            "content not in section\n\n= header 1 =\n\nmodified also header 1 content\n\n== header 2 ==\n\nheader 2 content",
            content2);

        // Remove a section
        String content3 = this.document.updateDocumentSection(2, "");

        assertEquals("content not in section\n\n= header 1 =\n\nheader 1 content", content3);
    }

    public void testDisplay()
    {
        this.mockXWiki.stubs().method("getCurrentContentSyntaxId").will(returnValue("xwiki/2.0"));

        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("string", this.document.display("string", "view", getContext()));
        assertEquals(
            "{{html clean=\"false\" wiki=\"false\"}}<input size='30' id='Space.Page_0_string' value='string' name='Space.Page_0_string' type='text'/>{{/html}}",
            this.document.display("string", "edit", getContext()));

        assertEquals("{{html clean=\"false\" wiki=\"false\"}}<p>area</p>{{/html}}",
            this.document.display("area", "view", getContext()));
    }

    public void testDisplay1020()
    {
        this.mockXWiki.stubs().method("getCurrentContentSyntaxId").will(returnValue("xwiki/1.0"));

        XWikiDocument doc10 = new XWikiDocument();
        doc10.setSyntax(Syntax.XWIKI_1_0);
        getContext().setDoc(doc10);

        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("string", this.document.display("string", "view", getContext()));
        assertEquals(
            "{pre}<input size='30' id='Space.Page_0_string' value='string' name='Space.Page_0_string' type='text'/>{/pre}",
            this.document.display("string", "edit", getContext()));

        assertEquals("<p>area</p>", this.document.display("area", "view", getContext()));
    }

    public void testDisplayTemplate20()
    {
        this.mockXWiki.stubs().method("getCurrentContentSyntaxId").will(returnValue("xwiki/2.0"));

        getContext().put("isInRenderingEngine", false);

        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("string", this.document.display("string", "view", getContext()));
        assertEquals(
            "<input size='30' id='Space.Page_0_string' value='string' name='Space.Page_0_string' type='text'/>",
            this.document.display("string", "edit", getContext()));

        assertEquals("<p>area</p>", this.document.display("area", "view", getContext()));
    }

    public void testConvertSyntax() throws XWikiException
    {
        this.document.setSyntax(Syntax.HTML_4_01);
        this.document.setContent("<p>content not in section</p>" + "<h1>header 1</h1><p>header 1 content</p>"
            + "<h2>header 2</h2><p>header 2 content</p>");
        this.baseObject.setLargeStringValue("area",
            "<p>object content not in section</p>" + "<h1>object header 1</h1><p>object header 1 content</p>"
                + "<h2>object header 2</h2><p>object header 2 content</p>");
        this.baseObject.setLargeStringValue("puretextarea",
            "<p>object content not in section</p>" + "<h1>object header 1</h1><p>object header 1 content</p>"
                + "<h2>object header 2</h2><p>object header 2 content</p>");

        this.document.convertSyntax("xwiki/2.0", getContext());

        assertEquals("content not in section\n\n" + "= header 1 =\n\nheader 1 content\n\n"
            + "== header 2 ==\n\nheader 2 content", this.document.getContent());
        assertEquals("object content not in section\n\n" + "= object header 1 =\n\nobject header 1 content\n\n"
            + "== object header 2 ==\n\nobject header 2 content", this.baseObject.getStringValue("area"));
        assertEquals(
            "<p>object content not in section</p>" + "<h1>object header 1</h1><p>object header 1 content</p>"
                + "<h2>object header 2</h2><p>object header 2 content</p>",
            this.baseObject.getStringValue("puretextarea"));
        assertEquals("xwiki/2.0", this.document.getSyntaxId());
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

    public void testGetRenderedContentWithSourceSyntax()
    {
        this.document.setSyntax(Syntax.XWIKI_1_0);

        assertEquals("<p><strong>bold</strong></p>",
            this.document.getRenderedContent("**bold**", "xwiki/2.0", getContext()));
    }

    public void testRename() throws XWikiException
    {
        // Possible ways to write parents, include documents, or make links:
        // "name" -----means-----> DOCWIKI+":"+DOCSPACE+"."+input
        // "space.name" -means----> DOCWIKI+":"+input
        // "database:space.name" (no change)

        this.document.setContent("[[doc:pageinsamespace]]");
        this.document.setSyntax(Syntax.XWIKI_2_1);
        DocumentReference targetReference = new DocumentReference("newwikiname", "newspace", "newpage");
        XWikiDocument targetDocument = this.document.duplicate(targetReference);
        targetDocument.setStore((XWikiStoreInterface) this.mockXWikiStoreInterface.proxy());

        DocumentReference reference1 = new DocumentReference(DOCWIKI, DOCSPACE, "Page1");
        XWikiDocument doc1 = new XWikiDocument(reference1);
        doc1.setContent("[[doc:" + DOCWIKI + ":" + DOCSPACE + "." + DOCNAME + "]] [[someName>>doc:" + DOCSPACE + "."
            + DOCNAME + "]] [[doc:" + DOCNAME + "]]");
        doc1.setSyntax(Syntax.XWIKI_2_1);
        doc1.setStore((XWikiStoreInterface) this.mockXWikiStoreInterface.proxy());

        DocumentReference reference2 = new DocumentReference("newwikiname", DOCSPACE, "Page2");
        XWikiDocument doc2 = new XWikiDocument(reference2);
        doc2.setContent("[[doc:" + DOCWIKI + ":" + DOCSPACE + "." + DOCNAME + "]]");
        doc2.setSyntax(Syntax.XWIKI_2_1);
        doc2.setStore((XWikiStoreInterface) this.mockXWikiStoreInterface.proxy());

        DocumentReference reference3 = new DocumentReference("newwikiname", "newspace", "Page3");
        XWikiDocument doc3 = new XWikiDocument(reference3);
        doc3.setContent("[[doc:" + DOCWIKI + ":" + DOCSPACE + "." + DOCNAME + "]]");
        doc3.setSyntax(Syntax.XWIKI_2_1);
        doc3.setStore((XWikiStoreInterface) this.mockXWikiStoreInterface.proxy());

        // Test to make sure it also drags children along.
        DocumentReference reference4 = new DocumentReference(DOCWIKI, DOCSPACE, "Page4");
        XWikiDocument doc4 = new XWikiDocument(reference4);
        doc4.setParent(DOCSPACE + "." + DOCNAME);
        doc4.setStore((XWikiStoreInterface) this.mockXWikiStoreInterface.proxy());

        DocumentReference reference5 = new DocumentReference("newwikiname", "newspace", "Page5");
        XWikiDocument doc5 = new XWikiDocument(reference5);
        doc5.setParent(DOCWIKI + ":" + DOCSPACE + "." + DOCNAME);
        doc5.setStore((XWikiStoreInterface) this.mockXWikiStoreInterface.proxy());

        this.mockXWiki.stubs().method("copyDocument").will(returnValue(true));
        this.mockXWiki.stubs().method("getDocument").with(eq(targetReference), ANYTHING)
            .will(returnValue(targetDocument));
        this.mockXWiki.stubs().method("getDocument").with(eq(reference1), ANYTHING).will(returnValue(doc1));
        this.mockXWiki.stubs().method("getDocument").with(eq(reference2), ANYTHING).will(returnValue(doc2));
        this.mockXWiki.stubs().method("getDocument").with(eq(reference3), ANYTHING).will(returnValue(doc3));
        this.mockXWiki.stubs().method("getDocument").with(eq(reference4), ANYTHING).will(returnValue(doc4));
        this.mockXWiki.stubs().method("getDocument").with(eq(reference5), ANYTHING).will(returnValue(doc5));
        this.mockXWiki.stubs().method("saveDocument").isVoid();
        this.mockXWiki.stubs().method("deleteDocument").isVoid();
        this.mockXWikiStoreInterface.stubs().method("getTranslationList").will(returnValue(Arrays.asList()));

        this.document.rename(new DocumentReference("newwikiname", "newspace", "newpage"),
            Arrays.asList(reference1, reference2, reference3), Arrays.asList(reference4, reference5), getContext());

        // Test links
        assertEquals("[[doc:Wiki:Space.pageinsamespace]]", this.document.getContent());
        assertEquals("[[doc:newwikiname:newspace.newpage]] " + "[[someName>>doc:newwikiname:newspace.newpage]] "
            + "[[doc:newwikiname:newspace.newpage]]", doc1.getContent());
        assertEquals("[[doc:newspace.newpage]]", doc2.getContent());
        assertEquals("[[doc:newpage]]", doc3.getContent());

        // Test parents
        assertEquals("newwikiname:newspace.newpage", doc4.getParent());
        assertEquals(new DocumentReference("newwikiname", "newspace", "newpage"), doc5.getParentReference());
    }

    /**
     * Validate rename does not crash when the document has 1.0 syntax (it does not support everything but it does not
     * crash).
     */
    public void testRename10() throws XWikiException
    {
        this.document.setContent("[pageinsamespace]");
        this.document.setSyntax(Syntax.XWIKI_1_0);
        DocumentReference targetReference = new DocumentReference("newwikiname", "newspace", "newpage");
        XWikiDocument targetDocument = this.document.duplicate(targetReference);

        this.mockXWiki.stubs().method("copyDocument").will(returnValue(true));
        this.mockXWiki.stubs().method("getDocument").with(eq(targetReference), ANYTHING)
            .will(returnValue(targetDocument));
        this.mockXWiki.stubs().method("saveDocument").isVoid();
        this.mockXWiki.stubs().method("deleteDocument").isVoid();

        this.document.rename(new DocumentReference("newwikiname", "newspace", "newpage"),
            Collections.<DocumentReference>emptyList(), Collections.<DocumentReference>emptyList(), getContext());

        // Test links
        assertEquals("[pageinsamespace]", this.document.getContent());
    }

    /**
     * @see XWIKI-7515: 'getIncludedPages' in class com.xpn.xwiki.api.Document threw java.lang.NullPointerException
     */
    public void testGetIncludedPages()
    {
        this.document.setSyntax(Syntax.XWIKI_2_1);

        this.document.setContent("no include");
        assertTrue(this.document.getIncludedPages(getContext()).isEmpty());

        this.document.setContent("bad {{include/}}");
        assertTrue(this.document.getIncludedPages(getContext()).isEmpty());

        this.document.setContent("good deprecated {{include document=\"Foo.Bar\"/}}");
        assertEquals(Arrays.asList("Foo.Bar"), this.document.getIncludedPages(getContext()));

        this.document.setContent("good {{include reference=\"One.Two\"/}}");
        assertEquals(Arrays.asList("One.Two"), this.document.getIncludedPages(getContext()));

        this.document.setContent("bad recursive {{include reference=\"\"/}}");
        assertTrue(this.document.getIncludedPages(getContext()).isEmpty());

        this.document.setContent("bad recursive {{include reference=\"" + DOCNAME + "\"/}}");
        assertTrue(this.document.getIncludedPages(getContext()).isEmpty());

        this.document.setContent("bad recursive {{include reference=\"" + DOCSPACE + "." + DOCNAME + "\"/}}");
        assertTrue(this.document.getIncludedPages(getContext()).isEmpty());
    }

    /**
     * XWIKI-8025: XWikiDocument#backup/restoreContext doesn't update the reference to the Velocity context stored on
     * the XWiki context
     */
    public void testBackupRestoreContextUpdatesVContext() throws Exception
    {
        final Execution execution = getComponentManager().getInstance(Execution.class);
        this.mockVelocityManager.stubs().method("getVelocityContext")
            .will(new CustomStub("Implements VelocityManager.getVelocityContext")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    return (VelocityContext) execution.getContext().getProperty("velocityContext");
                }
            });

        VelocityContext oldVelocityContext = new VelocityContext();
        execution.getContext().setProperty("velocityContext", oldVelocityContext);

        Map<String, Object> backup = new HashMap<String, Object>();
        XWikiDocument.backupContext(backup, getContext());

        VelocityContext newVelocityContext = (VelocityContext) execution.getContext().getProperty("velocityContext");
        assertNotNull(newVelocityContext);
        assertNotSame(oldVelocityContext, newVelocityContext);
        assertSame(newVelocityContext, getContext().get("vcontext"));

        XWikiDocument.restoreContext(backup, getContext());

        assertSame(oldVelocityContext, execution.getContext().getProperty("velocityContext"));
        assertSame(oldVelocityContext, getContext().get("vcontext"));
    }
}
