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
package org.xwiki.validator.framework;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xwiki.validator.ValidationError;

/**
 * @version $Id$
 */
public abstract class AbstractHTML5Validator extends AbstractXMLValidator
{
    /**
     * Submit.
     */
    protected static final String SUBMIT = "submit";

    /**
     * Style.
     */
    protected static final String STYLE = "style";

    /**
     * Image.
     */
    protected static final String IMAGE = "image";

    /**
     * Reset.
     */
    protected static final String RESET = "reset";

    /**
     * Button.
     */
    protected static final String BUTTON = "button";

    /**
     * mailto.
     */
    protected static final String MAILTO = "mailto:";

    /**
     * hidden.
     */
    protected static final String HIDDEN = "hidden";

    // Elements.

    /**
     * HTML element.
     */
    protected static final String ELEM_HTML = "html";

    /**
     * Body element.
     */
    protected static final String ELEM_BODY = "body";

    /**
     * Heading 1 element.
     */
    protected static final String ELEM_H1 = "h1";

    /**
     * Line break element.
     */
    protected static final String ELEM_BR = "br";

    /**
     * Bold element.
     */
    protected static final String ELEM_BOLD = "b";

    /**
     * Italics element.
     */
    protected static final String ELEM_ITALIC = "i";

    /**
     * Submit element.
     */
    protected static final String ELEM_SUBMIT = SUBMIT;

    /**
     * Frameset element.
     */
    protected static final String ELEM_FRAMESET = "frameset";

    /**
     * Frame element.
     */
    protected static final String ELEM_FRAME = "frame";

    /**
     * Iframe element.
     */
    protected static final String ELEM_IFRAME = "iframe";

    /**
     * Link element.
     */
    protected static final String ELEM_LINK = "a";

    /**
     * Input element.
     */
    protected static final String ELEM_INPUT = "input";

    /**
     * Image element.
     */
    protected static final String ELEM_IMG = "img";

    /**
     * Area element.
     */
    protected static final String ELEM_AREA = "area";

    /**
     * Table element.
     */
    protected static final String ELEM_TABLE = "table";

    /**
     * Table Header element.
     */
    protected static final String ELEM_TH = "th";

    /**
     * Form element.
     */
    protected static final String ELEM_FORM = "form";

    /**
     * Fieldset element.
     */
    protected static final String ELEM_FIELDSET = "fieldset";

    /**
     * Fieldset element.
     */
    protected static final String ELEM_META = "meta";

    // Attributes.

    /**
     * Type attribute.
     */
    protected static final String ATTR_TYPE = "type";

    /**
     * Type attribute.
     */
    protected static final String ATTR_ALT = "alt";

    /**
     * Href attribute.
     */
    protected static final String ATTR_HREF = "href";

    /**
     * Blur attribute.
     */
    protected static final String ATTR_BLUR = "onblur";

    /**
     * Change attribute.
     */
    protected static final String ATTR_CHANGE = "onchange";

    /**
     * Click attribute.
     */
    protected static final String ATTR_CLICK = "onclick";

    /**
     * Focus attribute.
     */
    protected static final String ATTR_FOCUS = "onfocus";

    /**
     * Load attribute.
     */
    protected static final String ATTR_LOAD = "onload";

    /**
     * Mouseover attribute.
     */
    protected static final String ATTR_MOUSEOVER = "onmouseover";

    /**
     * Select attribute.
     */
    protected static final String ATTR_SELECT = "onselect";

    /**
     * Submmit attribute.
     */
    protected static final String ATTR_SUBMIT = "onsubmit";

    /**
     * Unload attribute.
     */
    protected static final String ATTR_UNLOAD = "unload";

    /**
     * Accesskey attribute.
     */
    protected static final String ATTR_ACCESSKEY = "accesskey";

    /**
     * Scope attribute.
     */
    protected static final String ATTR_SCOPE = "scope";

    /**
     * ID attribute.
     */
    protected static final String ATTR_ID = "id";

    /**
     * Content attribute.
     */
    protected static final String ATTR_CONTENT = "content";

    /**
     * Charset attribute.
     */
    protected static final String ATTR_CHARSET = "charset";

    /**
     * Document parsed with JSoup.
     */
    protected Document html5Document;

    @Override
    public void setDocument(InputStream document)
    {
        setHTML5Document(document);
    }

