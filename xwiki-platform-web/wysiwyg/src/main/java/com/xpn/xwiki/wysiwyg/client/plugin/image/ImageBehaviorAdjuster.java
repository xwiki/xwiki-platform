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
package com.xpn.xwiki.wysiwyg.client.plugin.image;

import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Customizes the behavior of the editor according to the image constraints, e.g. disables all keys besides backspace
 * and delete when an image is selected.
 * 
 * @version $Id$
 */
public class ImageBehaviorAdjuster implements KeyboardListener
{
    /**
     * The rich text area for which this behavior adjuster operates.
     */
    private RichTextArea textArea;

    /**
     * Builds a behavior adjuster that operates for the passed rich text area.
     * 
     * @param textArea the target rich text area for this behavior adjuster.
     */
    public ImageBehaviorAdjuster(RichTextArea textArea)
    {
        super();
        this.setTextArea(textArea);
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyDown(Widget, char, int)
     */
    public void onKeyDown(Widget sender, char keyCode, int modifiers)
    {
        if (sender != textArea) {
            return;
        }

        // Disable all other keys besides delete and backspace when an image is selected
        if (textArea.getCommandManager().isExecuted(Command.INSERT_IMAGE) && keyCode != KEY_BACKSPACE
            && keyCode != KEY_DELETE) {
            textArea.getCurrentEvent().preventDefault();
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
     * @param textArea the textArea to set
     */
    public void setTextArea(RichTextArea textArea)
    {
        this.textArea = textArea;
        if (this.textArea != null) {
            this.textArea.addKeyboardListener(this);
        }
    }
}
