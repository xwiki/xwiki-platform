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

import java.util.List;

/**
 * This class to test for feat Sectional Edit
 * @author Phung Hai Nam
 * @version 22 Aug 2006
 */
public class SectionalEditTest extends HibernateTestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    /** getSplitSections() is method that return a list that contain of information about a title ,
     * index, number for every title
     */
    public void testGetSplitSections() throws Exception {
        XWikiDocument doc =  new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 This is the test \n This is content of test");
        List splitSections = doc.getSplitSections();
        assertEquals("The number of sections is not correct !", 1, splitSections.size());
        doc.setContent("1 This is the title 1 \n This is content 1\n 1.1 This is the title 2");
        assertEquals("The number of sections is not correct !", 2, doc.getSplitSections().size());
        doc.setContent("1 this is the title 1 \n This is the content 1 \n 1.1 This is the title 2\n" +
                "1 This is the title 3 \n This is the content 3");
        assertEquals("The number of sections is not correct !", 3, doc.getSplitSections().size());
    }

    /** Test getSection() method whether it return the correct section */
    public void testGetSection() throws Exception {
        XWikiDocument doc =  new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 This is the test\n" +
                "This is content in title 1\n" +
                "1.1 This is subtitle\n" +
                "This is content of subtitle\n");
        String[] sections1 = doc.getSection(1);
        assertEquals("Section number 1 is not correct !", "1", sections1[0]);
        assertEquals("Section index 1 is not correct !", "0", sections1[1]);
        assertEquals("Section level 1 is not correct !", "1", sections1[2]);
        assertEquals("Section title 1 is not correct !", "This is the test", sections1[3]);

        String[] sections2 = doc.getSection(2);
        assertEquals("Section number 2 is not correct !", "2", sections2[0]);
        assertEquals("Section index 2 is not correct !", "46", sections2[1]);

        assertEquals("Section level 2 is not correct !", "1.1", sections2[2]);
        assertEquals("Section title 2 is not correct ", "This is subtitle", sections2[3]);
    }

    public void testGetIndexSection() throws Exception {
        XWikiDocument doc =  new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 First\n 1.1 Second\n 1 Third");
        assertEquals("Index section 1 is not correct !", 0, doc.getIndexSection(1));
        assertEquals("Index section 2 is not correct !", 8, doc.getIndexSection(2));
        assertEquals("Index section 3 is not correct !", 20, doc.getIndexSection(3));
    }

    public void testGetTitleSection() throws Exception {
        XWikiDocument doc = new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 this is the title 1 \n This is the content 1 \n 1.1 This is the title 2\n" +
                "1 This is the title 3 \n This is the content 3");
        assertEquals("Title section 1 is not correct !", "this is the title 1 ", doc.getTitleSection(1));
        assertEquals("Title section 2 is not correct !", "This is the title 2", doc.getTitleSection(2));
        assertEquals("Title section 3 is not correct !", "This is the title 3 ", doc.getTitleSection(3));
    }

    /** Test content of section. getContentOfSection() method return the total content of section. */
    public void testGetContentOfSection() throws Exception {
        XWikiDocument doc = new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 this is the title 1 \n This is the content 1 \n 1.1 This is the subtitle 1\n This is content of subtitle 1\n" +
                "1 This is the title 3 \n This is the content 3");
        assertEquals("The contents of title 1 is not correct !", "1 this is the title 1 \n" +
                " This is the content 1 \n" +
                " 1.1 This is the subtitle 1\n" +
                " This is content of subtitle 1\n", doc.getContentOfSection(1));
        assertEquals("The contents of subtitle 1 is not correct !", " 1.1 This is the subtitle 1\n" +
                " This is content of subtitle 1\n", doc.getContentOfSection(2));
    }

    /** updateSection() is method that to update the content of a session
     * it requires two parameters are sectionNumber and newContent for that section
     */
    public void testUpdateSection() {
        XWikiDocument doc = new XWikiDocument("test", "SectionalEditTest");
        doc.setContent("1 Title1\nContent title 1\n1.1 Subtitle 1\nContent subtitle 1\n1.1 Subtitle 2\nContent subtitle2\n" +
                "1 Title2\nContent title 2\n1.1 Subtitle 2");
        assertEquals("Update the title is not correct !", "1 Title1 update\nContent title 1\n1.1 Subtitle 1\nContent subtitle 1\n1.1 Subtitle 2\nContent subtitle2\n" +
                "1 Title2\nContent title 2\n1.1 Subtitle 2",
                doc.updateSection(1, "1 Title1 update\nContent title 1\n1.1 Subtitle 1\nContent subtitle 1\n1.1 Subtitle 2\nContent subtitle2\n"));
        assertEquals("Update subtitle is not correct ", "1 Title1\nContent title 1\n1.1 Subtitle 1\nContent subtitle 1\n1.1 Subtitle 2\nContent subtitle2 update\n" +
                "1 Title2\nContent title 2\n1.1 Subtitle 2",
                doc.updateSection(3, "1.1 Subtitle 2\nContent subtitle2 update\n"));
        assertEquals("Update title 2 is not correct","1 Title1\nContent title 1\n1.1 Subtitle 1\nContent subtitle 1\n1.1 Subtitle 2\nContent subtitle2\n" +
                "1 Title2\nContent title 2 update\n1.1 Subtitle 2",
                doc.updateSection(4, "1 Title2\nContent title 2 update\n1.1 Subtitle 2"));
    }
}
