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
package org.xwiki.gwt.wysiwyg.client.plugin.image;

import java.util.Arrays;
import java.util.List;

import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.Widget;

/**
 * Customizes the behavior of the editor according to the image constraints, e.g. disables all keys besides backspace
 * and delete when an image is selected.
 * 
 * @version $Id$
 */
public class ImageBehaviorAdjuster implements KeyDownHandler, KeyUpHandler, KeyPressHandler
{
    /**
     * The list of allowed keys on image selection.
     */
    private static final List<Integer> ALLOWED_KEYS =
        Arrays.asList(KeyCodes.KEY_DELETE, KeyCodes.KEY_BACKSPACE, KeyCodes.KEY_LEFT, KeyCodes.KEY_UP,
            KeyCodes.KEY_RIGHT, KeyCodes.KEY_DOWN, KeyCodes.KEY_HOME, KeyCodes.KEY_END, KeyCodes.KEY_PAGEUP,
            KeyCodes.KEY_PAGEDOWN);

    /**
     * Flag to handle if the current key needs to be blocked or not. Will be set in onKeyDown() for all incoming keys.
     */
    private boolean blocking;

    /**
     * The rich text area for which this behavior adjuster operates.
     */
    private RichTextArea textArea;

    @Override
    public void onKeyDown(KeyDownEvent event)
    {
        // Test the current input and set the blocking flag.
        this.blocking =
            needsBlocking((Widget) event.getSource(), event.getNativeKeyCode(), (Event) event.getNativeEvent());
        // If we're blocking this key, prevent the default behavior for this key
        if (this.blocking) {
            ((Event) event.getNativeEvent()).xPreventDefault();
        }
    }

    /**
     * Tests if the passed key needs to be blocked or not. This test is done depending on the current selection in the
     * text area, the key pressed and its modifiers.
     * 
     * @param sender the sender of this key event
     * @param keyCode the code of the key that was pressed
     * @param event the native event that was fired
     * @return true if the key needs to be blocked by this adjuster, false otherwise.
     */
    private boolean needsBlocking(Widget sender, int keyCode, Event event)
    {
        // Make sure the sender is the listened text area and an image is selected
        if (!(sender == textArea && textArea.getCommandManager().isExecuted(Command.INSERT_IMAGE))) {
            return false;
        }
        // If it's a modified key (ctrl or alt), let it execute
        if (event.getCtrlKey() || event.getAltKey()) {
            return false;
        }
        // If it's in the defined list of allowed keys, let it execute
        if (ALLOWED_KEYS.contains(keyCode)) {
            return false;
        }
        // block everything else
        return true;
    }

    @Override
    public void onKeyPress(KeyPressEvent event)
    {
        // If we're blocking this key, prevent the default behavior for this key
        if (this.blocking) {
            ((Event) event.getNativeEvent()).xPreventDefault();
        }
    }

    @Override
    public void onKeyUp(KeyUpEvent event)
    {
        // If we're blocking this key, prevent the default behavior for this key
        if (this.blocking) {
            ((Event) event.getNativeEvent()).xPreventDefault();
        }
    }

    /**
     * @param textArea the textArea to set
     */
    public void setTextArea(RichTextArea textArea)
    {
        this.textArea = textArea;
    }
}
