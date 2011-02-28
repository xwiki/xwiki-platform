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
package org.xwiki.xml.html.filter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.xml.html.HTMLConstants;

/**
 * Abstract implementation of {@link HTMLFilter} providing utility methods for various common w3c dom operations.
 * 
 * @version $Id$
 * @since 1.8M2
 */
public abstract class AbstractHTMLFilter implements HTMLFilter, HTMLConstants
{
    /**
     * Utility method for filtering an element's children with a tagName.
     * 
     * @param parent the parent {@link Element}.
     * @param tagName expected tagName of the children elements.
     * @return list of children elements with the provided tagName.
     */
    protected List<Element> filterChildren(Element parent, String tagName)
    {
        List<Element> result = new ArrayList<Element>();
        Node current = parent.getFirstChild();
        while (current != null) {
            if (current.getNodeName().equals(tagName)) {
                result.add((Element) current);
            }
            current = current.getNextSibling();
        }
        return result;
    }

    /**
     * Utility method for filtering an element's descendants by their tag names.
     * 
     * @param parent the parent {@link Element}.
     * @param tagNames an array of tagNames.
     * @return list of descendants of the parent element having one of given tag names.
     */
    protected List<Element> filterDescendants(Element parent, String[] tagNames)
    {
        List<Element> result = new ArrayList<Element>();
        for (String tagName : tagNames) {
            NodeList nodes = parent.getElementsByTagName(tagName);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Utility method for filtering an element's descendants by their tag names and an {@link ElementSelector}.
     * 
     * @param parent the parent {@link Element}.
     * @param tagNames an array of tagNames.
     * @param elementSelector an {@link ElementSelector} that allows further filtering of elements.
     * @return list of descendants of the parent element having one of given tag names.
     */
    protected List<Element> filterDescendants(Element parent, String[] tagNames, ElementSelector elementSelector)
    {
        List<Element> result = new ArrayList<Element>();
        for (String tagName : tagNames) {
            NodeList nodes = parent.getElementsByTagName(tagName);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                if (elementSelector.isSelected(element)) {
                    result.add(element);
                }
            }
        }
        return result;
    }

    /**
     * Utility method for checking if a list of elements have the same attribute set. If the checkValue is true, the
     * values of the given attribute will be checked for equivalency.
     * 
     * @param elements the list of elements.
     * @param attributeName Name of the attribute.
     * @param checkValue flag indicating if the value of the attribute should be equal among all the elements.
     * @return true if the given attribute is present and the value check is passing.
     */
    protected boolean hasAttribute(List<Element> elements, String attributeName, boolean checkValue)
    {
        boolean hasAttribute = true;
        if (!checkValue) {
            for (Element e : elements) {
                hasAttribute = e.hasAttribute(attributeName) ? hasAttribute : false;
            }
        } else {
            String attributeValue = null;
            for (Element e : elements) {
                attributeValue = attributeValue == null ? e.getAttribute(attributeName) : attributeValue;
                hasAttribute = e.getAttribute(attributeName).equals(attributeValue) ? hasAttribute : false;
            }
        }
        return hasAttribute;
    }

    /**
     * Replaces the given {@link Element} with it's children.
     * 
     * @param element the {@link Element} to be replaced.
     */
    protected void replaceWithChildren(Element element)
    {
        Element parent = (Element) element.getParentNode();
        while (element.getFirstChild() != null) {
            parent.insertBefore(element.removeChild(element.getFirstChild()), element);
        }
        parent.removeChild(element);
    }

    /**
     * Moves all child elements of the parent into destination element.
     * 
     * @param parent the parent {@link Element}.
     * @param destination the destination {@link Element}.
     */
    protected void moveChildren(Element parent, Element destination)
    {
        NodeList children = parent.getChildNodes();
        while (children.getLength() > 0) {
            destination.appendChild(parent.removeChild(parent.getFirstChild()));
        }
    }
}
