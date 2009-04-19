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
package org.xwiki.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JsArrayString;

/**
 * Unit tests for {@link Element}.
 * 
 * @version $Id$
 */
public class ElementTest extends AbstractDOMTest
{
    /**
     * The name of the class attribute.
     */
    public static final String CLASS_ATTRIBUTE = "class";

    /**
     * The name of the style attribute.
     */
    public static final String STYLE_ATTRIBUTE = "style";

    /**
     * The name of the title attribute.
     */
    public static final String TITLE_ATTRIBUTE = "title";

    /**
     * The name of the ID attribute.
     */
    public static final String ID_ATTRIBUTE = "id";

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

    /**
     * Unit test for {@link Element#xSetInnerHTML(String)} when the input HTML starts and ends with comment nodes.
     */
    public void testXSetInnerHTMLWithComments()
    {
        String html = "<!--x--><span>y</span><!--z-->";
        Element element = Document.get().createDivElement().cast();
        element.xSetInnerHTML(html);
        assertEquals(html, element.getInnerHTML().toLowerCase());

        DocumentFragment contents = element.extractContents();
        assertEquals(html, contents.getInnerHTML().toLowerCase());

        Element container = Document.get().createDivElement().cast();
        container.setInnerHTML("ab<em>c</em><ins></ins>ij");
        DOMUtils.getInstance().insertAt(container, contents, 2);
        assertEquals("ab<em>c</em>" + html + "<ins></ins>ij", container.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link Element#xGetString()} on block-level elements to see if the white spaces added by some
     * browsers to format the HTML are removed.
     */
    public void testXGetStringOnBlockLevelElements()
    {
        String html = "<h1>header</h1><p>paragraph</p><ul><li>list</li></ul>";
        Element element = Document.get().createDivElement().cast();
        element.xSetInnerHTML(html);
        assertEquals("<div>" + html + "</div>", element.xGetString().toLowerCase());
    }

    /**
     * Unit test for {@link Element#xGetString()} to see if white spaces inside comment nodes are kept.
     */
    public void testXGetStringPreservesWhiteSpacesInCommentNodes()
    {
        Document document = (Document) Document.get();
        Element element = document.createDivElement().cast();
        String text = "\na \n b\t\n\tc";
        element.appendChild(document.createComment(text));
        assertEquals("<div><!--" + text + "--></div>", element.xGetString().toLowerCase());
    }

    /**
     * Unit test for {@link Element#getAttributeNames()}.
     */
    public void testGetAttributeNames()
    {
        Element element = Document.get().createSpanElement().cast();
        String customAttribute = "x";
        element.setAttribute(customAttribute, "y");
        element.setTitle("z");
        element.getStyle().setProperty(Style.BACKGROUND_COLOR, "rgb(255, 0, 0)");
        element.setClassName("test");

        List<String> attrList = new ArrayList<String>();
        attrList.add(customAttribute);
        attrList.add(TITLE_ATTRIBUTE);
        attrList.add(STYLE_ATTRIBUTE);
        attrList.add(CLASS_ATTRIBUTE);

        JsArrayString attributeNames = element.getAttributeNames();
        for (int i = 0; i < attributeNames.length(); i++) {
            attrList.remove(attributeNames.get(i));
        }
        assertEquals(0, attrList.size());
    }

    /**
     * Unit test for {@link Element#hasClassName(String)}.
     */
    public void testHasClassName()
    {
        String className = "listItem-selected";

        Element element = Document.get().createSpanElement().cast();
        assertFalse(element.hasClassName(className));

        element.setClassName(className);
        assertFalse(element.hasClassName("listItem"));
        assertTrue(element.hasClassName(className));

        element.setClassName(className + "   gwtListItem  ");
        assertTrue(element.hasClassName(className));
        assertFalse(element.hasClassName(className.toUpperCase()));
        assertTrue(element.hasClassName("gwtListItem"));

        assertFalse(element.hasClassName(null));
    }

    /**
     * Unit test for {@link Element#addClassName(String)}.
     */
    public void testAddClassName()
    {
        Element element = Document.get().createSpanElement().cast();
        element.addClassName("macro-selected");
        element.addClassName("macro");
        element.addClassName("macro ");
        element.addClassName(null);
        assertEquals("macro-selected macro", element.getClassName());
    }

    /**
     * Unit test for {@link Element#removeClassName(String)}.
     */
    public void testRemoveClassName()
    {
        Element element = Document.get().createSpanElement().cast();
        element.removeClassName("color");
        assertEquals("", element.getClassName());

        element.setClassName("colorCell  colorCell-selected   ");
        element.removeClassName("Cell");
        element.removeClassName("colorCell");
        assertEquals("colorCell-selected", element.getClassName());

        element.removeClassName(null);
        element.removeClassName(" colorCell-selected");
        assertEquals("", element.getClassName());
    }

    /**
     * Unit test for {@link Element#hasAttribute(String)}.
     */
    public void testHasAttribute()
    {
        Element element = Document.get().createSpanElement().cast();

        assertFalse(element.hasAttribute(CLASS_ATTRIBUTE));
        assertFalse(element.hasAttribute(STYLE_ATTRIBUTE));
        assertFalse(element.hasAttribute(TITLE_ATTRIBUTE));
        assertFalse(element.hasAttribute(ID_ATTRIBUTE));

        String customAttribute = "from";
        assertFalse(element.hasAttribute(customAttribute));

        element.setClassName("xyz");
        assertTrue(element.hasAttribute(CLASS_ATTRIBUTE));

        element.getStyle().setProperty(Style.FONT_WEIGHT.getJSName(), Style.FontWeight.BOLD);
        assertTrue(element.hasAttribute(STYLE_ATTRIBUTE));

        element.setTitle("abc");
        assertTrue(element.hasAttribute(TITLE_ATTRIBUTE));

        element.setId("x11");
        assertTrue(element.hasAttribute(ID_ATTRIBUTE));

        element.setAttribute(customAttribute, "me");
        assertTrue(element.hasAttribute(customAttribute));
    }

    /**
     * Unit test for {@link Element#xSetAttribute(String, String)}.
     */
    public void testXSetAttribute()
    {
        Element element = Document.get().createSpanElement().cast();

        testXSetXGetAttribute(element, TITLE_ATTRIBUTE, "123");
        testXSetXGetAttribute(element, ID_ATTRIBUTE, "qwe");
        testXSetXGetAttribute(element, CLASS_ATTRIBUTE, "def");
        testXSetXGetAttribute(element, STYLE_ATTRIBUTE, "font-weight: bold;");
    }

    /**
     * Sets the value of the specified attribute and then checks its value.
     * 
     * @param element a DOM element
     * @param attrName the name of the attribute
     * @param attrValue the value of the attribute
     */
    private void testXSetXGetAttribute(Element element, String attrName, String attrValue)
    {
        element.xSetAttribute(attrName, attrValue);
        assertEquals(attrValue, element.xGetAttribute(attrName).toLowerCase());
    }
}
