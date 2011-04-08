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
package org.xwiki.gwt.wysiwyg.client.plugin.list.internal;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.wysiwyg.client.plugin.list.ListBehaviorAdjuster;

import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.user.client.ui.Widget;

/**
 * Mozilla implementation of the {@link ListBehaviorAdjuster}.
 * 
 * @version $Id$
 */
public class MozillaListBehaviorAdjuster extends ListBehaviorAdjuster
{
    /**
     * The code of the pressed key, at keydown time.
     */
    private int keyDownCode = -1;

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
                currentListItem.appendChild(element.getOwnerDocument().createBRElement());
            }
        }
    }

    /**
     * {@inheritDoc}. The special keys need to be handled at key down time, not at keypress, since keypress should
     * return ascii key, not keyCode. Thus, the special key codes collide with character keys, for example . key with
     * delete key. Therefore, on key down we will store the key that is pressed and on keyPress we handle it. On keyUp,
     * we reset the pressed keycode to the unset value.
     * 
     * @see ListBehaviorAdjuster#onKeyDown(KeyDownEvent)
     */
    public void onKeyDown(KeyDownEvent event)
    {
        keyDownCode = event.getNativeKeyCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ListBehaviorAdjuster#onKeyPress(KeyPressEvent)
     * @see MozillaListBehaviorAdjuster#onKeyDown(KeyDownEvent)
     */
    public void onKeyPress(KeyPressEvent event)
    {
        // just to be sure, although it should have been set on key down
        if (keyDownCode > 0) {
            dispatchKey((Widget) event.getSource(), keyDownCode, (Event) event.getNativeEvent());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ListBehaviorAdjuster#onKeyUp(KeyUpEvent)
     * @see MozillaListBehaviorAdjuster#onKeyDown(KeyDownEvent)
     */
    public void onKeyUp(KeyUpEvent event)
    {
        keyDownCode = -1;
        super.onKeyUp(event);
    }
}
