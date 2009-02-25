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
package org.xwiki.officeimporter.filter;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;

/**
 * Remove some tags from HTML, such as style, script. The tag and all the contents under the tag will be removed.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class StripperFilter extends AbstractHTMLFilter
{
    /**
     * Tags that will be stripped off completely.
     */
    private String[] filterTags = new String[] {"style", "script"};

    /**
     * {@inheritDoc}
     */
    public void filter(Document document, Map<String, String> cleaningParams)
    {
        Element root = document.getDocumentElement();
        for (String tag : filterTags) {
            NodeList toBeRemovedTags = root.getElementsByTagName(tag);
            while (toBeRemovedTags.getLength() > 0) {
                Node t = toBeRemovedTags.item(0);
                t.getParentNode().removeChild(t);
            }
        }
    }
}
