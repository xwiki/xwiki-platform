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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DocumentSection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiDocument}.
 *
 * @version $Id$
 */
@OldcoreTest
@AllComponents
public class XWikiDocumentTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    private static final String DOCWIKI = "Wiki";

    private static final String DOCSPACE = "Space";

    private static final String DOCNAME = "Page";

    private static final String CLASSNAME = DOCSPACE + "." + DOCNAME;

    private static final DocumentReference CLASS_REFERENCE = new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME);

    private XWikiDocument document;

    private XWikiDocument translatedDocument;

    @Mock
    private XWikiMessageTool xWikiMessageTool;

    @Mock
    private XWikiRightService xWikiRightService;

    @Mock
    private XWiki xWiki;

    @Mock
    private VelocityEngine velocityEngine;

    private VelocityManager velocityManager;

    private XWikiStoreInterface xWikiStoreInterface;

    private BaseClass baseClass;

    private BaseObject baseObject;

    private BaseObject baseObject2;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    protected void setUp() throws Exception
    {
        XWikiVersioningStoreInterface mockXWikiVersioningStore =
            this.componentManager.registerMockComponent(XWikiVersioningStoreInterface.class);
        this.xWikiStoreInterface = this.componentManager.registerMockComponent(XWikiStoreInterface.class);
        this.velocityManager = this.componentManager.registerMockComponent(VelocityManager.class);
        this.componentManager.registerMockComponent(ExtendedRenderingConfiguration.class);

        when(this.velocityManager.getVelocityEngine()).thenReturn(this.velocityEngine);

        Answer<Boolean> invocationVelocity = invocationOnMock -> {
            // Output the given text without changes.
            StringWriter writer = invocationOnMock.getArgument(0);
            String text = invocationOnMock.getArgument(2);
            writer.append(text);
            return true;
        };
        when(this.velocityEngine.evaluate(any(), any(), any(), any(String.class))).then(invocationVelocity);

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

        this.oldcore.getXWikiContext().put("isInRenderingEngine", true);
        when(mockXWikiVersioningStore.getXWikiDocumentArchive(any(), any())).thenReturn(null);

        this.document.setStore(this.xWikiStoreInterface);

        when(this.xWikiMessageTool.get(any())).thenReturn("message");
        when(this.xWikiRightService.hasProgrammingRights(any())).thenReturn(true);

        when(this.xWiki.getVersioningStore()).thenReturn(mockXWikiVersioningStore);
        when(this.xWiki.getStore()).thenReturn(xWikiStoreInterface);
        when(this.xWiki.getDocument(any(DocumentReference.class), any())).thenReturn(this.document);
        when(this.xWiki.getDocumentReference(any(XWikiRequest.class), any())).thenReturn(documentReference);
        when(this.xWiki.getDocumentReference(any(EntityReference.class), any()))
            .then(i -> new DocumentReference(i.getArgument(0)));
        when(this.xWiki.getLanguagePreference(any())).thenReturn("en");
        when(this.xWiki.getSectionEditingDepth()).thenReturn(2L);
        when(this.xWiki.getRightService()).thenReturn(this.xWikiRightService);
        when(this.xWiki.exists(any(DocumentReference.class), any())).thenReturn(false);
        when(this.xWiki.evaluateTemplate(any(), any())).thenReturn("");

        this.oldcore.getXWikiContext().setWiki(this.xWiki);
        this.oldcore.getXWikiContext().put("msg", this.xWikiMessageTool);

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

        when(this.xWiki.getClass(any(), any())).thenReturn(this.baseClass);
        when(this.xWiki.getXClass(any(), any())).thenReturn(this.baseClass);

        this.baseObject = this.document.newObject(CLASSNAME, this.oldcore.getXWikiContext());
        this.baseObject.setStringValue("string", "string");
        this.baseObject.setLargeStringValue("area", "area");
        this.baseObject.setStringValue("passwd", "passwd");
        this.baseObject.setIntValue("boolean", 1);
        this.baseObject.setIntValue("int", 42);
        this.baseObject.setStringListValue("stringlist", Arrays.asList("VALUE1", "VALUE2"));

        this.baseObject2 = this.baseObject.clone();
        this.document.addXObject(this.baseObject2);
    }

    @Test
    public void getUniqueLinkedPages10() throws XWikiException
    {
        XWikiDocument contextDocument =
            new XWikiDocument(new DocumentReference("contextdocwiki", "contextdocspace", "contextdocpage"));
        this.oldcore.getXWikiContext().setDoc(contextDocument);

        when(xWiki.exists(any(DocumentReference.class), any())).thenReturn(true);

        this.document.setContent("[TargetPage][TargetLabel>TargetPage][TargetSpace.TargetPage]"
            + "[TargetLabel>TargetSpace.TargetPage?param=value#anchor][http://externallink][mailto:mailto][label>]");

        Set<String> linkedPages = this.document.getUniqueLinkedPages(this.oldcore.getXWikiContext());

        assertEquals(new HashSet<>(Arrays.asList("TargetPage", "TargetSpace.TargetPage")), new HashSet<>(linkedPages));
    }

    @Test
    public void getUniqueLinkedPages20()
    {
        XWikiDocument contextDocument =
            new XWikiDocument(new DocumentReference("contextdocwiki", "contextdocspace", "contextdocpage"));
        this.oldcore.getXWikiContext().setDoc(contextDocument);

        this.document.setContent("[[TargetPage]][[TargetLabel>>TargetPage]][[TargetSpace.TargetPage]]"
            + "[[TargetLabel>>TargetSpace.TargetPage?param=value#anchor]][[http://externallink]][[mailto:mailto]]"
            + "[[]][[#anchor]][[?param=value]][[targetwiki:TargetSpace.TargetPage]]");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        Set<String> linkedPages = this.document.getUniqueLinkedPages(this.oldcore.getXWikiContext());

        assertEquals(new LinkedHashSet<>(Arrays.asList("Space.TargetPage.WebHome",
            "TargetSpace.TargetPage.WebHome", "targetwiki:TargetSpace.TargetPage.WebHome")), linkedPages);
    }

    @Test
    public void getUniqueLinkedPages21() throws Exception
    {
        XWikiDocument contextDocument =
            new XWikiDocument(new DocumentReference("contextdocwiki", "contextdocspace", "contextdocpage"));
        this.oldcore.getXWikiContext().setDoc(contextDocument);

        this.document.setSyntax(Syntax.XWIKI_2_1);
        this.document.setContent(""
            + "[[TargetPage]]"
            + "[[TargetLabel>>TargetPage]]"
            + "[[TargetSpace.TargetPage]]"
            + "[[http://externallink]]"
            + "[[mailto:mailto]]"
            + "[[]]"
            + "[[targetwiki:TargetSpace.TargetPage]]"
            + "[[page:OtherPage]]"
            + "[[attach:AttachSpace.AttachDocument@attachment.ext]]"
            + "[[attach:attachment.ext]]"
            + "[[pageAttach:OtherPage/attachment.ext]]"
            + "image:ImageSpace.ImageDocument@image.png image:image.png");
        this.baseObject.setLargeStringValue("area", "[[TargetPage]][[ObjectTargetPage]]");

        // Simulate that "OtherPage.WebHome" exists
        doReturn(new DocumentReference("Wiki", "OtherPage", "WebHome")).when(this.xWiki)
            .getDocumentReference(new PageReference("Wiki", "OtherPage"), this.oldcore.getXWikiContext());

        Set<String> linkedPages = this.document.getUniqueLinkedPages(this.oldcore.getXWikiContext());

        assertEquals(
            new LinkedHashSet<>(Arrays.asList(
                "Space.TargetPage.WebHome",
                "TargetSpace.TargetPage.WebHome",
                "targetwiki:TargetSpace.TargetPage.WebHome",
                "OtherPage.WebHome",
                "AttachSpace.AttachDocument.WebHome",
                "ImageSpace.ImageDocument.WebHome",
                "Space.ObjectTargetPage.WebHome"
            )), linkedPages);
    }

    @Test
    public void getUniqueWikiLinkedPages() throws XWikiException
    {
        XWikiDocument contextDocument =
            new XWikiDocument(new DocumentReference("contextdocwiki", "contextdocspace", "contextdocpage"));
        this.oldcore.getXWikiContext().setDoc(contextDocument);

        this.document.setContent("[[TargetPage]][[TargetLabel>>TargetPage]][[TargetSpace.TargetPage]]"
            + "[[TargetLabel>>TargetSpace.TargetPage?param=value#anchor]][[http://externallink]][[mailto:mailto]]"
            + "[[]][[#anchor]][[?param=value]][[targetwiki:TargetSpace.TargetPage]]");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        Set<XWikiLink> linkedPages = this.document.getUniqueWikiLinkedPages(this.oldcore.getXWikiContext());
        Set<XWikiLink> expectedLinkedPages = new LinkedHashSet<>();
        XWikiLink xWikiLink = new XWikiLink();
        xWikiLink.setDocId(this.document.getId());
        xWikiLink.setFullName(DOCSPACE + "." + DOCNAME);
        xWikiLink.setLink("Space.TargetPage.WebHome");
        expectedLinkedPages.add(xWikiLink);

        xWikiLink = new XWikiLink();
        xWikiLink.setDocId(this.document.getId());
        xWikiLink.setFullName(DOCSPACE + "." + DOCNAME);
        xWikiLink.setLink("TargetSpace.TargetPage.WebHome");
        expectedLinkedPages.add(xWikiLink);

        xWikiLink = new XWikiLink();
        xWikiLink.setDocId(this.document.getId());
        xWikiLink.setFullName(DOCSPACE + "." + DOCNAME);
        xWikiLink.setLink("targetwiki:TargetSpace.TargetPage.WebHome");
        expectedLinkedPages.add(xWikiLink);

        assertEquals(expectedLinkedPages, linkedPages);
    }

    @Test
    public void getSections10() throws XWikiException
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

    @Test
    public void getSections() throws XWikiException
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

    @Test
    public void getDocumentSection10() throws XWikiException
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

    @Test
    public void getDocumentSection() throws XWikiException
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
    @Test
    public void getDocumentSectionWhenSectionInGroups() throws XWikiException
    {
        this.document.setContent(
            "= Heading1 =\n" + "para1\n" + "== Heading2 ==\n" + "para2\n" + "(((\n" + "== Heading3 ==\n" + "para3\n"
                + "(((\n" + "== Heading4 ==\n" + "para4\n" + ")))\n" + ")))\n" + "== Heading5 ==\n" + "para5\n");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        DocumentSection section = this.document.getDocumentSection(3);
        assertEquals("Heading5", section.getSectionTitle());
    }

    @Test
    public void getContentOfSection10() throws XWikiException
    {
        this.document.setContent(
            "content not in section\n" + "1 header 1\nheader 1 content\n" + "1.1 header 2\nheader 2 content");

        String content1 = this.document.getContentOfSection(1);
        String content2 = this.document.getContentOfSection(2);

        assertEquals("1 header 1\nheader 1 content\n1.1 header 2\nheader 2 content", content1);
        assertEquals("1.1 header 2\nheader 2 content", content2);
    }

    @Test
    public void getContentOfSection() throws XWikiException
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
        when(this.xWiki.getSectionEditingDepth()).thenReturn(3L);

        content3 = this.document.getContentOfSection(3);
        String content4 = this.document.getContentOfSection(4);

        assertEquals("=== header 3 ===\n\nheader 3 content", content3);
        assertEquals("== header 4 ==\n\nheader 4 content", content4);
    }

    @Test
    public void sectionSplit10() throws XWikiException
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

    @Test
    public void updateDocumentSection10() throws XWikiException
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

    @Test
    public void updateDocumentSection() throws XWikiException
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

    @Test
    public void display()
    {
        when(this.xWiki.getCurrentContentSyntaxId(any())).thenReturn("xwiki/2.0");

        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals(
            "{{html clean=\"false\" wiki=\"false\"}}<input size='30' id='Space.Page_0_string' value='string' name='Space.Page_0_string' type='text'/>{{/html}}",
            this.document.display("string", "edit", this.oldcore.getXWikiContext()));

        assertEquals("string", this.document.display("string", "view", this.oldcore.getXWikiContext()));

        this.baseObject.setStringValue("string", "1 & 2");

        assertEquals("{{html clean=\"false\" wiki=\"false\"}}1 &#38; 2{{/html}}",
            this.document.display("string", "view", this.oldcore.getXWikiContext()));

        this.baseObject.setStringValue("string", "1 < 2");

        assertEquals("{{html clean=\"false\" wiki=\"false\"}}1 &#60; 2{{/html}}",
            this.document.display("string", "view", this.oldcore.getXWikiContext()));

        this.baseObject.setStringValue("string", "1 > 2");

        assertEquals("1 > 2", this.document.display("string", "view", this.oldcore.getXWikiContext()));

        assertEquals("{{html clean=\"false\" wiki=\"false\"}}<p>area</p>{{/html}}",
            this.document.display("area", "view", this.oldcore.getXWikiContext()));
    }

    @Test
    public void display1020()
    {
        when(this.xWiki.getCurrentContentSyntaxId(any(), any())).thenReturn("xwiki/1.0");

        XWikiDocument doc10 = new XWikiDocument();
        doc10.setSyntax(Syntax.XWIKI_1_0);
        this.oldcore.getXWikiContext().setDoc(doc10);

        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("string", this.document.display("string", "view", this.oldcore.getXWikiContext()));
        assertEquals(
            "{pre}<input size='30' id='Space.Page_0_string' value='string' name='Space.Page_0_string' type='text'/>{/pre}",
            this.document.display("string", "edit", this.oldcore.getXWikiContext()));

        assertEquals("<p>area</p>", this.document.display("area", "view", this.oldcore.getXWikiContext()));
    }

    @Test
    public void displayTemplate20()
    {
        when(this.xWiki.getCurrentContentSyntaxId(any())).thenReturn("xwiki/2.0");

        this.oldcore.getXWikiContext().put("isInRenderingEngine", false);

        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("string", this.document.display("string", "view", this.oldcore.getXWikiContext()));
        assertEquals(
            "<input size='30' id='Space.Page_0_string' value='string' name='Space.Page_0_string' type='text'/>",
            this.document.display("string", "edit", this.oldcore.getXWikiContext()));

        assertEquals("<p>area</p>", this.document.display("area", "view", this.oldcore.getXWikiContext()));
    }

    @Test
    void displayEscapesClosingHTMLMacro()
    {
        this.oldcore.getXWikiContext().put("isInRenderingEngine", true);
        when(this.xWiki.getCurrentContentSyntaxId(any())).thenReturn("xwiki/2.1");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        BaseObject object = mock(BaseObject.class);
        when(object.getOwnerDocument()).thenReturn(this.document);

        BaseClass xClass = mock(BaseClass.class);
        when(object.getXClass(any())).thenReturn(xClass);
        PropertyClass propertyInterface = mock(PropertyClass.class);
        when(xClass.get("mock")).thenReturn(propertyInterface);
        doAnswer(call -> {
            call.getArgument(0, StringBuffer.class).append("{{/html}}content{{/html}}");
            return null;
        }).when(propertyInterface).displayView(any(StringBuffer.class), eq("mock"), any(String.class), eq(object),
            anyBoolean(), any(XWikiContext.class));

        assertEquals("{{html clean=\"false\" wiki=\"false\"}}&#123;&#123;/html}}content&#123;&#123;/html}}{{/html}}",
            this.document.display("mock", "view", object, this.oldcore.getXWikiContext()));
    }

    @Test
    public void convertSyntax() throws XWikiException
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

        this.document.convertSyntax("xwiki/2.0", this.oldcore.getXWikiContext());

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

    @Test
    public void getRenderedContent() throws XWikiException
    {
        this.document.setContent("**bold**");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("<p><strong>bold</strong></p>", this.document.getRenderedContent(this.oldcore.getXWikiContext()));

        this.translatedDocument = new XWikiDocument(this.document.getDocumentReference(), Locale.FRENCH);
        this.translatedDocument.setContent("//italic//");
        this.translatedDocument.setSyntax(Syntax.XWIKI_1_0);
        this.translatedDocument.setNew(false);

        when(this.xWiki.getLanguagePreference(any())).thenReturn(Locale.FRENCH.toString());
        when(this.xWiki.getDocument(eq(
            new DocumentReference(this.translatedDocument.getDocumentReference(), this.translatedDocument.getLocale())),
            any())).thenReturn(this.translatedDocument);

        assertEquals("<p><em>italic</em></p>", this.document.getRenderedContent(this.oldcore.getXWikiContext()));
    }

    @Test
    public void getRenderedContentWithSourceSyntax()
    {
        this.document.setSyntax(Syntax.XWIKI_1_0);

        assertEquals("<p><strong>bold</strong></p>",
            this.document.getRenderedContent("**bold**", "xwiki/2.0", this.oldcore.getXWikiContext()));
    }


    /**
     * Verify that if an error happens when evaluation the title, we fallback to the computed title.
     */
    @Test
    void testRenderedTitleWhenVelocityError() throws XWikiVelocityException
    {
        when(this.oldcore.getMockAuthorizationManager().hasAccess(same(Right.SCRIPT), any(), any())).thenReturn(true);

        this.document.setContent("Some content");
        this.document.setTitle("some content that generate a velocity error");
        when(this.velocityEngine.evaluate(any(), any(), any(), eq("some content that generate a velocity error")))
            .thenThrow(new XWikiVelocityException("message"));

        assertEquals("Page", this.document.getRenderedTitle(this.oldcore.getXWikiContext()));

        assertEquals("Failed to interpret title of document [Wiki:Space.Page].", this.logCapture.getLogEvent(0).getFormattedMessage());
    }

    /**
     * @see "XWIKI-7515: 'getIncludedPages' in class com.xpn.xwiki.api.Document threw java.lang.NullPointerException"
     */
    @Test
    public void getIncludedPages()
    {
        this.document.setSyntax(Syntax.XWIKI_2_1);

        this.document.setContent("no include");
        assertTrue(this.document.getIncludedPages(this.oldcore.getXWikiContext()).isEmpty());

        this.document.setContent("bad {{include/}}");
        assertTrue(this.document.getIncludedPages(this.oldcore.getXWikiContext()).isEmpty());

        this.document.setContent("good deprecated {{include document=\"Foo.Bar\"/}}");
        assertEquals(Arrays.asList("Foo.Bar"), this.document.getIncludedPages(this.oldcore.getXWikiContext()));

        this.document.setContent("good {{include reference=\"One.Two\"/}}");
        assertEquals(Arrays.asList("One.Two"), this.document.getIncludedPages(this.oldcore.getXWikiContext()));

        this.document.setContent("bad recursive {{include reference=\"\"/}}");
        assertTrue(this.document.getIncludedPages(this.oldcore.getXWikiContext()).isEmpty());

        this.document.setContent("bad recursive {{include reference=\"" + DOCNAME + "\"/}}");
        assertTrue(this.document.getIncludedPages(this.oldcore.getXWikiContext()).isEmpty());

        this.document.setContent("bad recursive {{include reference=\"" + DOCSPACE + "." + DOCNAME + "\"/}}");
        assertTrue(this.document.getIncludedPages(this.oldcore.getXWikiContext()).isEmpty());
    }

    /**
     * XWIKI-8025: XWikiDocument#backup/restoreContext doesn't update the reference to the Velocity context stored on
     * the XWiki context
     */
    @Test
    public void backupRestoreContextUpdatesVContext() throws Exception
    {
        final Execution execution = this.oldcore.getMocker().getInstance(Execution.class);

        when(this.velocityManager.getVelocityContext())
            .then(invocationOnMock -> execution.getContext().getProperty("velocityContext"));

        VelocityContext oldVelocityContext = new VelocityContext();
        execution.getContext().setProperty("velocityContext", oldVelocityContext);

        Map<String, Object> backup = new HashMap<>();
        XWikiDocument.backupContext(backup, this.oldcore.getXWikiContext());

        VelocityContext newVelocityContext = (VelocityContext) execution.getContext().getProperty("velocityContext");
        assertNotNull(newVelocityContext);
        assertNotSame(oldVelocityContext, newVelocityContext);
        assertSame(newVelocityContext, this.oldcore.getXWikiContext().get("vcontext"));

        XWikiDocument.restoreContext(backup, this.oldcore.getXWikiContext());

        assertSame(oldVelocityContext, execution.getContext().getProperty("velocityContext"));
        assertSame(oldVelocityContext, this.oldcore.getXWikiContext().get("vcontext"));
    }

    @Test
    public void getIntValue()
    {
        assertEquals(42, this.document.getIntValue(CLASS_REFERENCE, "int", 99));
        assertEquals(42, this.document.getIntValue(CLASS_REFERENCE, "int"));

        assertEquals(0, this.document.getIntValue(CLASS_REFERENCE, "foo"));
        assertEquals(99, this.document.getIntValue(CLASS_REFERENCE, "foo", 99));

        assertEquals(0, this.document.getIntValue(new DocumentReference("foo", "bar", "bla"), "foo"));
        assertEquals(99, this.document.getIntValue(new DocumentReference("foo", "bar", "bla"), "foo", 99));
    }

    @Test
    void getAttachment() throws Exception
    {
        this.document.setAttachment("file.txt", IOUtils.toInputStream("", Charset.defaultCharset()),
            this.oldcore.getXWikiContext());
        this.document.setAttachment("file2.txt", IOUtils.toInputStream("", Charset.defaultCharset()),
            this.oldcore.getXWikiContext());
        assertNotNull(this.document.getAttachment("file.txt"));
    }
    
    @Test
    void getAttachmentWithExtension() throws Exception
    {
        this.document.setAttachment("file2.txt", IOUtils.toInputStream("", Charset.defaultCharset()),
            this.oldcore.getXWikiContext());
        this.document.setAttachment("file.txt.txt", IOUtils.toInputStream("", Charset.defaultCharset()),
            this.oldcore.getXWikiContext());
        assertNotNull(this.document.getAttachment("file.txt"));
    }

    @Test
    void getExactAttachment() throws Exception
    {
        this.document.setAttachment("file.txt", IOUtils.toInputStream("", Charset.defaultCharset()),
            this.oldcore.getXWikiContext());
        this.document.setAttachment("file2.txt", IOUtils.toInputStream("", Charset.defaultCharset()),
            this.oldcore.getXWikiContext());
        assertNotNull(this.document.getExactAttachment("file.txt"));
    }

    @Test
    void getExactAttachmentWithExtension() throws Exception
    {
        this.document.setAttachment("file2.txt", IOUtils.toInputStream("", Charset.defaultCharset()),
            this.oldcore.getXWikiContext());
        this.document.setAttachment("file.txt.txt", IOUtils.toInputStream("", Charset.defaultCharset()),
            this.oldcore.getXWikiContext());
        assertNull(this.document.getExactAttachment("file.txt"));
    }

    /**
     * Validate that an attachment with the same name less the extension as an existing attachment does not override it.
     */
    @Test
    void setAttachment() throws Exception
    {
        this.document.setAttachment("file.txt", IOUtils.toInputStream("", Charset.defaultCharset()),
            this.oldcore.getXWikiContext());
        this.document.setAttachment("file", IOUtils.toInputStream("", Charset.defaultCharset()),
            this.oldcore.getXWikiContext());
        List<XWikiAttachment> attachmentList = this.document.getAttachmentList();
        assertEquals(2, attachmentList.size());
        assertEquals("file.txt", attachmentList.get(1).getFilename());
        assertEquals("file", attachmentList.get(0).getFilename());
    }

    /*
     * Test for checking that cloneInternal doesn't replace the XWikiDocumentArchive by an empty document archive,
     * and to ensure that the versioningStore is properly called when using
     * XWikiDocument#getDocumentArchive(XWikiContext).
     */
    @Test
    void getDocumentArchiveAfterClone() throws XWikiException
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        XWikiVersioningStoreInterface versioningStore =
            this.document.getVersioningStore(context);
        when(versioningStore.getXWikiDocumentArchive(any(), any())).then(invocationOnMock -> {
            XWikiDocument doc = invocationOnMock.getArgument(0);
            if (doc.getDocumentArchive() != null) {
                return doc.getDocumentArchive();
            } else {
                return mock(XWikiDocumentArchive.class);
            }
        });
        assertSame(versioningStore, document.getVersioningStore(context));
        assertNull(this.document.getDocumentArchive());
        XWikiDocumentArchive documentArchive = this.document.getDocumentArchive(context);
        assertNotNull(documentArchive);
        assertNotNull(this.document.getDocumentArchive());

        XWikiDocument cloneDoc = document.clone();
        assertNull(cloneDoc.getDocumentArchive());
        XWikiDocumentArchive cloneArchive = cloneDoc.getDocumentArchive(context);
        verify(versioningStore).getXWikiDocumentArchive(
            argThat(givenDoc -> givenDoc != XWikiDocumentTest.this.document), eq(context));
        assertNotNull(cloneArchive);
        assertNotSame(cloneArchive, documentArchive);
    }
}
