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

import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.SourcesLoadEvents;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * Adjusts the behavior of the rich text area to meet the cross browser specification.<br/>
 * The built-in WYSIWYG editor provided by all modern browsers may react differently to user input (like typing) on
 * different browsers. This class serves as a base class for browser specific behavior adjustment.
 * 
 * @version $Id$
 */
public class BehaviorAdjuster implements KeyboardListener, LoadListener
{
    /**
     * The rich text area whose behavior is being adjusted.
     */
    private RichTextArea textArea;

    /**
     * @return The rich text area whose behavior is being adjusted.
     */
    protected RichTextArea getTextArea()
    {
        return textArea;
    }

    /**
     * @param rta the textArea to set
     */
    public void setTextArea(RichTextArea rta)
    {
        this.textArea = rta;
        if (textArea != null) {
            // Add required listeners that this behaviour adjuster needs in order to operate
            textArea.addKeyboardListener(this);
            // Workaround till GWT provides a way to detect when the rich text area has finished loading.
            if (textArea.getBasicFormatter() != null && textArea.getBasicFormatter() instanceof SourcesLoadEvents) {
                ((SourcesLoadEvents) textArea.getBasicFormatter()).addLoadListener(this);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyDown(Widget, char, int)
     */
    public void onKeyDown(Widget sender, char keyCode, int modifiers)
    {
        // Make sure the sender is the text area of this BehaviorAdjuster.
        if ((RichTextArea) sender != textArea) {
            return;
        }

        switch (keyCode) {
            case KeyboardListener.KEY_ENTER:
                onEnter();
                break;
            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyPress(Widget, char, int)
     */
    public void onKeyPress(Widget sender, char keyCode, int modifiers)
    {
        // nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyUp(Widget, char, int)
     */
    public void onKeyUp(Widget sender, char keyCode, int modifiers)
    {
        // nothing
    }

    /**
     * Overwrites the default rich text area bahavior when the Enter key is being pressed.
     */
    public void onEnter()
    {
        // Keeps the default behavior.
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadListener#onError(Widget)
     */
    public void onError(Widget sender)
    {
        // Nothing to do upon load error
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadListener#onLoad(Widget)
     */
    public void onLoad(Widget sender)
    {
        adjustDragDrop(textArea.getDocument());
    }

    /**
     * Prevents the drag and drop default behaviour by disabling the default events. 
     * 
     * @param document the document in the loaded rich text area.
     */
    public void adjustDragDrop(Document document)
    {
        // nothing here by default
    }
}
