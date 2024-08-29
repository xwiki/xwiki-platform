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
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;

/**
 * Removes the line breaks that were added by the WYSIWYG editor as spacers. Precisely, it removes all {@code <br
 * class="spacer"/>}
 * .
 * 
 * @version $Id$
 */
@Component(roles = {HTMLFilter.class })
@Named("lineBreak")
@Singleton
public class LineBreakFilter extends AbstractHTMLFilter
{
    @Override
    public void filter(Document document, Map<String, String> parameters)
    {
        NodeList brs = document.getElementsByTagName("br");
        List<Element> emptyLineBRs = new ArrayList<>();
        for (int i = 0; i < brs.getLength(); i++) {
            Element br = (Element) brs.item(i);
            if ("spacer".equals(br.getAttribute("class"))) {
                emptyLineBRs.add(br);
            }
        }
        for (int i = 0; i < emptyLineBRs.size(); i++) {
            Element br = emptyLineBRs.get(i);
            br.getParentNode().removeChild(br);
        }
    }

    @Override
    public int getPriority()
    {
        // Make sure this filter is applied before the rest (EmptyLineFilter and StandAloneMacroFilter depend on it).
        return super.getPriority() - 1;
    }
}
