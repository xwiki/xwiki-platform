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
import org.xwiki.gwt.dom.client.Window;

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
    public native String getComputedStyleProperty(Element el, String propertyName)
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
                Element internalElement = (Element) doc.createElement(externalElement.getTagName());
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
            var attribute = element.attributes[i];
            // On IE attributes and properties are stored in the same array. We exclude all objects and functions, 
            // since attributes should be strings, numbers. Note that this does not ensure the elimination of all 
            // custom properties, but covers most cases.
            if (attribute.specified && typeof attribute.nodeValue != 'object' 
                && typeof attribute.nodeValue != 'function') {
                attrNames.push(attribute.nodeName);
            }
            // Typeof style is object and, in our quest to eliminate custom set properties, this one also gets removed
            if (element.style.cssText != '') {
                attrNames.push('style');
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
    public native String getAttribute(Element element, String name)
    /*-{
        // it seems that IE cannot return the style attribute value with getAttributeNode("style").nodeValue
        // http://www.quirksmode.org/dom/w3c_core.html
        if (name == "style") {
            return element.style.cssText;
        }
        // the class, for example, is not returned on getAttribute("class") but getAttribute("className") or 
        // getAttributeNode("class").nodeValue so make this the same for all attributes 
        var attrNode = element.getAttributeNode(name);
        if (attrNode) {
            // make sure we don't print "undefined" and always return a String
            return (attrNode.nodeValue || '' ) + ''; 
        }
        return '';
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see DOMUtils#setAttribute(Element, String, String)
     */
    public void setAttribute(Element element, String name, String value)
    {
        // In IE we can't set the style attribute using the standard setAttribute method from the DOM API.
        if (Style.STYLE_ATTRIBUTE.equalsIgnoreCase(name)) {
            element.getStyle().setProperty("cssText", value);
        } else if ("class".equalsIgnoreCase(name)) {
            element.setClassName(value);
        } else {
            element.setAttribute(name, value);
        }
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
        var attrNode = element.getAttributeNode(attrName);
        return attrNode && attrNode.specified;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see DOMUtils#hasAttributes(Element)
     */
    public boolean hasAttributes(Element element)
    {
        return getAttributeNames(element).length() > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DOMUtils#ensureBlockIsEditable(Element)
     */
    public void ensureBlockIsEditable(Element block)
    {
        if (!block.hasChildNodes()) {
            // Note: appending an empty text node doesn't help.
            block.setInnerHTML("");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DOMUtils#setDesignMode(Document, boolean)
     */
    public native void setDesignMode(Document document, boolean designMode)
    /*-{
        document.body.contentEditable = designMode ? true : 'inherit';
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see DOMUtils#isDesignMode(Document)
     */
    public native boolean isDesignMode(Document document)
    /*-{
        return document.body.isContentEditable;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see DOMUtils#stop(Window)
     */
    public void stop(Window window)
    {
        window.getDocument().execCommand("Stop", null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Internet Explorer stores element properties in attribute nodes. In order to remove a property we have to remove
     * its attribute node too.
     * 
     * @see DOMUtils#removeProperty(Element, String)
     */
    public void removeProperty(Element element, String propertyName)
    {
        super.removeProperty(element, propertyName);
        element.removeAttribute(propertyName);
    }
}
