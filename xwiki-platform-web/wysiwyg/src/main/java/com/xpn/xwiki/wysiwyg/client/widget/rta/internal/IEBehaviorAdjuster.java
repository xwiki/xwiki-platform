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
package com.xpn.xwiki.wysiwyg.client.widget.rta.internal;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.internal.ie.NativeSelection;

import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * Adjusts the behavior of the rich text area in Internet Explorer browsers.
 * 
 * @version $Id$
 */
public class IEBehaviorAdjuster extends BehaviorAdjuster
{
    /**
     * {@inheritDoc}
     * 
     * @see BehaviorAdjuster#onLoad(Widget)
     */
    public void onLoad(Widget sender)
    {
        super.onLoad(sender);
        NativeSelection.ensureSelectionIsPreserved(getTextArea().getDocument());
    }

    /**
     * {@inheritDoc}
     * 
     * @see BehaviorAdjuster#adjustDragDrop(Document)
     */
    public native void adjustDragDrop(Document document)
    /*-{
        // block default drag and drop mechanism to not allow content to be dropped on this document 
        document.body.attachEvent("ondrop", function(event) {
            event.returnValue = false;
        });
        // block dragging from this object too, because default behaviour is to cut & paste and 
        // we loose content from the editor 
        document.body.attachEvent("ondrag", function(event) {
            event.returnValue = false;
        });
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see BehaviorAdjuster#onKeyDown()
     */
    protected void onKeyDown()
    {
        Event event = getTextArea().getCurrentEvent();
        if (event == null || event.isCancelled()) {
            return;
        }

        switch (event.getKeyCode()) {
            case KeyboardListener.KEY_TAB:
                // IE moves the focus when Tab key is down and thus the key press event doesn't get fired. If we block
                // the key down event then IE doesn't fire key press. We are forced to apply out custom behavior for tab
                // key now, on key down, and not later, on key press.
                onTab();
                break;
            default:
                super.onKeyDown();
                break;
        }
    }
}
