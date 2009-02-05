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

import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.plugin.list.ListBehaviorAdjuster;

/**
 * Internet Explorer implementation of the {@link ListBehaviorAdjuster}.
 * 
 * @version $Id$
 */
public class IEListBehaviorAdjuster extends ListBehaviorAdjuster
{
    /**
     * {@inheritDoc}
     * 
     * @see ListBehaviorAdjuster#onKeyDown(Widget, char, int)
     */
    public void onKeyDown(Widget sender, char keyCode, int modifiers)
    {
        if (getTextArea() != sender) {
            return;
        }

        // get current range for some checks
        Range range = getTextArea().getDocument().getSelection().getRangeAt(0);
        Node li = DOMUtils.getInstance().getFirstAncestor(range.getCommonAncestorContainer(), LIST_ITEM_TAG);
        if (li == null) {
            return;
        }

        // Internet explorer seems to receive the delete key presses onKeyDown so we overwrite this function to dispatch
        // this key too
        switch (keyCode) {
            case KeyboardListener.KEY_DELETE:
                onDelete((Element) li);
                break;
            case KeyboardListener.KEY_BACKSPACE:
                onBackspace((Element) li);
                break;                
            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ListBehaviorAdjuster#onKeyPress(Widget, char, int)
     */
    public void onKeyPress(Widget sender, char keyCode, int modifiers)
    {
        // nothing
    }
}
