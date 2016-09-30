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
package org.xwiki.gwt.wysiwyg.client.plugin.importer;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.user.client.StringUtils;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;

/**
 * Removes non-HTML elements, i.e. element from a difference name-space.
 * <p>
 * When pasting rich text from an office document opened with Microsoft's Word application in Internet Explorer the
 * created DOM contains non-HTML elements. These elements have a different name-space that is serialized using a
 * non-standard syntax:
 * 
 * <pre>
 * &lt;?xml:namespace prefix = o ns = "urn:schemas-microsoft-com:office:office" /&gt;&lt;o:p&gt;&lt;/o:p&gt;
 * </pre>
 * 
 * which is not understood by the server side parser.
 * <p>
 * See XWIKI-4161: XML tags are displayed after copy&amp;paste word-doc in office importer.
 * 
 * @version $Id$
 */
public class IEPasteFilter extends PasteFilter
{
    @Override
    public void filter(Element element)
    {
        // We get the list of elements using a native DOM API instead of traversing the document because the DOM
        // document can be in an invalid state.
        NodeList<Element> descendants = element.getElementsByTagName("*");
        List<Element> nonHTMLElements = new ArrayList<Element>();
        for (int i = 0; i < descendants.getLength(); i++) {
            Element descendant = descendants.getItem(i);
            if (!StringUtils.isEmpty(descendant.getPropertyString("tagUrn"))) {
                nonHTMLElements.add(descendant);
            }
        }

        for (Element nonHTMLElement : nonHTMLElements) {
            try {
                nonHTMLElement.getParentNode().removeChild(nonHTMLElement);
            } catch (Exception e) {
                // Skip this element. The DOM is in a bad state.
            }
        }

        super.filter(element);
    }
}
