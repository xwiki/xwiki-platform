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
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

/**
 * Unit tests for {@link Element}.
 * 
 * @version $Id$
 */
public class ElementTest extends DOMTestCase
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
        Element element = getDocument().createDivElement().cast();
        element.setInnerHTML("a<!--x--><em r=\"s\"><br/><!--y-->b<span>c</span></em>");
        element.setAttribute("q", "p");
        assertEquals("<div q=\"p\">a<!--x--><em r=\"s\"><br><!--y-->b<span>c</span></em></div>", normalizeHTML(element
            .xGetString()));
    }

    /**
     * Unit test for {@link Element#xGetString()} when some elements have meta data.
     */
    public void testXGetStringWithMetaData()
    {
        Element container = getDocument().createDivElement().cast();
        container.xSetInnerHTML("<!--p--><span i=\"j\">" + Element.INNER_HTML_PLACEHOLDER + "</span><!--q-->");
        DocumentFragment metaData = container.extractContents();

        container.setInnerHTML("x<a href=\"about:blank\">y</a>z");
        container.setAttribute("k", "l");

        Element anchor = container.getChildNodes().getItem(1).cast();
        anchor.setMetaData(metaData);

        assertEquals("<div k=\"l\">x<!--p--><span i=\"j\"><a href=\"about:blank\">y</a></span><!--q-->z</div>",
            container.xGetString().trim().toLowerCase());
    }

    /**
     * Unit test for {@link Element#xGetInnerHTML()} when the inner text nodes contain HTML special characters.
     */
    public void testXGetInnerHTMLWithHTMLSpecialChars()
    {
        getContainer().appendChild(getDocument().createTextNode("<\"'>&"));
        getContainer().appendChild(getDocument().createBRElement());
        assertEquals("&lt;\"'&gt;&amp;<br>", normalizeHTML(getContainer().xGetInnerHTML()));
    }

    /**
     * Unit test for {@link Element#xGetInnerText()} when the element contains comment nodes.
     * 
     * See http://code.google.com/p/google-web-toolkit/issues/detail?id=3275.
     */
    public void testXGetInnerTextWithCommentedText()
    {
        getContainer().setInnerHTML("x<!--y-->z");
        assertEquals("xz", getContainer().xGetInnerText());
    }

    /**
     * Unit test for {@link Element#xSetInnerHTML(String)} when the input HTML starts and ends with comment nodes.
     */
    public void testXSetInnerHTMLWithComments()
    {
        String html = "<!--x--><span>y</span><!--z-->";
        Element element = getDocument().createDivElement().cast();
        element.xSetInnerHTML(html);
        assertEquals(html, element.getInnerHTML().toLowerCase());

        DocumentFragment contents = element.extractContents();
        assertEquals(html, contents.getInnerHTML().toLowerCase());

        getContainer().setInnerHTML("ab<em>c</em><ins></ins>ij");
        DOMUtils.getInstance().insertAt(getContainer(), contents, 2);
        assertEquals("ab<em>c</em>" + html + "<ins></ins>ij", getContainer().getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link Element#xGetString()} on block-level elements to see if the white spaces added by some
     * browsers to format the HTML are removed.
     */
    public void testXGetStringOnBlockLevelElements()
    {
        String html = "<h1>header</h1><p>paragraph</p><ul><li>list</li></ul>";
        Element element = getDocument().createDivElement().cast();
        element.xSetInnerHTML(html);
        assertEquals("<div>" + html + "</div>", element.xGetString().toLowerCase().replace("\r\n", ""));
    }

    /**
     * Unit test for {@link Element#xGetString()} to see if white spaces inside comment nodes are kept.
     */
    public void testXGetStringPreservesWhiteSpacesInCommentNodes()
    {
        Element element = getDocument().createDivElement().cast();
        String text = "\na \n b\t\n\tc";
        element.appendChild(getDocument().createComment(text));
        assertEquals("<div><!--" + text + "--></div>", element.xGetString().trim().toLowerCase());
    }

    /**
     * Unit test for {@link Element#getAttributeNames()}.
     */
    public void testGetAttributeNames()
    {
        Element element = getDocument().createSpanElement().cast();
        String customAttribute = "x";
        element.setAttribute(customAttribute, "y");
        element.setTitle("z");
        element.getStyle().setBackgroundColor("rgb(255, 0, 0)");
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
     * Unit test for {@link Element#getAttributeNames()}, for the case when the element also has custom properties set,
     * which should be excluded from the attribute names.
     */
    public void testGetAttributeNamesExcludesProperties()
    {
        Element element = getDocument().createSpanElement().cast();
        assertEquals(0, element.getAttributeNames().length());

        String propertyFunc = "xx";
        ((JavaScriptObject) element.cast()).set(propertyFunc, JavaScriptObject.createFunction());
        String propertyObj = "yy";
        ((JavaScriptObject) element.cast()).set(propertyObj, JavaScriptObject.createObject());

        assertEquals(0, element.getAttributeNames().length());
    }

    /**
     * Unit test for {@link Element#hasClassName(String)}.
     */
    public void testHasClassName()
    {
        String className = "listItem-selected";

        Element element = getDocument().createSpanElement().cast();
        assertFalse(element.hasClassName(className));

        element.setClassName(className);
        assertFalse(element.hasClassName("listItem"));
        assertTrue(element.hasClassName(className));

        element.setClassName(className + "   gwtListItem  ");
        assertTrue(element.hasClassName(className));
        assertFalse(element.hasClassName(className.toUpperCase()));
        assertTrue(element.hasClassName("gwtListItem"));
    }

    /**
     * Unit test for {@link Element#addClassName(String)}.
     */
    public void testAddClassName()
    {
        Element element = getDocument().createSpanElement().cast();
        element.addClassName("macro-selected");
        element.addClassName("macro");
        element.addClassName("macro ");
        // Element#addClassName(String) is not null-safe anymore (since GWT2.0).
        // element.addClassName(null);
        assertEquals("macro-selected macro", element.getClassName());
    }

    /**
     * Unit test for {@link Element#removeClassName(String)}.
     */
    public void testRemoveClassName()
    {
        Element element = getDocument().createSpanElement().cast();
        element.removeClassName("color");
        assertEquals("", element.getClassName());

        element.setClassName("colorCell  colorCell-selected   ");
        element.removeClassName("Cell");
        element.removeClassName("colorCell");
        assertEquals("colorCell-selected", element.getClassName());

        // Element#removeClassName(String) is not null-safe anymore (since GWT2.0).
        // element.removeClassName(null);
        element.removeClassName(" colorCell-selected");
        assertEquals("", element.getClassName());
    }

    /**
     * Unit test for {@link Element#xHasAttribute(String)}.
     */
    public void testXHasAttribute()
    {
        Element element = getDocument().createSpanElement().cast();

        assertFalse(element.xHasAttribute(CLASS_ATTRIBUTE));
        assertFalse(element.xHasAttribute(STYLE_ATTRIBUTE));
        assertFalse(element.xHasAttribute(TITLE_ATTRIBUTE));
        assertFalse(element.xHasAttribute(ID_ATTRIBUTE));

        String customAttribute = "from";
        assertFalse(element.xHasAttribute(customAttribute));

        element.setClassName("xyz");
        assertTrue(element.xHasAttribute(CLASS_ATTRIBUTE));

        element.getStyle().setFontWeight(FontWeight.BOLD);
        assertTrue(element.xHasAttribute(STYLE_ATTRIBUTE));

        element.setTitle("abc");
        assertTrue(element.xHasAttribute(TITLE_ATTRIBUTE));

        element.setId("x11");
        assertTrue(element.xHasAttribute(ID_ATTRIBUTE));

        element.setAttribute(customAttribute, "me");
        assertTrue(element.xHasAttribute(customAttribute));
    }

    /**
     * Unit test for {@link Element#xSetAttribute(String, String)}.
     */
    public void testXSetAttribute()
    {
        Element element = getDocument().createSpanElement().cast();

        testXSetXGetAttribute(element, TITLE_ATTRIBUTE, "123");
        testXSetXGetAttribute(element, ID_ATTRIBUTE, "qwe");
        testXSetXGetAttribute(element, CLASS_ATTRIBUTE, "def");

        element.xSetAttribute(STYLE_ATTRIBUTE, "font-weight: bold;");
        assertEquals("font-weight: bold", element.xGetAttribute(STYLE_ATTRIBUTE).toLowerCase().replace(";", ""));
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

    /**
     * Clones an element, removes an attribute from the clone and checks if the original element is affected.
     */
    public void testRemoveAttributeFromClone()
    {
        // Create an element and set a custom attribute on it.
        String alice = "alice";
        String bob = "bob";
        Element element = Element.as(getDocument().createSpanElement());
        element.setAttribute(alice, alice);
        element.setAttribute(bob, bob);

        // At this point, although we set the attribute, the attribute node was not created.
        // It seems that IE7 creates the attribute node the first time we access it.
        // This line calls in behind getAttributeNode which creates the attribute node at first call.
        assertTrue(element.xHasAttribute(alice));
        assertTrue(element.xHasAttribute(bob));

        // Now let's clone the element.
        Element clone = Element.as(element.cloneNode(true));
        assertTrue(clone.xHasAttribute(alice));
        assertTrue(clone.xHasAttribute(bob));

        // Remove the attribute from the clone. It seems that IE7 doesn't clone the attribute node, it only copies the
        // reference. As a consequence removing the attribute from the clone affects the original element. Note that if
        // we don't access the attribute node on the original element the attribute node is not created thus not copied
        // to the clone. This way the clone has its own attribute which we can safely remove without affecting the
        // original element.
        clone.xRemoveAttribute(alice);
        element.xRemoveAttribute(bob);

        assertTrue(clone.xHasAttribute(bob));
        assertTrue(element.xHasAttribute(alice));
    }

    /**
     * Tests that the computed style is correctly determined when the style property is explicitly set to {@code
     * inherit}.
     * <p>
     * Note: We don't run this test with HtmlUnit because the font-family of the inner element is incorrectly computed
     * as {@code inherit}. We fixed this bug for IE.
     */
    @DoNotRunWith(Platform.HtmlUnitBug)
    public void testGetComputedStyleWhenPropertyIsExplicitlySetToInherit()
    {
        Element inner = Element.as(getDocument().createSpanElement());
        inner.getStyle().setProperty(Style.FONT_FAMILY.getJSName(), Style.Float.INHERIT);
        inner.appendChild(getDocument().createTextNode("computed"));

        Element outer = Element.as(getDocument().createSpanElement());
        outer.getStyle().setProperty(Style.FONT_FAMILY.getJSName(), "comic sans");
        outer.appendChild(inner);

        getContainer().appendChild(outer);
        assertEquals(outer.getComputedStyleProperty(Style.FONT_FAMILY.getJSName()), inner
            .getComputedStyleProperty(Style.FONT_FAMILY.getJSName()));
    }
}