    /**
     * Set document to validate.
     *
     * @param document document to validate
     */
    public void setHTML5Document(InputStream document)
    {
        this.html5Document = null;

        if (document != null) {
            try {
                this.html5Document = Jsoup.parse(document, null, "");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @return the parsed html5 document
     */
    public Document getHTML5Document()
    {
        return this.html5Document;
    }

    @Override
    public List<ValidationError> validate()
    {
        if (this.html5Document == null) {
            return this.errorHandler.getErrors();
        }

        this.errorHandler.clear();

        validate(this.document);

        return this.errorHandler.getErrors();
    }

    /**
     * Asserts that a condition is false. If it isn't it puts an error message in the validation results.
     *
     * @param errorType type of the error
     * @param message the message to add
     * @param condition condition to be checked
     */
    protected void assertFalse(ValidationError.Type errorType, String message, boolean condition)
    {
        if (condition) {
            // TODO: handle line/column
            addError(errorType, -1, -1, message);
        }
    }

    /**
     * Asserts that a condition is true. If it isn't it puts an error message in the validation results.
     *
     * @param errorType type of the error
     * @param message the message to add
     * @param condition condition to be checked
     */
    protected void assertTrue(ValidationError.Type errorType, String message, boolean condition)
    {
        if (!condition) {
            // TODO: handle line/column
            addError(errorType, -1, -1, message);
        }
    }

    // Dom utils

    /**
     * Check if the html5Document contains the given element.
     *
     * @param tagName element to search
     * @return true if the html5Document contains the element, false otherwise
     */
    public boolean containsElement(String tagName)
    {
        return !this.html5Document.getElementsByTag(tagName).isEmpty();
    }

    /**
     * Get a list of elements matching a given tag name in the html5Document.
     *
     * @param tagName tag name to search for
     * @return the list of matching elements
     */
    public Elements getElements(String tagName)
    {
        return this.html5Document.getElementsByTag(tagName);
    }

    /**
     * Get all the elements matching one of the given tags.
     *
     * @param tagNames tag names to match
     * @return the list of matching tags
     */
    public Elements getElements(Collection<String> tagNames)
    {
        Elements elements = new Elements();
        for (String tagName : tagNames) {
            elements.addAll(this.html5Document.getElementsByTag(tagName));
        }
        return elements;
    }

    /**
     * Check if an element has an child element with the given tag name.
     *
     * @param element element to analyze
     * @param tagName tag name to search for
     * @return true if the element has an child element with the given tag name
     */
    public boolean hasChildElement(Element element, String tagName)
    {
        return !element.getElementsByTag(tagName).isEmpty();
    }

    /**
     * Get children of a given type.
     *
     * @param element element to search in
     * @param tagName name of the tags to match
     * @return a list of matching tags
     */
    public Elements getChildren(Element element, String tagName)
    {
        return element.getElementsByTag(tagName);
    }

    /**
     * Get the names of all the children elements of an element.
     *
     * @param element parent element
     * @return the names of all the children elements of an element.
     */
    public List<String> getChildrenTagNames(Element element)
    {
        List<String> childrenTagNames = new ArrayList<String>();
        for (Element child : element.getAllElements()) {
            childrenTagNames.add(child.tagName());
        }

        return childrenTagNames;
    }

    /**
     * Check if an element has the given attribute.
     *
     * @param element element to analyze
     * @param attributeName name of the attribute to search
     * @return true if the element has the given attribute, false otherwise
     */
    public static boolean hasAttribute(Element element, String attributeName)
    {
        return element.hasAttr(attributeName);
    }

    /**
     * Get the names of all the attribute of an element.
     *
     * @param element element to analyze
     * @return the names of all the attribute of the given element
     */
    public static List<String> getAttributeNames(Element element)
    {
        List<String> attributeNames = new ArrayList<String>();

        for (Attribute attribute : element.attributes()) {
            attributeNames.add(attribute.getKey());
        }

        return attributeNames;
    }

    /**
     * AbstractDOMValidator Get the value of an element attribute.
     *
     * @param element element to analyze
     * @param attributeName name of the attribute to search
     * @return the value of the given attribute
     */
    public static String getAttributeValue(Element element, String attributeName)
    {
        return element.attr(attributeName);
    }

    /**
     * Retrieve a list of values of an attribute for a list of elements.
     *
     * @param elements the list of elements to get the attribute from
     * @param attributeName name of the attribute to retrieve the value from
     * @return the list of values of the given attribute for all the elements in the given element list
     */
    public static List<String> getAttributeValues(Elements elements, String attributeName)
    {
        List<String> results = new ArrayList<String>();
        for (Element element : elements) {
            results.add(element.attr(attributeName));
        }
        return results;
    }

    /**
     * @param tagName name of the tag to match
     * @return The first element found for the given tag name in the XHTML html5Document.
     */
    public Element getElement(String tagName)
    {
        return getElements(tagName).first();
    }
}
