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

    public void testGetSplitSectionsAccordingToTitle() throws Exception {
        XWikiDocument doc =  new XWikiDocument("test", "SectionalEditTest");
        String content = "1 This is title 1 \n This is content of title 1\n" ;
        doc.setContent(content);
        assertEquals("The number of sections with 1 title is not correct !", 1, doc.getSplitSectionsAccordingToTitle().size());

        content += "1.1 This is the subtitle 1\nThis is content of subtitle 1\n";
        doc.setContent(content);
        assertEquals("The number of sections with 1 subtitle is not correct !", 2, doc.getSplitSectionsAccordingToTitle().size());

        content += "1.1 This is the subtitle 2\nThis is content of subtitle 2\n";
        doc.setContent(content);
        assertEquals("The number of sections with 2 subtitle is not correct !", 3, doc.getSplitSectionsAccordingToTitle().size());

        content += "1 This is the title 2 \n This is the content of title 2\n";
        doc.setContent(content);
        assertEquals("The number of sections with 2 title is not correct !", 4, doc.getSplitSectionsAccordingToTitle().size());
    }

    public void testGetSection() throws Exception {
        XWikiDocument doc =  new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n");
        // doc.getSection(int x) with return is an array that contain  sectionNumber , sectionIndex, sectionLevel, sectionTitle of a section.
        String[] sections1 = doc.getSection(1);
        assertEquals("Section number 1 is not correct !", "1", sections1[0]);
        assertEquals("Section index 1 is not correct !", "0", sections1[1]);
        assertEquals("Section level 1 is not correct !", "1", sections1[2]);
        assertEquals("Section title 1 is not correct !", "This is title 1", sections1[3]);

        String[] sections2 = doc.getSection(2);
        assertEquals("Section number 2 is not correct !", "2", sections2[0]);
        assertEquals("Section index 2 is not correct !", "45", sections2[1]);
        assertEquals("Section level 2 is not correct !", "1.1", sections2[2]);
        assertEquals("Section title 2 is not correct ", "This is the subtitle 1", sections2[3]);
    }

    public void testGetIndexOfSection() throws Exception {
        XWikiDocument doc =  new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1 This is the title 2\nThis is the content of title 2");
        assertEquals("Index section 1 is not correct !", 0, doc.getIndexOfSection(1));
        assertEquals("Index section 2 is not correct !", 45, doc.getIndexOfSection(2));
        assertEquals("Index section 3 is not correct !", 102, doc.getIndexOfSection(3));
    }

     public void testGetTitleOfSection() throws Exception {
        XWikiDocument doc = new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1 This is the title 2\nThis is the content of title 2");
        assertEquals("Title section 1 is not correct !", "This is title 1", doc.getTitleOfSection(1));
        assertEquals("Title section 2 is not correct !", "This is the subtitle 1", doc.getTitleOfSection(2));
        assertEquals("Title section 3 is not correct !", "This is the title 2", doc.getTitleOfSection(3));
    }

    public void testGetContentOfSection() throws Exception {
        XWikiDocument doc = new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1 This is the title 2\nThis is the content of title 2");
        assertEquals("The contents of title 1 is not correct !", "1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n",
                doc.getContentOfSection(1));
        assertEquals("The contents of subtitle 1 is not correct !", "1.1 This is the subtitle 1\nThis is content of subtitle 1\n",
                doc.getContentOfSection(2));
        assertEquals("The contents of title 2 is not correct !", "1 This is the title 2\nThis is the content of title 2",
                doc.getContentOfSection(3));
    }

    public void testUpdateSection() {
        XWikiDocument doc = new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1 This is the title 2\nThis is the content of title 2");
        assertEquals("Update the title is false !", "1 This is title 1 'update'\nThis is content of title 1 'update'\n1.1 This is the subtitle 1 'update'\nThis is content of subtitle 1 'update'\n1 This is the title 2\nThis is the content of title 2",
                doc.updateSection(1, "1 This is title 1 'update'\nThis is content of title 1 'update'\n1.1 This is the subtitle 1 'update'\nThis is content of subtitle 1 'update'\n"));

        assertEquals("Update subtitle 1 is false ", "1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1 'update'\nThis is content of subtitle 1 'update'\n1 This is the title 2\nThis is the content of title 2",
                doc.updateSection(2, "1.1 This is the subtitle 1 'update'\nThis is content of subtitle 1 'update'\n"));

        assertEquals("Update title 2 is fales","1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1 This is the title 2 'update'\nThis is the content of title 2 'update'",
                doc.updateSection(3, "1 This is the title 2 'update'\nThis is the content of title 2 'update'"));
    }
}