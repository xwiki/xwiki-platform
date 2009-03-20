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
package org.xwiki.gwt.dom.client.internal.ie;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Style;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Node;

/**
 * Contains methods from {@link DOMUtils} that require a different implementation in Internet Explorer.
 * 
 * @version $Id$
 */
public class IEDOMUtils extends DOMUtils
{
    /**
     * {@inheritDoc}
     * 
     * @see DOMUtils#getComputedStyleProperty(Element, String)
     */
    public String getComputedStyleProperty(Element el, String propertyName)
    {
        return getComputedStylePropertyIE(el, Style.toCamelCase(propertyName));
    }

    /**
     * @param el The element for which we retrieve the computed value of the given style property.
     * @param propertyName The name of the property in camel case format.
     * @return The computed value of the given style property on the specified element.
     */
    private native String getComputedStylePropertyIE(Element el, String propertyName)
    /*-{
        // We force it to be a string because we treat it as a string in the java code.
        return '' + el.currentStyle[propertyName];
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see DOMUtils#importNode(Document, Node, boolean)
     */
    public Node importNode(Document doc, Node externalNode, boolean deep)
    {
        switch (externalNode.getNodeType()) {
            case Node.TEXT_NODE:
                return doc.createTextNode(externalNode.getNodeValue());
            case Node.ELEMENT_NODE:
                Element externalElement = Element.as(externalNode);
                Element internalElement = doc.xCreateElement(externalElement.getTagName());
                JsArrayString attrNames = getAttributeNames(externalElement);
                for (int i = 0; i < attrNames.length(); i++) {
                    String attrName = attrNames.get(i);
                    internalElement.setAttribute(attrName, externalElement.getAttribute(attrName));
                }
                if (deep) {
                    // TODO
                }
                return internalElement;
            default:
                throw new IllegalArgumentException("Cannot import node of type " + externalNode.getNodeType() + "!");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DOMUtils#getAttributeNames(Element)
     */
    public native JsArrayString getAttributeNames(Element element)
    /*-{
        var attrNames = [];
        for(var i = 0; i < element.attributes.length; i++){
            if(element.attributes[i].specified) {
                attrNames.push(element.attributes[i].nodeName);
            }
        }
        return attrNames;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see DOMUtils#setInnerHTML(Element, String)
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3146
     */
    public void setInnerHTML(Element element, String html)
    {
        element.setInnerHTML("<span>iesucks</span>" + html);
        element.removeChild(element.getFirstChild());
    }

    /**
     * {@inheritDoc}
     * 
     * @see DOMUtils#getAttribute(Element, String)
     */
    public String getAttribute(Element element, String name)
    {
        return element.getAttribute(name) + "";
    }

    /**
     * {@inheritDoc}
     * 
     * @see DOMUtils#getInnerText(Element)
     */
    public native String getInnerText(Element element)
    /*-{
        return element.innerText;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see DOMUtils#hasAttribute(Element, String)
     */
    public native boolean hasAttribute(Element element, String attrName)
    /*-{
        return element.getAttribute(attrName) != null;
    }-*/;
}
