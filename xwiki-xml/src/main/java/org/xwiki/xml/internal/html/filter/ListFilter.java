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

import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;
import org.xwiki.xml.html.filter.ElementSelector;

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
@Component("list")
public class ListFilter extends AbstractHTMLFilter
{

    /**
     * {@inheritDoc}
     * 
     * <p>The {@link ListFilter} does not use any cleaningParameters passed in.</p>
     */
    public void filter(Document document, Map<String, String> cleaningParameters)
    {
        // Collect all nested lists.
        List<Element> nestedLists =
            filterDescendants(document.getDocumentElement(), new String[] {TAG_UL, TAG_OL}, new ElementSelector()
            {
                public boolean isSelected(Element element)
                {
                    String parentNodeName = element.getParentNode().getNodeName();
                    return parentNodeName.equalsIgnoreCase(TAG_UL) || parentNodeName.equalsIgnoreCase(TAG_OL);
                }
            });
        for (Element nestedList : nestedLists) {            
            Element parent = (Element) nestedList.getParentNode();
            // Look for the previous sibling that is an Element. If there are none it means we're in the following
            // situation "<ul><ul>" and we need to insert a <li> element to generate "<ul><li><ul>".
            // If there's one check that it's a <li>. It means that we're in the following situation "<ul><li/><ul>"
            // and we need to move the <ul> inside the <li>.
            Node previousElement = nestedList.getPreviousSibling();
            while (null != previousElement && !(previousElement instanceof Element)) {
                previousElement = previousElement.getPreviousSibling();
            }
            if (null == previousElement) {
                // This means we have <ul><ul>. We need to insert a <li> element.
                Element li = document.createElement(TAG_LI);
                parent.insertBefore(li, nestedList);
                li.appendChild(parent.removeChild(nestedList));                
            } else {
                // Move it inside the previous sibling
                previousElement.appendChild(parent.removeChild(nestedList));
            }
        }
    }    
}
