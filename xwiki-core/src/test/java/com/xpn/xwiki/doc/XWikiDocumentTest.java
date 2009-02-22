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
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.jmock.Mock;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DocumentSection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit tests for {@link XWikiDocument}.
 * 
 * @version $Id$
 */
public class XWikiDocumentTest extends AbstractBridgedXWikiComponentTestCase
{
    private XWikiDocument document;

    private Mock mockXWiki;

    private Mock mockXWikiRenderingEngine;

    private Mock mockXWikiVersioningStore;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.document = new XWikiDocument("Space", "Page");
        this.document.setSyntaxId("xwiki/1.0");

        this.mockXWiki = mock(XWiki.class);
        this.mockXWiki.stubs().method("Param").will(returnValue(null));

        this.mockXWikiRenderingEngine = mock(XWikiRenderingEngine.class);

        this.mockXWikiVersioningStore = mock(XWikiVersioningStoreInterface.class);
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(returnValue(null));

        this.mockXWiki.stubs().method("getRenderingEngine").will(returnValue(this.mockXWikiRenderingEngine.proxy()));
        this.mockXWiki.stubs().method("getVersioningStore").will(returnValue(this.mockXWikiVersioningStore.proxy()));

        getContext().setWiki((XWiki) this.mockXWiki.proxy());
    }

    public void testGetDisplayTitleWhenNoTitleAndNoContent()
    {
        this.document.setContent("Some content");

        assertEquals("Page", this.document.getDisplayTitle(getContext()));
    }

    public void testGetDisplayWhenTitleExists()
    {
        this.document.setContent("Some content");
        this.document.setTitle("Title");
        this.mockXWikiRenderingEngine.expects(once()).method("interpretText").with(eq("Title"), ANYTHING, ANYTHING)
            .will(returnValue("Title"));

        assertEquals("Title", this.document.getDisplayTitle(getContext()));
    }

    public void testGetDisplayWhenNoTitleButSectionExists()
    {
        this.document.setContent("Some content\n1 Title");
        this.mockXWikiRenderingEngine.expects(once()).method("interpretText").with(eq("Title"), ANYTHING, ANYTHING)
            .will(returnValue("Title"));

        assertEquals("Title", this.document.getDisplayTitle(getContext()));
    }

    public void testMinorMajorVersions()
    {
        // there is no version in doc yet, so 1.1
        assertEquals("1.1", this.document.getVersion());

        this.document.setMinorEdit(false);
        this.document.incrementVersion();
        // no version => incrementVersion sets 1.1
        assertEquals("1.1", this.document.getVersion());

        this.document.setMinorEdit(false);
        this.document.incrementVersion();
        // increment major version
        assertEquals("2.1", this.document.getVersion());

        this.document.setMinorEdit(true);
        this.document.incrementVersion();
        // increment minor version
        assertEquals("2.2", this.document.getVersion());
    }

    public void testAuthorAfterDocumentCopy() throws XWikiException
    {
        String author = "Albatross";
        this.document.setAuthor(author);
        XWikiDocument copy = this.document.copyDocument(this.document.getName() + " Copy", getContext());

        assertTrue(author.equals(copy.getAuthor()));
    }

    public void testCreatorAfterDocumentCopy() throws XWikiException
    {
        String creator = "Condor";
        this.document.setCreator(creator);
        XWikiDocument copy = this.document.copyDocument(this.document.getName() + " Copy", getContext());

        assertTrue(creator.equals(copy.getCreator()));
    }

    public void testCreationDateAfterDocumentCopy() throws XWikiException, InterruptedException
    {
        Date sourceCreationDate = this.document.getCreationDate();
        Thread.sleep(1000);
        XWikiDocument copy = this.document.copyDocument(this.document.getName() + " Copy", getContext());

        assertTrue(copy.getCreationDate().equals(sourceCreationDate));
    }

    public void testToStringReturnsFullName()
    {
        assertEquals("Space.Page", this.document.toString());
        assertEquals("Main.WebHome", new XWikiDocument().toString());
    }

    public void testSectionSplit() throws XWikiException
    {
        List<DocumentSection> sections;
        // Simple test
        this.document.setContent("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
            + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
        sections = this.document.getSections();
        assertEquals(3, sections.size());
        assertEquals("Section 1", sections.get(0).getSectionTitle());
        assertEquals("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
            + "Content of second section\n", this.document.getContentOfSection(1));
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

    public void testUpdateDocumentSection() throws XWikiException
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
        assertEquals("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
            + "Content of second section\n", this.document.getContentOfSection(1));
        assertEquals("1.1", sections.get(1).getSectionLevel());
        assertEquals("1.1 Subsection 2\nContent of second section\n", this.document.getContentOfSection(2));
        assertEquals(3, sections.get(2).getSectionNumber());
        assertEquals(80, sections.get(2).getSectionIndex());
        assertEquals("1 Section 3\nModified content of section 3", this.document.getContentOfSection(3));
    }

    public void testCloneSaveVersions()
    {
        XWikiDocument doc1 = new XWikiDocument("qwe", "qwe");
        XWikiDocument doc2 = (XWikiDocument) doc1.clone();
        doc1.incrementVersion();
        doc2.incrementVersion();
        assertEquals(doc1.getVersion(), doc2.getVersion());
    }

    public void testAddObject() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument("test", "document");
        this.mockXWiki.stubs().method("getClass").will(returnValue(new BaseClass()));
        BaseObject object = BaseClass.newCustomClassInstance("XWiki.XWikiUsers", getContext());
        doc.addObject("XWiki.XWikiUsers", object);
        assertEquals("XWikiDocument.addObject does not set the object's name", doc.getFullName(), object.getName());
    }

    public void testObjectNumbersAfterXMLRoundrip() throws XWikiException
    {
        String classname = XWikiConstant.TAG_CLASS;
        BaseClass tagClass = new BaseClass();
        tagClass.setName(classname);
        tagClass.addStaticListField(XWikiConstant.TAG_CLASS_PROP_TAGS, "Tags", 30, true, "", "checkbox");

        XWikiDocument doc = new XWikiDocument("test", "document");
        this.mockXWiki.stubs().method("getClass").will(returnValue(tagClass));
        this.mockXWiki.stubs().method("getEncoding").will(returnValue("iso-8859-1"));

        BaseObject object = BaseClass.newCustomClassInstance(classname, getContext());
        doc.addObject(classname, object);

        object = BaseClass.newCustomClassInstance(classname, getContext());
        doc.addObject(classname, object);

        object = BaseClass.newCustomClassInstance(classname, getContext());
        doc.addObject(classname, object);

        doc.getObjects(classname).set(1, null);

        String docXML = doc.toXML(getContext());
        XWikiDocument docFromXML = new XWikiDocument();
        docFromXML.fromXML(docXML);

        Vector<BaseObject> objects = doc.getObjects(classname);
        Vector<BaseObject> objectsFromXML = docFromXML.getObjects(classname);

        assertNotNull(objects);
        assertNotNull(objectsFromXML);

        assertTrue(objects.size() == objectsFromXML.size());

        for (int i = 0; i < objects.size(); i++) {
            if (objects.get(i) == null) {
                assertNull(objectsFromXML.get(i));
            } else {
                assertTrue(objects.get(i).getNumber() == objectsFromXML.get(i).getNumber());
            }
        }
    }

    public void testGetLinkedPages10()
    {
        this.mockXWiki.stubs().method("exists").will(returnValue(true));

        this.document.setContent("[TargetPage][TargetLabel>TargetPage][TargetSpace.TargetPage]"
            + "[TargetLabel>TargetSpace.TargetPage?param=value#anchor][http://externallink][mailto:mailto]");

        List<String> linkedPages = this.document.getLinkedPages(getContext());

        assertEquals(Arrays.asList("Space.TargetPage", "TargetSpace.TargetPage", "TargetSpace.TargetPage",
            "Space.TargetPage"), linkedPages);
    }

    public void testGetLinkedPages()
    {
        this.document.setContent("[[TargetPage]][[TargetLabel>>TargetPage]][[TargetSpace.TargetPage]]"
            + "[[TargetLabel>>TargetSpace.TargetPage?param=value#anchor]][[http://externallink]][[mailto:mailto]]");
        this.document.setSyntaxId("xwiki/2.0");

        List<String> linkedPages = this.document.getLinkedPages(getContext());

        assertEquals(Arrays.asList("XWiki.TargetPage", "TargetSpace.TargetPage"), linkedPages);
    }
}
