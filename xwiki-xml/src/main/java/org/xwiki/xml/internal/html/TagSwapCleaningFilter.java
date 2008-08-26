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
package org.xwiki.xml.internal.html;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

import java.util.Iterator;

/**
 * Replace tags by others. For example replace tags that have been deprecated in XHTML with their equivalent.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public class TagSwapCleaningFilter implements CleaningFilter
{
    /**
     * {@inheritDoc}
     * Replaces deprecated tags in HTML by their XHTML equivalent (b with strong, i with em, etc).
     *
     * @see org.xwiki.xml.internal.html.CleaningFilter#filter(org.jdom.Document)
     */
    public void filter(Document document)
    {
        swapTag(document, "b", "strong");
        swapTag(document, "i", "em");
    }

    /**
     * Replace one tag by another.
     *
     * @param document the document to clean
     * @param sourceTag the tag name to modify
     * @param newTag the new tag name
     */
    private void swapTag(Document document, String sourceTag, String newTag)
    {
        Iterator descendants = document.getDescendants(new ElementFilter(sourceTag));
        while (descendants.hasNext()) {
            Element element = (Element) descendants.next();
            element.setName(newTag);
        }
    }
}
