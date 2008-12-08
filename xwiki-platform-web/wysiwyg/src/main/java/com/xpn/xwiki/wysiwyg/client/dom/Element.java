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
     */
    public final String xGetInnerHTML()
    {
        // TODO
        return null;
    }

    /**
     * Replaces this element with its child nodes. In other words, all the child nodes of this element are moved to its
     * parent node and the element is removed from its parent.
     */
    public final void unwrap()
    {
        if (this.getParentNode() == null || this.getParentNode().getNodeType() == Node.DOCUMENT_NODE) {
            return;
        }
        Node child = this.getFirstChild();
        while (child != null) {
            this.getParentNode().insertBefore(child, this);
            child = this.getFirstChild();
        }
        this.getParentNode().removeChild(this);
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
}
