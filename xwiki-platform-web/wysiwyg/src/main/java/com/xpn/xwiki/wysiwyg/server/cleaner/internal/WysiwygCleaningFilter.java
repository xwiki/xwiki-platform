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
package com.xpn.xwiki.wysiwyg.server.cleaner.internal;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Removes or replaces the HTML elements that were added by the WYSIWYG editor only for internal reasons. The following
 * transformations are done:
 * <ul>
 * <li>Removes <code>&lt;br class="spacer"/&gt;</code></li>
 * <li>Converts <code>&lt;p&gt;&lt;/p&gt;</code> to &lt;div class="wikimodel-emptyline"&gt;&lt;/div&gt;</li>
 * </ul>
 * 
 * @version $Id$
 */
public class WysiwygCleaningFilter
{
    /**
     * The HTML class attribute.
     */
    private static final String CLASS = "class";

    /**
     * Filters the given document for HTML elements specific to the WYSIWYG editor.
     * 
     * @param document the document to be filtered. It should be the document resulted from cleaning the HTML generated
     *            by the WYSIWYG editor.
     */
    public void filter(Document document)
    {
        // Remove the BRs needed by Firefox in edit mode.
        NodeList brs = document.getElementsByTagName("br");
        List<Element> emptyLineBRs = new ArrayList<Element>();
        for (int i = 0; i < brs.getLength(); i++) {
            Element br = (Element) brs.item(i);
            if ("spacer".equals(br.getAttribute(CLASS))) {
                emptyLineBRs.add(br);
            }
        }
        for (int i = 0; i < emptyLineBRs.size(); i++) {
            Element br = emptyLineBRs.get(i);
            br.getParentNode().removeChild(br);
        }

        // Convert empty paragraphs back into empty-line DIVs.
        NodeList paragraphs = document.getElementsByTagName("p");
        List<Element> emptyParagraphs = new ArrayList<Element>();
        for (int i = 0; i < paragraphs.getLength(); i++) {
            Element paragraph = (Element) paragraphs.item(i);
            if (!paragraph.hasChildNodes()) {
                emptyParagraphs.add(paragraph);
            }
        }
        for (int i = 0; i < emptyParagraphs.size(); i++) {
            Element div = document.createElement("div");
            div.setAttribute(CLASS, "wikimodel-emptyline");
            Element paragraph = emptyParagraphs.get(i);
            paragraph.getParentNode().replaceChild(div, paragraph);
        }
    }
}
