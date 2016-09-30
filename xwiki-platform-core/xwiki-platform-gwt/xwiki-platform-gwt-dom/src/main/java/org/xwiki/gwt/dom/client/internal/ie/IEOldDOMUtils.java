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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.xwiki.gwt.dom.client.Attribute;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.JavaScriptObject;
import org.xwiki.gwt.dom.client.JavaScriptType;
import org.xwiki.gwt.dom.client.Style;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Node;

/**
 * Contains methods from {@link IEDOMUtils} that require a different implementation in the older versions of the
 * Internet Explorer browser (6, 7 and 8).
 * 
 * @version $Id$
 */
public class IEOldDOMUtils extends IEDOMUtils
{
    /**
     * The class attribute.
     */
    private static final String CLASS_ATTRIBUTE = "class";

    /**
     * The type an attribute value can have.
     */
    private static final EnumSet<JavaScriptType> ATTRIBUTE_TYPES = EnumSet.of(JavaScriptType.BOOLEAN,
        JavaScriptType.NUMBER, JavaScriptType.STRING);

    /**
     * A list of know attributes that are handled in a special way.
     * 
     * @see #isAttribute(Element, String)
     */
    private static final List<String> KNOWN_ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(new String[] {
        Style.STYLE_ATTRIBUTE, CLASS_ATTRIBUTE}));

    @Override
    public native String getComputedStyleProperty(Element element, String propertyName)
    /*-{
      // We force it to be a string because we treat it as a string in the java code.
      return '' + element.currentStyle[propertyName];
    }-*/;

    @Override
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

    @Override
    public void setInnerHTML(Element element, String html)
    {
        element.setInnerHTML("<span>iesucks</span>" + html);
        element.removeChild(element.getFirstChild());
    }

    @Override
    public native JsArrayString getAttributeNames(Element element)
    /*-{
        var attrNames = [];
        for(var i = 0; i < element.attributes.length; i++){
            var attribute = element.attributes[i];
            if (this.@org.xwiki.gwt.dom.client.internal.ie.IEOldDOMUtils::hasAttribute(Lorg/xwiki/gwt/dom/client/Element;Ljava/lang/String;)(element, attribute.nodeName)) {
                attrNames.push(attribute.nodeName);
            }
        }
        return attrNames;
    }-*/;

    @Override
    public boolean hasAttributes(Element element)
    {
        return getAttributeNames(element).length() > 0;
    }

    @Override
    public String getAttribute(Element element, String attributeName)
    {
        // In IE we can't get the style and class attributes using the standard getAttribute method from the DOM API.
        if (Style.STYLE_ATTRIBUTE.equalsIgnoreCase(attributeName)) {
            return element.getStyle().getProperty(Style.STYLE_PROPERTY);
        } else if (CLASS_ATTRIBUTE.equalsIgnoreCase(attributeName)) {
            return element.getClassName();
        } else if (hasAttribute(element, attributeName)) {
            // IE handles attributes and properties in the same way so we check if the given attribute name refers
            // indeed to an attribute (the associated value is of primitive type).
            return element.getAttribute(attributeName);
        } else {
            return "";
        }
    }

    @Override
    public void setAttribute(Element element, String attributeName, String attributeValue)
    {
        // In IE we can't set the style and class attributes using the standard setAttribute method from the DOM API.
        if (Style.STYLE_ATTRIBUTE.equalsIgnoreCase(attributeName)) {
            element.getStyle().setProperty(Style.STYLE_PROPERTY, attributeValue);
        } else if (CLASS_ATTRIBUTE.equalsIgnoreCase(attributeName)) {
            element.setClassName(attributeValue);
        } else {
            element.setAttribute(attributeName, attributeValue);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * NOTE: We override the default implementation to fix an IE7 bug in {@link #removeAttribute(String)}. It seems that
     * {@link #cloneNode(boolean)} doesn't clone the attributes in IE7 but only copies their references to the clone. As
     * a consequence an attribute can be shared by multiple elements. When we {@link #removeAttribute(String)} the
     * {@code specified} flag is set to {@code false} and thus {@link #hasAttribute(String)}, which uses this flag in
     * its IE7 implementation, mistakenly reports the attribute as missing from the rest of the elements that share it.
     * </p>
     * <p>
     * We also think that element properties of non-primitive types shouldn't be counted as attributes.
     * </p>
     * <p>
     * See http://code.google.com/p/google-web-toolkit/issues/detail?id=4690.
     * 
     * @see IEDOMUtils#hasAttribute(Element, String)
     */
    @Override
    public boolean hasAttribute(Element element, String attributeName)
    {
        Attribute attribute = element.getAttributeNode(attributeName);
        // IE handlers attributes and properties in the same way so we have to check if the given attribute name refers
        // indeed to an attribute and not to a JavaScript property of the element object.
        return attribute != null && attribute.isSpecified() && isAttribute(element, attributeName);
    }

    /**
     * On IE attributes and properties are stored in the same array. We exclude all objects and functions, since
     * attributes should be strings, numbers or booleans. Note that this does not ensure the elimination of all custom
     * properties, but covers most cases.
     * <p>
     * In IE8 type of attribute.nodeValue is always string that's why we use element[attributeName] to get the attribute
     * value.
     * 
     * @param element a DOM element
     * @param name the name of a JavaScript property of the given element object
     * @return {@code true} if the specified property is an attribute (i.e. has a primitive type), {@code false}
     *         otherwise
     */
    private boolean isAttribute(Element element, String name)
    {
        return KNOWN_ATTRIBUTES.contains(name)
            || ATTRIBUTE_TYPES.contains(((JavaScriptObject) element.cast()).typeOf(name));
    }

    @Override
    public void removeAttribute(Element element, String attributeName)
    {
        // IE7 has a buggy implementation of cloneNode which can lead to multiple elements sharing the same attribute
        // node. As a result removing the attribute from one of the sharing elements affects the others. Additionally,
        // removing the attribute from two different elements that share it can crash the browser. Instead of removing
        // the attribute we set its value to the empty object which prevents it from being serialized to HTML.
        // Considering that attributes must have values of primitive types we think this is an acceptable workaround.
        if (isExpando(element, attributeName)) {
            ((JavaScriptObject) element.cast()).set(attributeName, JavaScriptObject.createObject());
        } else {
            element.removeAttribute(attributeName);
        }
    }

    /**
     * See http://msdn.microsoft.com/en-us/library/ms533747.aspx.
     * 
     * @param element a DOM element
     * @param attributeName the name of an attribute
     * @return {@code true} if the specified attribute is custom, set through JavaScript, {@code false} otherwise
     */
    private native boolean isExpando(Element element, String attributeName)
    /*-{
        var attribute = element.getAttributeNode(attributeName);
        var buggyExpando = function() {
            var elementClone = element.ownerDocument.createElement(element.nodeName);
            var attributeClone = attribute.cloneNode();
            elementClone.setAttributeNode(attributeClone);
            return attributeClone.expando;
        }
        return !!(attribute && (attribute.expando || buggyExpando()));
    }-*/;

    /**
     * {@inheritDoc}
     * <p>
     * Internet Explorer stores element properties in attribute nodes. In order to remove a property we have to remove
     * its attribute node too.
     * </p>
     * 
     * @see IEDOMUtils#removeProperty(Element, String)
     */
    @Override
    public void removeProperty(Element element, String propertyName)
    {
        super.removeProperty(element, propertyName);
        removeAttribute(element, propertyName);
    }

    @Override
    public native String getInnerText(Element element)
    /*-{
        return element.innerText;
    }-*/;

    @Override
    public void ensureBlockIsEditable(Element block)
    {
        if (!block.hasChildNodes() && block.canHaveChildren()) {
            // Note: appending an empty text node doesn't help.
            block.setInnerHTML("");
        }
    }

    @Override
    public native void setDesignMode(Document document, boolean designMode)
    /*-{
        document.body.contentEditable = designMode ? true : 'inherit';
    }-*/;

    @Override
    public native boolean isDesignMode(Document document)
    /*-{
        return document.body.isContentEditable;
    }-*/;

    @Override
    public boolean canHaveChildren(Node node)
    {
        return Element.is(node) && Element.as(node).getPropertyBoolean("canHaveChildren");
    }
}
