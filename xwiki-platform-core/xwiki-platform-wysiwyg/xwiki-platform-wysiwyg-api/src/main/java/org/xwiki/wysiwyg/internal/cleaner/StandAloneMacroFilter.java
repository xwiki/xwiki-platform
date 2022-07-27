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
package org.xwiki.wysiwyg.internal.cleaner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;

/**
 * Looks for stand alone macros that are wrapped in paragraphs and unwraps them. In other words, looks for paragraphs
 * that contain just a macro call and replaces these paragraphs with those macro calls. For instance {@code <p>
 * <!--startmacro:toc|-||-|><!--stopmacro-->
 * </p>} is replaced by {@code <!--startmacro:toc|-||-|><!--stopmacro-->}.
 * 
 * @version $Id$
 */
@Component(roles = {HTMLFilter.class })
@Named("standAloneMacro")
@Singleton
public class StandAloneMacroFilter extends AbstractHTMLFilter
{
    @Override
    public void filter(Document document, Map<String, String> parameters)
    {
        List<Element> wrappers = getStandAloneMacroWrappers(document);
        for (int i = 0; i < wrappers.size(); i++) {
            Element paragraph = wrappers.get(i);
            // Replace the paragraph with the macro comments.
            paragraph.getParentNode().insertBefore(paragraph.getFirstChild(), paragraph);
            paragraph.getParentNode().insertBefore(paragraph.getLastChild(), paragraph);
            paragraph.getParentNode().removeChild(paragraph);
        }
    }

    /**
     * @param document a DOM document
     * @return the list of paragraphs that contain just a macro call
     */
    private List<Element> getStandAloneMacroWrappers(Document document)
    {
        NodeList paragraphs = document.getElementsByTagName("p");
        List<Element> wrappers = new ArrayList<>();
        for (int i = 0; i < paragraphs.getLength(); i++) {
            Element paragraph = (Element) paragraphs.item(i);
            // See if the paragraph contains only a macro call.
            // Look for the start macro comment.
            Node child = paragraph.getFirstChild();
            if (child == null || child.getNodeType() != Node.COMMENT_NODE
                || !child.getNodeValue().startsWith("startmacro:")) {
                continue;
            }
            // Look for the stop macro comment.
            do {
                child = child.getNextSibling();
            } while (child != null
                && !(child.getNodeType() == Node.COMMENT_NODE && child.getNodeValue().equals("stopmacro")));
            // See if there's something else inside the paragraph.
            if (child != null && child.getNextSibling() == null) {
                wrappers.add(paragraph);
            }
        }
        return wrappers;
    }
}
