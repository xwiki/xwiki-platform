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
 *
 */
package org.xwiki.xml.internal.html;

import java.io.StringReader;
import java.util.Collections;

import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;
import org.xwiki.xml.html.filter.HTMLFilter;

import org.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Unit tests for {@link org.xwiki.xml.internal.html.DefaultHTMLCleaner}.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public class DefaultHTMLCleanerTest extends AbstractXWikiComponentTestCase
{
    public static final String HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n";

    private static final String HEADER_FULL = HEADER + "<html><head /><body>";

    private static final String FOOTER = "</body></html>\n";

    private HTMLCleaner cleaner;

    protected void setUp() throws Exception
    {
        super.setUp();
        cleaner = (HTMLCleaner) getComponentManager().lookup(HTMLCleaner.ROLE, "default");
    }

    public void testSpecialCharacters()
    {
        // TODO: We still have a problem I think in that if there are characters such as "&" or quote in the source
        // text they are not escaped. This is because we have use "false" in DefaultHTMLCleaner here:
        // Document document = new JDomSerializer(this.cleanerProperties, false).createJDom(cleanedNode);
        // See the problem described here: http://sourceforge.net/forum/forum.php?thread_id=2243880&forum_id=637246
        assertHTML("<p>&quot;&amp;**notbold**&lt;notag&gt;&nbsp;</p>", "<p>&quot;&amp;**notbold**&lt;notag&gt;&nbsp;</p>");
        assertHTML("<p>\"&amp;</p>", "<p>\"&</p>");
        assertHTML("<p><img src=\"http://host.com/a.gif?a=foo&amp;b=bar\" /></p>", "<img src=\"http://host.com/a.gif?a=foo&b=bar\" />");
        assertHTML("<p>&#xA;</p>", "<p>&#xA;</p>");
}

    public void testCloseUnbalancedTags()
    {
        assertHTML("<hr /><p>hello</p>", "<hr><p>hello");
    }

    public void testConversionsFromHTML()
    {
        assertHTML("<p>this <strong>is</strong> bold</p>", "this <b>is</b> bold");
        assertHTML("<p><em>italic</em></p>", "<i>italic</i>");
        assertHTML("<del>strike</del>", "<strike>strike</strike>");
        assertHTML("<del>strike</del>", "<s>strike</s>");
        assertHTML("<ins>strike</ins>", "<u>strike</u>");
        assertHTML("<p style=\"text-align:center\">center</p>", "<center>center</center>");
        assertHTML("<p><span style=\"color:red;font-family=arial;font-size=3pt;\">This is some text!</span></p>",
            "<font face=\"arial\" size=\"3\" color=\"red\">This is some text!</font>");
    }

    public void testConvertImplicitParagraphs()
    {
        assertHTML("<p>word1</p><p>word2</p><p>word3</p><hr /><p>word4</p>", "word1<p>word2</p>word3<hr />word4");
        
        // Don't convert when there are only spaces or new lines
        assertHTML("<p>word1</p>  \n  <p>word2</p>", "<p>word1</p>  \n  <p>word2</p>");
        
        // Ensure that whitespaces at the end works.
        assertHTML("\n ", "\n ");
        
        // Ensure that comments are not wrapped
        assertHTML("<!-- comment1 -->\n<p>hello</p>\n<!-- comment2 -->", 
            "<!-- comment1 -->\n<p>hello</p>\n<!-- comment2 -->");
    }

    public void testCleanNonXHTMLLists()
    {
        assertHTML("<ul><li>item1<ul><li>item2</li></ul></li></ul>", "<ul><li>item1</li><ul><li>item2</li></ul></ul>");
        assertHTML("<ul><li>item1<ul><li>item2<ul><li>item3</li></ul></li></ul></li></ul>",
            "<ul><li>item1</li><ul><li>item2</li><ul><li>item3</li></ul></ul></ul>");
        assertHTML("<ul><li><ul><li>item</li></ul></li></ul>", "<ul><ul><li>item</li></ul></ul>");
        assertHTML("<ul> <li><ul><li>item</li></ul></li></ul>", "<ul> <ul><li>item</li></ul></ul>");
        assertHTML("<ul><li>item1<ol><li>item2</li></ol></li></ul>", "<ul><li>item1</li><ol><li>item2</li></ol></ul>");
        assertHTML("<ol><li>item1<ol><li>item2<ol><li>item3</li></ol></li></ol></li></ol>",
            "<ol><li>item1</li><ol><li>item2</li><ol><li>item3</li></ol></ol></ol>");
        assertHTML("<ol><li><ol><li>item</li></ol></li></ol>", "<ol><ol><li>item</li></ol></ol>");
        assertHTML("<ul><li>item1<ul><li><ul><li>item2</li></ul></li><li>item3</li></ul></li></ul>",
            "<ul><li>item1</li><ul><ul><li>item2</li></ul><li>item3</li></ul></ul>");
    }

    /**
     * Verify that scripts are not cleaned and that we can have a CDATA section inside. Also verify CADATA behaviors.
     */
    public void testScriptAndCData()
    {
        String content = "<script type=\"text/javascript\">//<![CDATA[alert(\"Hello World\")// ]]></script>";
        assertHTML(content, content);
        
        content = "<p><![CDATA[&]]></p>";
        assertHTML(content, content);

        assertHTML("<p>&amp;<![CDATA[&]]>&amp;</p>", "<p>&<![CDATA[&]]>&</p>");
    }

    /**
     * Verify that we can control what filters are used for cleaning.
     */
    public void testExplicitFilterList()
    {
        HTMLCleanerConfiguration configuration = this.cleaner.getDefaultConfiguration();
        configuration.setFilters(Collections.<HTMLFilter>emptyList());
        String result = XMLUtils.toString(this.cleaner.clean(new StringReader("something"), configuration));
        // Note that if the default Body filter had been executed the result would have been:
        // <p>something</p>.
        assertEquals(HEADER_FULL + "something" + FOOTER, result);
    }
    
    private void assertHTML(String expected, String actual)
    {
        assertEquals(HEADER_FULL + expected + FOOTER, XMLUtils.toString(this.cleaner.clean(new StringReader(actual))));
    }
}
