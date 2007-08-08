/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

import java.util.Date;

import org.jmock.cglib.MockObjectTestCase;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Unit tests for {@link XWikiDocumentArchive}.
 *
 * @version $Id: $
 */
public class XWikiDocumentArchiveTest extends MockObjectTestCase
{
    private XWikiContext context;
    protected void setUp() throws Exception
    {
        super.setUp();
        this.context = new XWikiContext();
    }
    
    /**
     * JRCS uses the user.name system property to set the author of a change. Verify that it
     * works if the user name has a space in its name. This used to fail and this test is here to
     * ensure it'll continue to work fine in the future...
     *
     * @todo simplify this test. Not sure how to do it. I guess we could create a real document.
     */
    public void testUpdateArchiveWhenSpaceInUsername() throws Exception
    {
        String originalArchive = "head\t1.1;\n" +
            "access;\n" +
            "symbols;\n" +
            "locks; strict;\n" +
            "comment\t@# @;\n" +
            "\n" +
            "\n" +
            "1.1\n" +
            "date\t2007.02.14.14.01.57;\tauthor vmassol;\tstate Exp;\n" +
            "branches;\n" +
            "next\t;\n" +
            "\n" +
            "\n" +
            "desc\n" +
            "@@\n" +
            "\n" +
            "\n" +
            "1.1\n" +
            "log\n" +
            "@KnowledgeBase.WebHome\n" +
            "@\n" +
            "text\n" +
            "@<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
            "\n" +
            "<xwikidoc>\n" +
            "<web>KnowledgeBase</web>\n" +
            "<name>WebHome</name>\n" +
            "<language></language>\n" +
            "<defaultLanguage>en</defaultLanguage>\n" +
            "<translation>0</translation>\n" +
            "<parent>Main.Notes</parent>\n" +
            "<creator>XWiki.Admin</creator>\n" +
            "<author>XWiki.Admin</author>\n" +
            "<customClass></customClass>\n" +
            "<contentAuthor>XWiki.Admin</contentAuthor>\n" +
            "<creationDate>1165874272000</creationDate>\n" +
            "<date>1166177448000</date>\n" +
            "<contentUpdateDate>1171458116000</contentUpdateDate>\n" +
            "<version>1.1</version>\n" +
            "<title></title>\n" +
            "<template></template>\n" +
            "<defaultTemplate></defaultTemplate>\n" +
            "<validationScript></validationScript>\n" +
            "<object>\n" +
            "<class>\n" +
            "<name>XWiki.TagClass</name>\n" +
            "<customClass></customClass>\n" +
            "<customMapping></customMapping>\n" +
            "<defaultViewSheet></defaultViewSheet>\n" +
            "<defaultEditSheet></defaultEditSheet>\n" +
            "<defaultWeb></defaultWeb>\n" +
            "<nameField></nameField>\n" +
            "<validationScript></validationScript>\n" +
            "<tags>\n" +
            "<name>tags</name>\n" +
            "<prettyName>Tags</prettyName>\n" +
            "<unmodifiable>0</unmodifiable>\n" +
            "<relationalStorage>1</relationalStorage>\n" +
            "<displayType>checkbox</displayType>\n" +
            "<multiSelect>1</multiSelect>\n" +
            "<size>30</size>\n" +
            "<separator> </separator>\n" +
            "<separators> ,|</separators>\n" +
            "<values></values>\n" +
            "<number>1</number>\n" +
            "<classType>com.xpn.xwiki.objects.classes.StaticListClass</classType>\n" +
            "</tags>\n" +
            "</class>\n" +
            "<name>KnowledgeBase.WebHome</name>\n" +
            "<number>0</number>\n" +
            "<className>XWiki.TagClass</className>\n" +
            "<property>\n" +
            "<tags/>\n" +
            "</property>\n" +
            "</object>\n" +
            "<content>1 Wiki Knowledge Base\n" +
            "\n" +
            "This is the Wiki Knowledge Base, where you can start writing about your favorite subjects.\n" +
            "\n" +
            "To create new pages, click edit button and write links using brackets around words.\n" +
            "\n" +
            "* [Example Link 1]\n" +
            "* [Example Link 2]</content>\n" +
            "</xwikidoc>\n" +
            "@";
        
        XWikiDocumentArchive archive = new XWikiDocumentArchive(123456789L);
        archive.setArchive(originalArchive);
        
        // Set a username with a space
        System.setProperty("user.name", "Vincent Massol");
        
        XWikiDocument doc = new XWikiDocument("KnowledgeBase", "WebHome");
        doc.setContent(doc.getContent() + "\nsomething added");
        archive.updateArchive("XWiki.XWikiGuest", new Date(), "some comment", false, doc, context);

        // Try to construct again the archive from the last modification. This will happen when
        // XWiki loads a document from the database for example. We verify here that a username
        // with a space works.
        new XWikiDocumentArchive(123456789L).setArchive(archive.getArchive(context));
    }
    
