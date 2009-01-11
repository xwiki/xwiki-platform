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
package org.xwiki.xml.internal.html.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.Filter;
import org.xwiki.xml.html.filter.CleaningFilter;

/**
 * Replaces {@code<br/>} elements placed in between block elements with {@code<div class="wikikmodel-emptyline"/>}.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class LineBreakCleaningFilter implements CleaningFilter
{
    /**
     * List of block element tag names.
     */
    private static final String[] BLOCK_ELEMENT_TAGS =
        new String[] {"p", "ul", "ol", "hr", "h1", "h2", "h3", "h4", "h5", "h6", "table"};

    /**
     * {@link Filter} for filtering out {@code<br/>} elements.
     */
    private class LineBreakFilter implements Filter
    {
        /**
         * {@inheritDoc}
         */
        public boolean matches(Object o)
        {
            boolean result = false;
            if (o.getClass().isAssignableFrom(Element.class)) {
                Element element = (Element) o;
                result = element.getName().equalsIgnoreCase("br") ? true : result;
            }
            return result;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void filter(Document document)
    {
        // First collect the <br/> elements that should be replaced. We cannot replace them on-the-fly because it will
        // throw a ConcurrentModificationException.
        Iterator lineBreaks = document.getDescendants(new LineBreakFilter());
        List<Element> lineBreaksToReplace = new ArrayList<Element>();
        while (lineBreaks.hasNext()) {
            Element lineBreak = (Element) lineBreaks.next();
            Element prev = getPreviousDifferentSibling(lineBreak);
            Element next = getNextDifferentSibling(lineBreak);
            boolean shouldReplace = !(null == prev && null == next) && (isBlockElement(prev) || isBlockElement(next));
            if (shouldReplace) {
                lineBreaksToReplace.add(lineBreak);
            }
        }
        // Perform the substitution.
        for (Element lineBreak : lineBreaksToReplace) {
            Element parent = lineBreak.getParentElement();
            Element div = new Element("div");
            div.setAttribute("class", "wikimodel-emptyline");
            parent.addContent(parent.indexOf(lineBreak), div);
            parent.removeContent(lineBreak);
        }
    }

    /**
     * Check whether the given element represents a block element.
     * 
     * @param element the {@link Element}.
     * @return true if the element represents a block element.
     */
    private boolean isBlockElement(Element element)
    {
        boolean isBlockElement = false;
        if (null != element) {
            for (String blockElementTag : BLOCK_ELEMENT_TAGS) {
                isBlockElement = element.getName().equals(blockElementTag) ? true : isBlockElement;
            }
        }
        return isBlockElement;
    }

    /**
     * Utility method for retrieving the next sibling of an element.
     * 
     * @param element the {@link Element}
     * @return next sibling of the given element or null if there is no such element.
     */
    private Element getNextSibling(Element element)
    {
        Element parent = element.getParentElement();
        List children = parent.getChildren();
        int index = children.indexOf(element);
        if (index >= 0 && index < children.size() - 1) {
            return (Element) children.get(index + 1);
        }
        return null;
    }

    /**
     * Utility method for retrieving the next sibling which is not equal to the given element.
     * 
     * @param element the {@link Element}.
     * @return next different sibling of the given element or null if there is no such element.
     */
    private Element getNextDifferentSibling(Element element)
    {
        Element next = getNextSibling(element);
        while (next != null && element.getName().equals(next.getName())) {
            next = getNextSibling(next);
        }
        return next;
    }

    /**
     * Utility method for retrieving the previous sibling of an element.
     * 
     * @param element the {@link Element}
     * @return previous sibling of the given element or null if there is no such element.
     */
    private Element getPreviousSibling(Element element)
    {
        Element parent = element.getParentElement();
        List children = parent.getChildren();
        int index = children.indexOf(element);
        if (index > 0 && index < children.size()) {
            return (Element) children.get(index - 1);
        }
        return null;
    }

    /**
     * Utility method for retrieving the previous sibling which is not equal to the given element.
     * 
     * @param element the {@link Element}.
     * @return previous different sibling of the given element or null if there is no such element.
     */
    private Element getPreviousDifferentSibling(Element element)
    {
        Element prev = getPreviousSibling(element);
        while (prev != null && element.getName().equals(prev.getName())) {
            prev = getPreviousSibling(prev);
        }
        return prev;
    }
}
