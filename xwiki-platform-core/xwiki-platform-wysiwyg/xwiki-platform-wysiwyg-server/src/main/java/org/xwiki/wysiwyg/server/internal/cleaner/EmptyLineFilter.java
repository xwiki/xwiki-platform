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
package org.xwiki.wysiwyg.server.internal.cleaner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;

/**
 * Converts empty paragraphs to empty lines. Precisely, converts all {@code <p>
 * </p>} to {@code <div class="wikimodel-emptyline"></div>}.
 * 
 * @version $Id$
 */
@Component(roles = {HTMLFilter.class })
@Named("emptyLine")
@Singleton
public class EmptyLineFilter implements HTMLFilter
{
    @Override
    public void filter(Document document, Map<String, String> parameters)
    {
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
            div.setAttribute("class", "wikimodel-emptyline");
            Element paragraph = emptyParagraphs.get(i);
            paragraph.getParentNode().replaceChild(div, paragraph);
        }
    }
}
