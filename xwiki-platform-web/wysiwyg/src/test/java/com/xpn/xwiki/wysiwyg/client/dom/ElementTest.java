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
package com.xpn.xwiki.wysiwyg.client.dom;

import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;

/**
 * Unit tests for {@link Element}.
 * 
 * @version $Id$
 */
public class ElementTest extends AbstractWysiwygClientTest
{
    /**
     * Unit test for {@link Element#xGetString()} when elements don't have meta data.
     */
    public void testXGetStringWithoutMetaData()
    {
        Element element = Document.get().createDivElement().cast();
        element.setInnerHTML("a<!--x--><em r=\"s\"><br/><!--y-->b<del>c</del></em>");
        element.setAttribute("q", "p");
        assertEquals("<div q=\"p\">a<!--x--><em r=\"s\"><br><!--y-->b<del>c</del></em></div>", element.xGetString()
            .toLowerCase());
    }

    /**
     * Unit test for {@link Element#xGetString()} when some elements have meta data.
     */
    public void testXGetStringWithMetaData()
    {
        Element container = Document.get().createDivElement().cast();
        container.xSetInnerHTML("<!--p--><span i=\"j\">" + Element.INNER_HTML_PLACEHOLDER + "</span><!--q-->");
        DocumentFragment metaData = container.extractContents();

        container.setInnerHTML("x<a href=\"about:blank\">y</a>z");
        container.setAttribute("k", "l");

        Element anchor = container.getChildNodes().getItem(1).cast();
        anchor.setMetaData(metaData);

        assertEquals("<div k=\"l\">x<!--p--><span i=\"j\"><a href=\"about:blank\">y</a></span><!--q-->z</div>",
            container.xGetString().toLowerCase());
    }

    /**
     * Unit test for {@link Element#xGetInnerHTML()} when the inner text nodes contain HTML special characters.
     */
    public void testXGetInnerHTMLWithHTMLSpecialChars()
    {
        Element element = Document.get().createDivElement().cast();
        element.appendChild(Document.get().createTextNode("<\"'>&"));
        element.appendChild(Document.get().createBRElement());
        assertEquals("&lt;\"'&gt;&amp;<br>", element.xGetInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link Element#xGetInnerText()} when the element contains comment nodes.
     * 
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3275
     */
    public void testXGetInnerTextWithCommentedText()
    {
        Element element = Document.get().createDivElement().cast();
        element.setInnerHTML("x<!--y-->z");
        assertEquals("xz", element.xGetInnerText());
    }
}
