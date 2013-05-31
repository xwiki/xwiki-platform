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

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.StringUtils;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

/**
 * A {@link PasteFilter} specific for the browsers based on the WebKit engine.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public class WebKitPasteFilter extends PasteFilter
{
    @Override
    public void filter(Element element)
    {
        // WebKit adds sometimes a BR after the paste content. Also, since WebKit copies HTML elements with styles (in
        // an attempt to preserve the style of the copied text from its source) the fact that the BR element doesn't
        // have the style attribute is a good indicator that it's not part of the paste content.
        Node lastLeaf = DOMUtils.getInstance().getLastLeaf(element);
        if (Element.is(lastLeaf) && "br".equalsIgnoreCase(lastLeaf.getNodeName())
            && StringUtils.isEmpty(Element.as(lastLeaf).getAttribute(Style.STYLE_ATTRIBUTE))) {
            lastLeaf.removeFromParent();
        }

        super.filter(element);
    }
}
