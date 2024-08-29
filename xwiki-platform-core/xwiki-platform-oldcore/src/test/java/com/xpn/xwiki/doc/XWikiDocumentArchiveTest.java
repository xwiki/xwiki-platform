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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.user.api.XWikiRightService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link XWikiDocumentArchive}.
 *
 * @version $Id$
 */
@OldcoreTest
@AllComponents
class XWikiDocumentArchiveTest
{
    private XWikiContext context;

    @BeforeEach
    void setUp(MockitoOldcore mockitoOldcore) throws Exception
    {
        this.context = mockitoOldcore.getXWikiContext();
    }
    
    /**
     * JRCS uses the user.name system property to set the author of a change. Verify that it
     * works if the user name has a space in its name. This used to fail and this test is here to
     * ensure it'll continue to work fine in the future...
     *
     * @todo simplify this test. Not sure how to do it. I guess we could create a real document.
     */
    @Test
    void updateArchiveWhenSpaceInUsername() throws Exception
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
        
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Wiki", "KnowledgeBase", "WebHome"));
        doc.setContent(doc.getContent() + "\nsomething added");
        archive.updateArchive(doc, XWikiRightService.GUEST_USER_FULLNAME, new Date(), "some comment", null, context);

        // Try to construct again the archive from the last modification. This will happen when
        // XWiki loads a document from the database for example. We verify here that a username
        // with a space works.
        new XWikiDocumentArchive(123456789L).setArchive(archive.getArchive(context));
    }

    @Test
    void updateLoad() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Test", "Test", "Test"));
        doc.setContent("content 1.1");
        
        XWikiDocumentArchive archive = new XWikiDocumentArchive(doc.getId());
        assertEquals(0, archive.getNodes().size());
        
        String author = "XWiki.some author";
        archive.updateArchive(doc, author, new Date(), "initial, 1.1", null, context);
        assertEquals(new Version(1,1), archive.getLatestVersion());
        String archive11 = archive.getArchive(context);
        assertEquals(1, archive.getNodes().size());
        assertEquals(1, archive.getUpdatedNodeInfos().size());
        assertEquals(1, archive.getUpdatedNodeContents().size());
        
        XWikiDocumentArchive archive2 = new XWikiDocumentArchive(doc.getId());
        archive2.setArchive(archive11);
        assertEquals(new Version(1,1), archive2.getLatestVersion());
        assertEquals(archive11, archive2.getArchive(context));
        assertEquals(1, archive2.getNodes().size());
        assertEquals(1, archive2.getUpdatedNodeInfos().size());
        assertEquals(1, archive2.getUpdatedNodeContents().size());
        
        doc.setContent("content\n1.2");
        archive.updateArchive(doc, author, new Date(), "1.2", new Version(1,2), context);
        assertEquals(new Version(1,2), archive.getLatestVersion());
        String archive12 = archive.getArchive(context);
        assertEquals(2, archive.getNodes().size());
        assertEquals(2, archive.getUpdatedNodeInfos().size());
        assertEquals(2, archive.getUpdatedNodeContents().size());
        
        XWikiDocumentArchive archive3 = new XWikiDocumentArchive(doc.getId());
        archive3.setArchive(archive12);
        assertEquals(new Version(1,2), archive3.getLatestVersion());
        assertEquals(2, archive3.getNodes().size());
        assertEquals(2, archive3.getUpdatedNodeInfos().size());
        assertEquals(2, archive3.getUpdatedNodeContents().size());
        
        doc.setContent("major change\ncontent\n2.1");
        archive.updateArchive(doc, author, new Date(), "2.1", new Version(1,1), context);
        assertEquals(new Version(2,1), archive.getLatestVersion());
        assertEquals(3, archive.getNodes().size());
        assertEquals(3, archive.getUpdatedNodeInfos().size());
        assertEquals(3, archive.getUpdatedNodeContents().size());
        
        doc.setContent("major change\ncontent\n 3.3");
        archive.updateArchive(doc, author, new Date(), "2.1", new Version(3,3), context);
        assertEquals(new Version(3,3), archive.getLatestVersion());
    }

    @Test
    void removeVersions() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Test", "Test", "Test"));
        XWikiDocumentArchive archive = new XWikiDocumentArchive(doc.getId());
        doc.setDocumentArchive(archive);
        String author = "XWiki.some author";
        
        addRevisionToHistory(archive, doc, "content 1.1", author, "initial 1.1");
        XWikiDocument doc11 = doc.clone();
        
        doc.setContent("content 2.1\nqwe @ ");
        archive.updateArchive(doc, author, new Date(), "2.1", new Version(2,1), context);
        
        doc.setContent("content 2.2\nqweq@ ");
        archive.updateArchive(doc, author, new Date(), "2.2", new Version(2,2), context);
        
        doc.setContent("content 2.3\nqweqe @@");
        archive.updateArchive(doc, author, new Date(), "2.3", new Version(2,3), context);
        assertEquals(new Version(2,3), archive.getLatestVersion());
        
        archive.removeVersions(new Version(2,1), new Version(2,2), context);
        
        assertEquals(2, archive.getNodes().size());
        assertEquals(2, archive.getDeletedNodeInfo().size());
        assertNull(archive.getNode(new Version(2,1)));
        assertNull(archive.getNode(new Version(2,2)));
        
        XWikiDocument actual = archive.loadDocument(new Version(1,1), context);
        assertEquals(doc11.getContent(), actual.getContent());
        assertEquals(doc11.getDate(), actual.getDate());
        assertEquals(doc11.getAuthor(), actual.getAuthor());
        assertEquals(doc11.getComment(), actual.getComment());
    }

    @Test
    void getNodes() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Test", "Test", "Test"));
        XWikiDocumentArchive archive = new XWikiDocumentArchive(doc.getId());
        doc.setDocumentArchive(archive);
        String author = "XWiki.some author";

        addRevisionToHistory(archive, doc, "content 1.1", author, "initial 1.1");

        doc.setContent("content 2.1\nqwe @ ");
        archive.updateArchive(doc, author, new Date(), "2.1", new Version(2,1), context);

        doc.setContent("content 2.2\nqweq@ ");
        archive.updateArchive(doc, author, new Date(), "2.2", new Version(2,2), context);

        doc.setContent("content 2.3\nqweqe @@");
        archive.updateArchive(doc, author, new Date(), "2.3", new Version(2,3), context);
        assertEquals(new Version(2,3), archive.getLatestVersion());

        Collection<XWikiRCSNodeInfo> nodes = archive.getNodes(new Version("3.0"), new Version("2.2"));
        assertEquals(2, nodes.size());
        Iterator<XWikiRCSNodeInfo> iterator = nodes.iterator();
        assertEquals(new Version("2.3"), iterator.next().getVersion());
        assertEquals(new Version("2.2"), iterator.next().getVersion());

        nodes = archive.getNodes(new Version("2.2"), new Version("2.2"));
        assertEquals(1, nodes.size());
        iterator = nodes.iterator();
        assertEquals(new Version("2.2"), iterator.next().getVersion());
    }

    @Test
    void getNextFullVersions() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Test", "Test", "Test"));
        XWikiDocumentArchive archive = new XWikiDocumentArchive(doc.getId());
        doc.setDocumentArchive(archive);
        String author = "XWiki.some author";

        // We have a full version every 5 nodes, so we're injecting 15 versions
        addRevisionToHistory(archive, doc, "content 1.1", author, "initial 1.1");

        doc.setContent("content 2.1\nqwe @ ");
        archive.updateArchive(doc, author, new Date(), "2.1", new Version(2,1), context);

        doc.setContent("content 2.2\nqweq@ ");
        archive.updateArchive(doc, author, new Date(), "2.2", new Version(2,2), context);

        doc.setContent("content 2.3\nqweqe @@");
        archive.updateArchive(doc, author, new Date(), "2.3", new Version(2,3), context);

        doc.setContent("content 2.4\nqweqe @@");
        archive.updateArchive(doc, author, new Date(), "2.4", new Version(2,4), context);

        // 5 nodes so far,
        // let's add 5

        for (int i = 1; i <= 5; i++) {
            String version = String.format("3.%s", i);
            doc.setContent(version + "\nqweqe @@");
            archive.updateArchive(doc, author, new Date(), version, new Version(version), context);
        }

        // let's add 7
        for (int i = 1; i <= 7; i++) {
            String version = String.format("4.%s", i);
            doc.setContent(version + "\nqweqe @@");
            archive.updateArchive(doc, author, new Date(), version, new Version(version), context);
        }
        assertEquals(new Version(4,7), archive.getLatestVersion());

        assertEquals(new Version("2.4"), archive.getNextFullVersion(new Version("2.3")));
        assertEquals(new Version("2.4"), archive.getNextFullVersion(new Version("1.1")));
        assertEquals(new Version("2.4"), archive.getNextFullVersion(new Version("2.1")));
        assertEquals(new Version("3.5"), archive.getNextFullVersion(new Version("3.1")));
        assertEquals(new Version("4.7"), archive.getNextFullVersion(new Version("4.6")));
        assertEquals(new Version("4.5"), archive.getNextFullVersion(new Version("4.4")));
    }

    /**
     * Verify issue "When loading a revision of a document the creation date is incorrectly set as the last
     * modification date".
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-2029">XWIKI-2029</a>
     */
    @Test
    void verifyCreationDateWhenLoadingDocumentFromArchive() throws Exception
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Test", "Test", "Test"));
        XWikiDocumentArchive archive = new XWikiDocumentArchive(doc.getId());
        doc.setDocumentArchive(archive);
        String author = "XWiki.some author";

        addRevisionToHistory(archive, doc, "content 1.1", author, "initial 1.1");

        Date creationDate = doc.getCreationDate();

        // Wait for 2 seconds and make a change. We'll then load the last revision and verify it has a correct
        // creation date.
        Thread.sleep(1000L);

        doc.setContent("content 2.1\nqwe @ ");
        archive.updateArchive(doc, author, new Date(), "2.1", new Version(2,1), context);

        XWikiDocument latest = archive.loadDocument(new Version(2,1), context);

        assertEquals(creationDate, latest.getCreationDate());
    }

    @Test
    void verifyDiffAndFullRevisionAlgorithm() throws Exception
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Test", "Test", "Test"));
        XWikiDocumentArchive archive = new XWikiDocumentArchive(doc.getId());
        doc.setDocumentArchive(archive);
        String author = "XWiki.some author";

        // The first revision is always a full revision (not a diff)
        addRevisionToHistory(archive, doc, "content 1.1", author, "1.1");
        assertFalse(archive.getNode(new Version(1, 1)).isDiff());

        // When a new revision is added the new revision is always the full revision but the previous one is
        // modified to be a diff.
        addRevisionToHistory(archive, doc, "content 2.1", author, "2.1");
        assertTrue(archive.getNode(new Version(1, 1)).isDiff());
        assertFalse(archive.getNode(new Version(2, 1)).isDiff());

        addRevisionToHistory(archive, doc, "content 3.1", author, "3.1");
        assertTrue(archive.getNode(new Version(1, 1)).isDiff());
        assertTrue(archive.getNode(new Version(2, 1)).isDiff());
        assertFalse(archive.getNode(new Version(3, 1)).isDiff());

        addRevisionToHistory(archive, doc, "content 4.1", author, "4.1");
        assertTrue(archive.getNode(new Version(1, 1)).isDiff());
        assertTrue(archive.getNode(new Version(2, 1)).isDiff());
        assertTrue(archive.getNode(new Version(3, 1)).isDiff());
        assertFalse(archive.getNode(new Version(4, 1)).isDiff());

        // Every 5th revision we save the full content and not a diff
        addRevisionToHistory(archive, doc, "content 5.1", author, "5.1");
        assertTrue(archive.getNode(new Version(1, 1)).isDiff());
        assertTrue(archive.getNode(new Version(2, 1)).isDiff());
        assertTrue(archive.getNode(new Version(3, 1)).isDiff());
        assertTrue(archive.getNode(new Version(4, 1)).isDiff());
        assertFalse(archive.getNode(new Version(5, 1)).isDiff());

        // Verify that the 5th revision is kept as a full content revision when the 6th is added
        addRevisionToHistory(archive, doc, "content 6.1", author, "6.1");
        assertTrue(archive.getNode(new Version(1, 1)).isDiff());
        assertTrue(archive.getNode(new Version(2, 1)).isDiff());
        assertTrue(archive.getNode(new Version(3, 1)).isDiff());
        assertTrue(archive.getNode(new Version(4, 1)).isDiff());
        assertFalse(archive.getNode(new Version(5, 1)).isDiff());
        assertFalse(archive.getNode(new Version(6, 1)).isDiff());
    }

    private void addRevisionToHistory(XWikiDocumentArchive archive, XWikiDocument document, String content,
        String author, String comment) throws XWikiException
    {
        document.setContent(content);
        document.setAuthor(author);
        document.setComment(comment);
        document.setDate(new Date());
        archive.updateArchive(document, document.getAuthor(), document.getDate(), document.getComment(), null, context);
    }
}