    public void testUpdateLoad() throws XWikiException {
        XWikiDocument doc = new XWikiDocument("Test", "Test");
        doc.setContent("content 1.1");
        
        XWikiContext context = new XWikiContext();

        XWikiDocumentArchive archive = new XWikiDocumentArchive(doc.getId());
        assertEquals(0, archive.getNodes().size());
        
        String author = "XWiki.some author";
        archive.updateArchive(author, new Date(), "initial, 1.1", false, doc, context);
        String archive11 = archive.getArchive(context);
        assertEquals(1, archive.getNodes().size());
        assertEquals(1, archive.getUpdatedNodeInfos().size());
        assertEquals(1, archive.getUpdatedNodeContents().size());
        
        XWikiDocumentArchive archive2 = new XWikiDocumentArchive(doc.getId());
        archive2.setArchive(archive11);
        assertEquals(archive11, archive2.getArchive(context));
        assertEquals(1, archive2.getNodes().size());
        assertEquals(1, archive2.getUpdatedNodeInfos().size());
        assertEquals(1, archive2.getUpdatedNodeContents().size());
        
        doc.setContent("content\n1.2");
        archive.updateArchive(author, new Date(), "1.2", true, doc, context);
        String archive12 = archive.getArchive(context);
        assertEquals(2, archive.getNodes().size());
        assertEquals(2, archive.getUpdatedNodeInfos().size());
        assertEquals(2, archive.getUpdatedNodeContents().size());
        
        XWikiDocumentArchive archive3 = new XWikiDocumentArchive(doc.getId());
        archive3.setArchive(archive12);
        assertEquals(2, archive3.getNodes().size());
        assertEquals(2, archive3.getUpdatedNodeInfos().size());
        assertEquals(2, archive3.getUpdatedNodeContents().size());
        
        doc.setContent("major change\ncontent\n2.1");
        archive.updateArchive(author, new Date(), "2.1", false, doc, context);
        assertEquals(3, archive.getNodes().size());
        assertEquals(3, archive.getUpdatedNodeInfos().size());
        assertEquals(3, archive.getUpdatedNodeContents().size());
    }
    
    public void testRemoveVersions() throws XWikiException {
        XWikiContext context = new XWikiContext();
        
        XWikiDocument doc = new XWikiDocument("Test", "Test");
        XWikiDocumentArchive archive = new XWikiDocumentArchive(doc.getId());
        doc.setDocumentArchive(archive);
        String author = "XWiki.some author";
        
        doc.setContent("content 1.1");
        archive.updateArchive(author, new Date(), "initial, 1.1", false, doc, context);
        XWikiDocument doc11 = (XWikiDocument) doc.clone();
        
        doc.setContent("content 2.1\nqwe");
        archive.updateArchive(author, new Date(), "2.1", false, doc, context);
        
        doc.setContent("content 2.2\nqweq");
        archive.updateArchive(author, new Date(), "2.2", true, doc, context);
        
        doc.setContent("content 2.3\nqweqe");
        archive.updateArchive(author, new Date(), "2.3", true, doc, context);
        
        /* TODO: need many mocking
        archive.removeVersions(new Version(2,1), new Version(2,2), doc, context);
        
        assertEquals(2, archive.getNodes().size());
        assertEquals(2, archive.getDeleteNodeInfo().size());
        assertNull(archive.getNode(new Version(2,1)));
        assertNull(archive.getNode(new Version(2,2)));
        
        assertEquals(doc11, archive.loadDocument(doc, new Version(1,1), context));*/
    }
}
