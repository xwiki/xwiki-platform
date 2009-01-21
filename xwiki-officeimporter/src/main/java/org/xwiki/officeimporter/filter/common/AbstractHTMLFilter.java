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
package org.xwiki.officeimporter.filter.common;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xwiki.officeimporter.filter.HTMLFilter;

/**
 * Abstract implementation of {@link HTMLFilter} providing utility methods for various common dom operations.
 * 
 * @version $Id$
 * @since 1.8M2
 */
public abstract class AbstractHTMLFilter implements HTMLFilter
{
    /**
     * xhtml 'rowspan' table attribute.
     */
    public static final String ATT_ROWSPAN = "rowspan";

    /**
     * xhtml paragraph tag.
     */
    public static final String TAG_P = "p";

    /**
     * xhtml line break tag.
     */
    public static final String TAG_BR = "br";

    /**
     * xhtml table row tag.
     */
    public static final String TAG_TR = "tr";

    /**
     * xhtml table data tag.
     */
    public static final String TAG_TD = "td";

    /**
     * Utility method for filtering an element's children with a tagName.
     * 
     * @param parent the parent {@link Element}.
     * @param tagName tagName of the children elements.
     * @return list of elements with the provided tagName.
     */
    protected List<Element> filterChildren(Element parent, String tagName)
    {
        List<Element> result = new ArrayList<Element>();
        NodeList nodes = parent.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            result.add(element);
        }
        return result;
    }

    /**
     * Utility method for filtering an element's children with a tagName and an {@link ElementFilterCriterion}.
     * 
     * @param parent the parent {@link Element}.
     * @param tagName tagName of the children elements.
     * @param filter the {@link ElementFilterCriterion} used to select elements.
     * @return list of elements with the provided tagName and matching the given criterion.
     */
    protected List<Element> filterChildren(Element parent, String tagName, ElementFilterCriterion filter)
    {
        List<Element> result = new ArrayList<Element>();
        NodeList nodes = parent.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            if (filter.isFiltered(element)) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Removes the given list of elements.
     * 
     * @param elements the list of elements.
     */
    protected void stripElements(List<Element> elements)
    {
        for (Element e : elements) {
            e.getParentNode().removeChild(e);
        }
    }

    /**
     * Utility method for checking if a list of elements has the same attribute set. If the checkValue is true, the
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
     * Strips off the specified attribute from all the elements present in the given list. 
     * 
     * @param elements the list of elements.
     * @param attributeName name of the attribute.
     */
    protected void stripAttribute(List<Element> elements, String attributeName)
    {
        for (Element e : elements) {
            e.removeAttribute(attributeName);
        }
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
     * Replaces all the elements in the given list with their children.
     * 
     * @param elements the list of elements to be replaced.
     */
    protected void replaceWithChildren(List<Element> elements)
    {
        for (Element e : elements) {
            replaceWithChildren(e);
        }
    }
}
