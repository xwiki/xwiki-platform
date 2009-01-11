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

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.Filter;
import org.xwiki.xml.html.filter.CleaningFilter;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Transform non XHTML list into XHTML valid lists. Specifically, move &lt;ul&gt; or &lt;ol&gt; element nested inside
 * a &lt;ul&gt; or &lt;ol&gt; element inside the previous &lt;li&gt; element.
 *
 * For example:
 * <code><pre>
 *   <ul>
 *     <li>item1</li>
 *     <ul>
 *       <li>item2</li>
 *     </ul>
 *   </ul>
 * </pre></code>
 * becomes
 * <code><pre>
 *   <ul>
 *     <li>item1
 *       <ul>
 *         <li>item2</li>
 *       </ul>
 *     </li>
 *   </ul>
 * </pre></code>
 *
 * @version $Id: $
 * @since 1.6M1
 */
public class ListCleaningFilter implements CleaningFilter
{
    /**
     * HTML unsorted list tag.
     */
    private static final String UL = "ul";

    /**
     * HTML numbered list tag.
     */
    private static final String OL = "ol";

    /**
     * HTML list element tag.
     */
    private static final String LI = "li";

    /**
     * Filters UL and OL elements.
     */
    private class ListElementFilter implements Filter
    {
        /**
         * {@inheritDoc}
         * @see Filter#matches(Object)
         */
        public boolean matches(Object o)
        {
            boolean result = false;
            if (o.getClass().isAssignableFrom(Element.class)) {
                Element element = (Element) o;
                if (element.getName().equalsIgnoreCase(UL) || element.getName().equalsIgnoreCase(OL)) {
                    result = true;
                }
            }
            return result;
        }
    }

    /**
     * {@inheritDoc}
     * @see CleaningFilter#filter(org.jdom.Document)
     */
    public void filter(Document document)
    {
        // Note: we need to gather all elements to modify in a list before we make any modification as otherwise
        // We'd have some problems since it means we would modify the iterator for the case when we create a new
        // li element below thus leading to an exception.

        List<Element> elementsToModify = new ArrayList<Element>();
        Iterator descendants = document.getDescendants(new ListElementFilter());
        while (descendants.hasNext()) {
            Element element = (Element) descendants.next();
            Element parent = element.getParentElement();
            if (parent.getName().equalsIgnoreCase(UL) || parent.getName().equalsIgnoreCase(OL)) {
                elementsToModify.add(element);
            }
        }

        for (Element element : elementsToModify) {
            Element parent = element.getParentElement();
            // Look for the previous sibling that is an Element. If there are none it means we're in the following
            // situation "<ul><ul>" and we need to insert a <li> element to generate "<ul><li><ul>".
            // If there's one check that it's a <li>. It means that we're in the following situation "<ul><li/><ul>"
            // and we need to move the <ul> inside the <li>.
            Element previousElement = null;
            List contentList = parent.getContent();
            int pos = contentList.indexOf(element);
            for (int i = pos - 1; i > -1; i--) {
                if ((previousElement == null)
                    && contentList.get(i).getClass().isAssignableFrom(Element.class))
                {
                    previousElement = (Element) contentList.get(i);
                    break;
                }
            }

            if (previousElement == null) {
                // This means we have <ul><ul>. We need to insert a <li> element.
                Element li = new Element(LI);
                li.addContent(element.detach());
                parent.addContent(pos, li);
            } else {
                // Move it inside the previous sibling
                previousElement.addContent(element.detach());
            }
        }
    }
}
