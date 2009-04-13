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

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Node;

/**
 * Extends the element implementation provided by GWT to add useful methods. All of them should be removed as soon as
 * they make their way into GWT's API.
 * 
 * @version $Id$
 */
public class Element extends com.google.gwt.dom.client.Element
{
    /**
     * The text used in an element's meta data as a place holder for that element's outer HTML.
     */
    public static final String INNER_HTML_PLACEHOLDER = "org.xwiki.gwt.dom.client.Element#placeholder";

    /**
     * The name of the JavaScript property storing the reference to the meta data.<br/>
     * NOTE: We can't use the same name as for {@link #META_DATA_ATTR} because IE stores attribute values as JavaScript
     * properties of DOM element objects.
     */
    public static final String META_DATA_REF = "metaDataRef";

    /**
     * The name of the DOM attribute storing the HTML of the meta data. This HTML is used to recreate the meta data when
     * an element is cloned or copy&pasted.
     */
    public static final String META_DATA_ATTR = "metadata";

    /**
     * The {@code class} attribute is a space-separated list of CSS class names.
     */
    public static final String CLASS_NAME_SEPARATOR = "\\s+";

    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected Element()
    {
        super();
    }

    /**
     * Casts a {@link Node} to an instance of this type.
     * 
     * @param node the instance to be casted to this type.
     * @return the given object as an instance of {@link Element}.
     */
    public static Element as(Node node)
    {
        return (Element) com.google.gwt.dom.client.Element.as(node);
    }

    /**
     * @return The names of DOM attributes present on this element.
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3054
     */
    public final JsArrayString getAttributeNames()
    {
        return DOMUtils.getInstance().getAttributeNames(this);
    }

    /**
     * Returns the value of the specified CSS property for this element as it is computed by the browser before the
     * element is displayed. The CSS property doesn't have to be applied explicitly or directly on this element. It can
     * be inherited or assumed by default on this element.<br/>
     * NOTE: You have to pass the JavaScript name of the property and not its CSS name. The JavaScript name has camel
     * case style ({@code fontWeight}) and it is used like this {@code object.style.propertyJSName = value}. The CSS
     * name has dash style ({@code font-weight}) and it is used like this {@code propertyCSSName: value;}.
     * 
     * @param propertyName the script name of the CSS property whose value is returned.
     * @return the computed value of the specified CSS property for this element.
     */
    public final String getComputedStyleProperty(String propertyName)
    {
        return DOMUtils.getInstance().getComputedStyleProperty(this, propertyName);
    }

    /**
     * Set inner HTML in cross browser manner and notify the owner document.
     * 
     * @param html the html to set.
     * @see {@link DOMUtils#setInnerHTML(Element, String)}
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3146
     */
    public final void xSetInnerHTML(String html)
    {
        DOMUtils.getInstance().setInnerHTML(this, html);
        ((Document) getOwnerDocument()).fireInnerHTMLChange(this);
    }

