/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DocumentSection;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * This class to test for features of Sectional Edit
 * @author Phung Hai Nam
 * @version 22 Aug 2006
 */
public class SectionalEditTest extends HibernateTestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * getSplitSectionAccordingToTitle() method to get a list of sections that are splitted by the title of content
     * this test will check number of sections that splited by the title.
     */
    public void testGetSplitSectionsAccordingToTitle() throws XWikiException {
        XWikiDocument doc =  new XWikiDocument("test", "SectionalEditTest");
        String content = "1 This is title 1 \n This is content of title 1\n" ;
        doc.setContent(content);
        // only a title (level 1)
        assertEquals("The number of sections with 1 title is not correct !", 1, doc.getSplitSectionsAccordingToTitle().size());

        content += "1.1 This is the subtitle 1\nThis is content of subtitle 1\n";
        doc.setContent(content);
        // there is a subtitle (1.1)
        assertEquals("The number of sections with 1 subtitle is not correct !", 2, doc.getSplitSectionsAccordingToTitle().size());

        content += "1.1 This is the subtitle 2\nThis is content of subtitle 2\n";
        doc.setContent(content);
        //  there are two subtitle (1.1)
        assertEquals("The number of sections with 2 subtitle is not correct !", 3, doc.getSplitSectionsAccordingToTitle().size());

        content += "1 This is the title 2 \n This is the content of title 2\n";
        doc.setContent(content);
        // there are two title (level 1)
        assertEquals("The number of sections with 2 title is not correct !", 4, doc.getSplitSectionsAccordingToTitle().size());
    }

    public void testGetDocumentSection() throws XWikiException {
        XWikiDocument doc =  new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n");

        DocumentSection docSection1 = doc.getDocumentSection(1);
        // test document section 1
        assertEquals("Section number 1 is not correct !", 1, docSection1.getSectionNumber());
        assertEquals("Section index 1 is not correct !", 0, docSection1.getSectionIndex());
        assertEquals("Section level 1 is not correct !", "1", docSection1.getSectionLevel());
        assertEquals("Section title 1 is not correct !", "This is title 1", docSection1.getSectionTitle());

        DocumentSection docSection2 = doc.getDocumentSection(2);
        // test document section 2
        assertEquals("Section number 2 is not correct !", 2, docSection2.getSectionNumber());
        assertEquals("Section index 2 is not correct !", 45, docSection2.getSectionIndex());
        assertEquals("Section level 2 is not correct !", "1.1", docSection2.getSectionLevel());
        assertEquals("Section title 2 is not correct ", "This is the subtitle 1", docSection2.getSectionTitle());
    }

    public void testGetContentOfSection() throws XWikiException {
        XWikiDocument doc = new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1 This is the title 2\nThis is the content of title 2");
        // test the contents of section 1 (title 1)
        assertEquals("The contents of title 1 is not correct !", "1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n",
                doc.getContentOfSection(1));
        // test the contents of section 2 (title 2)
        assertEquals("The contents of subtitle 1 is not correct !", "1.1 This is the subtitle 1\nThis is content of subtitle 1\n",
                doc.getContentOfSection(2));
        // test the contents of section 2 (title 3)
        assertEquals("The contents of title 2 is not correct !", "1 This is the title 2\nThis is the content of title 2",
                doc.getContentOfSection(3));
    }

    /**
     * This test for updateDocumentSection(int sectionNumber, String newSectionContent) method
     * this method will return a string that are new contents of document after it has been updated the content of a section
     */
    public void testUpdateDocumentSection() throws XWikiException{
        XWikiDocument doc = new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1 This is the title 2\nThis is the content of title 2");
        assertEquals("Update the title is false !", "1 This is title 1 'update'\nThis is content of title 1 'update'\n1.1 This is the subtitle 1 'update'\nThis is content of subtitle 1 'update'\n1 This is the title 2\nThis is the content of title 2",
                doc.updateDocumentSection(1, "1 This is title 1 'update'\nThis is content of title 1 'update'\n1.1 This is the subtitle 1 'update'\nThis is content of subtitle 1 'update'\n"));

        assertEquals("Update subtitle 1 is false ", "1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1 'update'\nThis is content of subtitle 1 'update'\n1 This is the title 2\nThis is the content of title 2",
                doc.updateDocumentSection(2, "1.1 This is the subtitle 1 'update'\nThis is content of subtitle 1 'update'\n"));

        assertEquals("Update title 2 is fales","1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1 This is the title 2 'update'\nThis is the content of title 2 'update'",
                doc.updateDocumentSection(3, "1 This is the title 2 'update'\nThis is the content of title 2 'update'"));

        //  test editing section with the contents only contain a title (level 1) without subtitle
        doc.setContent("1 This is title without subtitle\nThis is the content of title without subtitle");
        assertEquals("Update title without subtitle is false", "1 This is title\nThis is the content of title",
                doc.updateDocumentSection(1, "1 This is title\nThis is the content of title"));

        //  test editing section with the contents only contain a subtitle (level 1.1) without title (level 1)
        doc.setContent("1.1 This is title (level 1.1) without title level 1\nThis is the content of title");
        assertEquals("Update title level 1.1 without title level 1 is false", "1.1 'Update' This is title (level 1.1) without title level 1\nThis is the content of title 'Update'",
                doc.updateDocumentSection(1, "1.1 'Update' This is title (level 1.1) without title level 1\nThis is the content of title 'Update'"));
    }
}