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
 *
 * @author ludovic
 * @author vmassol
 * @author sdumitriu
 * @author tepich
 */

package com.xpn.xwiki.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.oro.text.regex.MalformedPatternException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.TOCGenerator;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.XWikiServletURLFactory;
import com.xpn.xwiki.web.XWikiURLFactory;

public class UtilTest extends HibernateClassesTest {

    public void testTopicInfo() throws IOException {
        String topicinfo;
        Hashtable params;

        topicinfo = "author=\"ludovic\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("author").equals("ludovic"));
        topicinfo = "author=\"ludovic\" date=\"1026671586\" format=\"1.0beta2\" version=\"1.1\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("author").equals("ludovic"));
        assertTrue(params.get("date").equals("1026671586"));
        assertTrue(params.get("format").equals("1.0beta2"));
        assertTrue(params.get("version").equals("1.1"));
        topicinfo = "author=\"Ludovic Dubost\" format=\"1.0 beta\" version=\"1.2\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("author").equals("Ludovic Dubost"));
        assertTrue(params.get("format").equals("1.0 beta"));
        assertTrue(params.get("version").equals("1.2"));
        topicinfo = "test=\"%_Q_%Toto%_Q_%\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("test").equals("\"Toto\""));
        topicinfo = "test=\"Ludovic%_N_%Dubost\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("test").equals("Ludovic\nDubost"));
        topicinfo = "   test=\"Ludovic%_N_%Dubost\"   ";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("test").equals("Ludovic\nDubost"));
    }

    public void testgetDocumentFromPath() throws XWikiException {
        String path = "/view/Main/WebHome";
        XWikiDocument doc = getXWiki().getDocumentFromPath(path, getXWikiContext());
        assertEquals("Doc web is not correct", "Main", doc.getWeb());
        assertEquals("Doc name is not correct", "WebHome", doc.getName());
         path = "/view/Main/WebHome/taratata.doc";
        doc = getXWiki().getDocumentFromPath(path, getXWikiContext());
        assertEquals("Doc web is not correct", "Main", doc.getWeb());
        assertEquals("Doc name is not correct", "WebHome", doc.getName());
         path = "/view/Main/WebHome/blabla/tfdfdf.doc";
        doc = getXWiki().getDocumentFromPath(path, getXWikiContext());
        assertEquals("Doc web is not correct", "Main", doc.getWeb());
        assertEquals("Doc name is not correct", "WebHome", doc.getName());
        path = "/view/Test/Titi/taratata.doc";
       doc = getXWiki().getDocumentFromPath(path, getXWikiContext());
       assertEquals("Doc web is not correct", "Test", doc.getWeb());
       assertEquals("Doc name is not correct", "Titi", doc.getName());

    }

    public void testGetMatches() throws MalformedPatternException {
      String pattern = "#include(Topic|Form)\\(\"(.*?)\"\\)";
      List list = getXWikiContext().getUtil().getMatches("", pattern, 2);
      assertEquals("List should have not items", 0, list.size());
      list = getXWikiContext().getUtil().getMatches("Hello#includeTopic(\"Main.Toto\")Hi", pattern, 2);
      assertEquals("List should have one items", 1, list.size());
      assertEquals("List item 1 should be Main.Toto", "Main.Toto", list.get(0));
      list = getXWikiContext().getUtil().getMatches("Hello#includeTopic(\"Main.Toto\")Hi#includeForm(\"Main.Toto\")Hi", pattern, 2);
      assertEquals("List should have one items", 1, list.size());
      assertEquals("List item 1 should be Main.Toto", "Main.Toto", list.get(0));
      list = getXWikiContext().getUtil().getMatches("Hello#includeTopic(\"Main.Toto\")Hi#includeForm(\"XWiki.Tata\")Hi", pattern, 2);
      assertEquals("List should have two items", 2, list.size());
      assertEquals("List item 1 should be Main.Toto", "Main.Toto", list.get(0));
      assertEquals("List item 2 should be XWiki.Tata", "XWiki.Tata", list.get(1));
    }

    public void testSubstitute() {
      Util util = new Util();
      String result = util.substitute("hello", "Hello", "hello how are you. hello how are you");
      assertEquals("Wrong result", "Hello how are you. Hello how are you", result);

    }

    public void testServletURLFactory() throws MalformedURLException {
        URL url = new URL("http://www.xwiki.org/xwiki/bin/view/Main/WebHome");
        XWikiURLFactory factory = new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/");
        getXWikiContext().setURL(url);
        getXWikiContext().setURLFactory(factory);

        assertEquals("URL is incorrect",
                     "http://www.xwiki.org/xwiki/bin/view/Main/Toto",
                     factory.createURL("Main", "Toto", getXWikiContext()).toString());
        assertEquals("URL is incorrect",
                     "http://www.xwiki.org/xwiki/bin/view/XWiki/Toto",
                     factory.createURL("XWiki", "Toto", getXWikiContext()).toString());
        assertEquals("URL is incorrect",
                     "http://www.xwiki.org/xwiki/bin/edit/XWiki/Toto",
                     factory.createURL("XWiki", "Toto", "edit", getXWikiContext()).toString());
        assertEquals("URL is incorrect",
                     "http://www.xwiki.org/xwiki/bin/edit/XWiki/Toto?raw=1",
                     factory.createURL("XWiki", "Toto", "edit", "raw=1", null, getXWikiContext()).toString());
    }
    
    public void testTOCGeneration() {
      String content = "1.1 a\n1.1 b\n1.1.1 c\n1.1 d\n1 a\n1.1.1.1 f\n1.1.1.1 g\n1.1 h\n1.1 i\n1.1.1.1.1.1 j";

      // test init level 1
      Map result = TOCGenerator.generateTOC(content, 1, 6, true, getXWikiContext());
      assertEquals (((Map) result.get("a")).get(TOCGenerator.TOC_DATA_NUMBERING), "1.1");
      assertEquals (((Map) result.get("b")).get(TOCGenerator.TOC_DATA_NUMBERING), "1.2");
      assertEquals (((Map) result.get("c")).get(TOCGenerator.TOC_DATA_NUMBERING), "1.2.1");
      assertEquals (((Map) result.get("d")).get(TOCGenerator.TOC_DATA_NUMBERING), "1.3");
      assertEquals (((Map) result.get("a-1")).get(TOCGenerator.TOC_DATA_NUMBERING), "2");
      assertEquals (((Map) result.get("f")).get(TOCGenerator.TOC_DATA_NUMBERING), "2.1.1.1");
      assertEquals (((Map) result.get("g")).get(TOCGenerator.TOC_DATA_NUMBERING), "2.1.1.2");
      assertEquals (((Map) result.get("h")).get(TOCGenerator.TOC_DATA_NUMBERING), "2.2");
      assertEquals ((((Map) result.get("i")).get(TOCGenerator.TOC_DATA_NUMBERING)), "2.3");
      assertEquals ((((Map) result.get("j")).get(TOCGenerator.TOC_DATA_NUMBERING)), "2.3.1.1.1.1");
      
      // test init level 2
      result = TOCGenerator.generateTOC(content, 2, 6, true, getXWikiContext());
      assertEquals ((((Map) result.get("a")).get(TOCGenerator.TOC_DATA_NUMBERING)), "1");
      assertEquals (((Map) result.get("b")).get(TOCGenerator.TOC_DATA_NUMBERING), "2");
      assertEquals (((Map) result.get("c")).get(TOCGenerator.TOC_DATA_NUMBERING), "2.1");
      assertEquals (((Map) result.get("d")).get(TOCGenerator.TOC_DATA_NUMBERING), "3");
      assertNull(result.get("a-1")); 
      assertEquals (((Map) result.get("f")).get(TOCGenerator.TOC_DATA_NUMBERING), "3.1.1");
      assertEquals (((Map) result.get("g")).get(TOCGenerator.TOC_DATA_NUMBERING), "3.1.2");
      assertEquals (((Map) result.get("h")).get(TOCGenerator.TOC_DATA_NUMBERING), "4");
      assertEquals ((((Map) result.get("i")).get(TOCGenerator.TOC_DATA_NUMBERING)), "5");
      assertEquals ((((Map) result.get("j")).get(TOCGenerator.TOC_DATA_NUMBERING)), "5.1.1.1.1");
      
      // test max level 3
      result = TOCGenerator.generateTOC(content, 1, 3, true, getXWikiContext());
      assertEquals ((((Map) result.get("a")).get(TOCGenerator.TOC_DATA_NUMBERING)), "1.1");
      assertEquals (((Map) result.get("b")).get(TOCGenerator.TOC_DATA_NUMBERING), "1.2");
      assertEquals (((Map) result.get("c")).get(TOCGenerator.TOC_DATA_NUMBERING), "1.2.1");
      assertEquals (((Map) result.get("d")).get(TOCGenerator.TOC_DATA_NUMBERING), "1.3");
      assertEquals (((Map) result.get("a-1")).get(TOCGenerator.TOC_DATA_NUMBERING), "2");
      assertNull(result.get("f"));
      assertNull(result.get("g"));
      assertEquals (((Map) result.get("h")).get(TOCGenerator.TOC_DATA_NUMBERING), "2.1");
      assertEquals ((((Map) result.get("i")).get(TOCGenerator.TOC_DATA_NUMBERING)), "2.2");
      assertNull(result.get("j"));
    }



    public void testSecureLaszloCode() throws XWikiException {
        testSecureLaszloCode("<image src=\"../../../toto.gif\" />");
        testSecureLaszloCode("<image src=../../../toto.gif />");
        testSecureLaszloCode("<image src=\"/../../../toto.gif\" />");
        testSecureLaszloCode("<image src=\"../../../toto.gif\" />");
        testSecureLaszloCode("<image src=\"..../../../toto.gif\" />");
        testSecureLaszloCode("<image anything=\"../../../toto.gif\" />");
    }

    private void testSecureLaszloCode(String data) throws XWikiException {
        try {
            String result = Util.secureLaszloCode(data);
            assertTrue("Code is not secure", (result.indexOf("..")==-1));
        } catch (XWikiException e) {
            if ((e.getCode()!=XWikiException.ERROR_LASZLO_INVALID_DOTDOT)
                &&(e.getCode()!=XWikiException.ERROR_LASZLO_INVALID_XML))
                throw e;
        }
    }

}
