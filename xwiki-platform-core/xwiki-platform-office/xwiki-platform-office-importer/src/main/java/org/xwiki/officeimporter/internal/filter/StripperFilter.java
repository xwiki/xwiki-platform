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
package org.xwiki.officeimporter.internal.filter;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;

/**
 * Remove some tags from HTML, such as style, script. The tag and all the contents under the tag will be removed.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("officeimporter/stripper")
public class StripperFilter extends AbstractHTMLFilter
{
    /**
     * Tags that will be stripped off completely.
     */
    private String[] filterTags = new String[] {TAG_STYLE, TAG_SCRIPT};

    @Override
    public void filter(Document document, Map<String, String> cleaningParams)
    {
        List<Element> toBeStrippedElements = filterDescendants(document.getDocumentElement(), this.filterTags);
        for (Element element : toBeStrippedElements) {
            element.getParentNode().removeChild(element);
        }
    }
}
