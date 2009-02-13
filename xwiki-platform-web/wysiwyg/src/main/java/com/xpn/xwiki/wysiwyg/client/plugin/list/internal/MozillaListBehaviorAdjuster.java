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
package com.xpn.xwiki.wysiwyg.client.plugin.list.internal;

import com.google.gwt.dom.client.NodeList;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.plugin.list.ListBehaviorAdjuster;

/**
 * Mozilla implementation of the {@link ListBehaviorAdjuster}.
 * 
 * @version $Id$
 */
public class MozillaListBehaviorAdjuster extends ListBehaviorAdjuster
{
    /**
     * {@inheritDoc} In addition to default cleanup, also add a br in each empty list item (&lt;li /&gt;), so that it
     * stays editable.
     * 
     * @see ListBehaviorAdjuster#cleanUp(Element)
     */
    protected void cleanUp(Element element)
    {
        // default cleanup behavior
        super.cleanUp(element);
        // now for each list item, if it's empty, add a line break inside
        NodeList<com.google.gwt.dom.client.Element> listItems = element.getElementsByTagName(LIST_ITEM_TAG);
        for (int i = 0; i < listItems.getLength(); i++) {
            Element currentListItem = (Element) listItems.getItem(i);
            if (currentListItem.getChildNodes().getLength() == 0) {
                currentListItem.appendChild(((Document) element.getOwnerDocument()).xCreateBRElement());
            }
        }
    }
}