    /**
     * @return the extended inner HTML of this element, which includes meta data.
     * @see #getInnerHTML()
     */
    public final String xGetInnerHTML()
    {
        if (getFirstChildElement() == null) {
            return getInnerHTML();
        } else {
            Element container = ((Document) getOwnerDocument()).xCreateDivElement().cast();
            StringBuffer innerHTML = new StringBuffer();
            Node child = getFirstChild();
            do {
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    innerHTML.append(Element.as(child).xGetString());
                } else {
                    container.appendChild(child.cloneNode(true));
                    innerHTML.append(container.getInnerHTML());
                    container.removeChild(container.getLastChild());
                }
                child = child.getNextSibling();
            } while (child != null);
            return innerHTML.toString();
        }
    }

    /**
     * @return the extended outer HTML of this element, which includes meta data.
     * @see #getString()
     */
    public final String xGetString()
    {
        String outerHTML;
        // We need to remove the meta data attribute on serialization
        String metaDataHTML = null;
        if (hasAttribute(META_DATA_ATTR)) {
            metaDataHTML = xGetAttribute(META_DATA_ATTR);
            // Remove the attribute from this element
            removeAttribute(META_DATA_ATTR);
        }
        if (hasChildNodes()) {
            Element clone = Element.as(cloneNode(false));
            clone.appendChild(getOwnerDocument().createTextNode(INNER_HTML_PLACEHOLDER));
            outerHTML = clone.getString();
            outerHTML = outerHTML.replace(INNER_HTML_PLACEHOLDER, xGetInnerHTML());
        } else {
            outerHTML = getString();
        }
        // Some browsers, including IE, format the HTML returned by innerHTML and outerHTML properties by adding new
        // lines or tabs. We have to remove leading and trailing white spaces from the outerHTML because when we reset
        // the innerHTML or outerHTML properties these white spaces can generate additional text nodes which can, for
        // instance, mess up the History mechanism.
        outerHTML = outerHTML.trim();
        if (metaDataHTML != null) {
            // Put the meta data attribute back
            setAttribute(META_DATA_ATTR, metaDataHTML);
            outerHTML = metaDataHTML.replace(INNER_HTML_PLACEHOLDER, outerHTML);
        }
        return outerHTML;
    }

    /**
     * Places all the children of this element in a document fragment and returns it.<br/>
     * NOTE: The element will remain empty after this method call.
     * 
     * @return A document fragment containing all the descendants of this element.
     */
    public final DocumentFragment extractContents()
    {
        DocumentFragment contents = ((Document) getOwnerDocument()).createDocumentFragment();
        Node child = getFirstChild();
        while (child != null) {
            contents.appendChild(child);
            child = getFirstChild();
        }
        return contents;
    }

    /**
     * Replaces this element with its child nodes. In other words, all the child nodes of this element are moved to its
     * parent node and the element is removed from its parent.
     */
    public final void unwrap()
    {
        if (getParentNode() == null || getParentNode().getNodeType() == Node.DOCUMENT_NODE) {
            return;
        }
        getParentNode().replaceChild(extractContents(), this);
    }

    /**
     * Wraps the passed node and takes its place in its parent. In other words, it adds the passed element as a child of
     * this element and replaces it in its parent.
     * 
     * @param node the node to wrap
     */
    public final void wrap(Node node)
    {
        if (node.getParentNode() == null) {
            return;
        }
        node.getParentNode().replaceChild(this, node);
        appendChild(node);
    }

    /**
     * @return the meta data associated with this element.
     */
    public final DocumentFragment getMetaData()
    {
        DocumentFragment metaData = (DocumentFragment) ((JavaScriptObject) cast()).get(META_DATA_REF);
        if (metaData == null) {
            // There's no saved reference to the meta data.
            // Test if this element has stored meta data.
            if (hasAttribute(META_DATA_ATTR)) {
                // This element could be the result of node cloning or copy&paste.
                // Let's update the cached meta data reference.
                Element container = (Element) getOwnerDocument().createDivElement().cast();
                container.xSetInnerHTML(xGetAttribute(META_DATA_ATTR));
                metaData = container.extractContents();
                ((JavaScriptObject) cast()).set(META_DATA_REF, metaData);
            }
        }
        return metaData;
    };

    /**
     * Sets the meta data of this element.
     * 
     * @param metaData a document fragment with additional information regarding this element.
     */
    public final void setMetaData(DocumentFragment metaData)
    {
        // Save a reference to the meta data for fast retrieval.
        ((JavaScriptObject) cast()).set(META_DATA_REF, metaData);
        if (metaData != null) {
            // We have to serialize the meta data and store it using a custom attribute to avoid loosing the meta data
            // over node cloning or copy&paste. The custom attribute used for storing the meta data should be filtered
            // when getting the outer HTML.
            setAttribute(META_DATA_ATTR, metaData.getInnerHTML());
        } else {
            removeAttribute(META_DATA_ATTR);
        }
    };

    /**
     * @return true if HTML Strict DTD specifies that this element must be empty.
     */
    public final boolean mustBeEmpty()
    {
        for (int i = 0; i < DOMUtils.HTML_EMPTY_TAGS.length; i++) {
            if (DOMUtils.HTML_EMPTY_TAGS[i].equalsIgnoreCase(getTagName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the value for the specified attribute in cross browser manner.
     * 
     * @param name the name of the attribute
     * @return the value of the attribute
     * @see DOMUtils#getAttribute(Element, String)
     */
    public final String xGetAttribute(String name)
    {
        return DOMUtils.getInstance().getAttribute(this, name);
    }

    /**
     * We need this method because {@link #getInnerText()} includes commented text in the output.
     * 
     * @return the text between the start and end tags of this element
     * @see #getInnerText()
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3275
     */
    public final String xGetInnerText()
    {
        return DOMUtils.getInstance().getInnerText(this);
    }

    /**
     * @param attrName a string representing the name of an attribute
     * @return true is this element has an attribute with the specified name, false otherwise
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=2852
     */
    public final boolean hasAttribute(String attrName)
    {
        return DOMUtils.getInstance().hasAttribute(this, attrName);
    }

    /**
     * @return {@code true} if this element has any attribute, {@code false} otherwise
     */
    public final boolean hasAttributes()
    {
        return DOMUtils.getInstance().hasAttributes(this);
    }

    /**
     * @param className a {@link String} representing the CSS class to look for
     * @return {@code true} if this element's {@code class} attribute contains the given class name, {@code false}
     *         otherwise
     */
    public final boolean hasClassName(String className)
    {
        if (className == null) {
            return false;
        }
        String trimmedClassName = className.trim();
        String[] classNames = getClassName().split(CLASS_NAME_SEPARATOR);
        for (int i = 0; i < classNames.length; i++) {
            if (classNames[i].equals(trimmedClassName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a new CSS class name to this element's {@code class} attribute. The {@code class} attribute is a
     * space-separated list of CSS class names.
     * 
     * @param className a {@link String} representing the CSS class to be appended to this element's {@code class}
     *            attribute
     * @see #setClassName(String)
     */
    public final void addClassName(String className)
    {
        if (className != null && !hasClassName(className)) {
            String classNames = getClassName().trim();
            setClassName(classNames.length() == 0 ? className.trim() : classNames + " " + className.trim());
        }
    }

    /**
     * Removes a CSS class name form this element's {@code class} attribute. The {@code class} attribute is a
     * space-separated list of CSS class names.
     * 
     * @param className a {@link String} representing the CSS class to be removed from this element's {@code class}
     *            attribute
     * @see #setClassName(String)
     */
    public final void removeClassName(String className)
    {
        if (className == null) {
            return;
        }
        String trimmedClassName = className.trim();
        String[] classNames = getClassName().split(CLASS_NAME_SEPARATOR);
        StringBuffer newClassName = new StringBuffer();
        for (int i = 0; i < classNames.length; i++) {
            if (!classNames[i].equals(trimmedClassName)) {
                newClassName.append(" ");
                newClassName.append(classNames[i]);
            }
        }
        setClassName(newClassName.toString().trim());
    }
}
