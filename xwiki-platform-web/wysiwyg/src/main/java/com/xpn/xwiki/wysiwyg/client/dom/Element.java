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
    public static final String INNER_HTML_PLACEHOLDER = "com.xpn.xwiki.wysiwyg.client.dom.Element#placeholder";

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
     * be inherited or assumed by default on this element.
     * 
     * @param propertyName the name of the CSS property whose value is returned.
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
            StringBuffer innerHTML = new StringBuffer();
            Node child = getFirstChild();
            do {
                switch (child.getNodeType()) {
                    case Node.TEXT_NODE:
                        innerHTML.append(child.getNodeValue());
                        break;
                    case Node.ELEMENT_NODE:
                        innerHTML.append(Element.as(child).xGetString());
                        break;
                    default:
                        Element container = ((Document) getOwnerDocument()).xCreateDivElement().cast();
                        container.appendChild(child.cloneNode(true));
                        innerHTML.append(container.getInnerHTML());
                        break;
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
        if (hasChildNodes()) {
            Element clone = Element.as(cloneNode(false));
            clone.appendChild(getOwnerDocument().createTextNode(INNER_HTML_PLACEHOLDER));
            outerHTML = clone.getString();
            outerHTML = outerHTML.replace(INNER_HTML_PLACEHOLDER, xGetInnerHTML());
        } else {
            outerHTML = getString();
        }
        DocumentFragment metaData = getMetaData();
        if (metaData != null) {
            return metaData.getInnerHTML().replace(INNER_HTML_PLACEHOLDER, outerHTML);
        } else {
            return outerHTML;
        }
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
     * @return the meta data associated with this element.
     */
    public final native DocumentFragment getMetaData()
    /*-{
        return this.metaData;
    }-*/;

    /**
     * Sets the meta data of this element.
     * 
     * @param metaData a document fragment with additional information regarding this element.
     */
    public final native void setMetaData(DocumentFragment metaData)
    /*-{
        this.metaData = metaData;
    }-*/;

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
     * Clones this element along with its meta data.
     * 
     * @param deep if true then all the descendants of this element will be cloned too.
     * @return the clone.
     * @see #cloneNode(boolean)
     */
    public final Node xCloneNode(boolean deep)
    {
        Element clone = cloneNode(deep).cast();
        clone.setMetaData(getMetaData());
        return clone;
    }
}
