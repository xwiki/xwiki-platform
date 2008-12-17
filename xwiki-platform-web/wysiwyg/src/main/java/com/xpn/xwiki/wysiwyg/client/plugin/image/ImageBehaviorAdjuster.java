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

import java.util.Arrays;
import java.util.List;

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
     * The list of allowed keys on image selection.
     */
    private static final List<Integer> ALLOWED_KEYS =
        Arrays.asList(KEY_DELETE, KEY_BACKSPACE, KEY_LEFT, KEY_UP, KEY_RIGHT, KEY_DOWN, KEY_HOME, KEY_END, KEY_PAGEUP,
            KEY_PAGEDOWN);

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
        this.textArea = textArea;
        this.textArea.addKeyboardListener(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyDown(Widget, char, int)
     */
    public void onKeyDown(Widget sender, char keyCode, int modifiers)
    {
        // nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyPress(Widget, char, int)
     */
    public void onKeyPress(Widget sender, char keyCode, int modifiers)
    {
        // Make sure the sender is the listened text area and an image is selected
        if (!(sender == textArea && textArea.getCommandManager().isExecuted(Command.INSERT_IMAGE))) {
            return;
        }
        // If it's a modified key (ctrl or alt), let it execute
        if (textArea.getCurrentEvent().getCtrlKey() || textArea.getCurrentEvent().getAltKey()) {
            return;
        }
        // If it's in the defined list of allowed keys, let it execute
        if (ALLOWED_KEYS.contains((int) keyCode)) {
            return;
        }
        // block everything else
        textArea.getCurrentEvent().preventDefault();
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
}
