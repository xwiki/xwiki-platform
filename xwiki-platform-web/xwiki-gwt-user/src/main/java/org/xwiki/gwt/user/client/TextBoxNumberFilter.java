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
package org.xwiki.gwt.user.client;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.TextBox;

/**
 * @version $Id$
 */
public class TextBoxNumberFilter implements KeyPressHandler
{
    /**
     * List of forbidden keys in the TextBox.
     */
    private static final char[] AUTHORIZED_KEYS = {
        KeyCodes.KEY_BACKSPACE, KeyCodes.KEY_DELETE, KeyCodes.KEY_HOME, KeyCodes.KEY_END, KeyCodes.KEY_LEFT,
        KeyCodes.KEY_UP, KeyCodes.KEY_RIGHT, KeyCodes.KEY_DOWN, KeyCodes.KEY_TAB, KeyCodes.KEY_ENTER,
        KeyCodes.KEY_ESCAPE
    };

    /**
     * Determine if a char must be filtered.
     * 
     * @param keyCode char to inspect.
     * @return true if the char needs to be filtered.
     */
    private boolean isFilteredKey(int keyCode)
    {
        if (Character.isDigit((char) keyCode)) {
            return false;
        }
        for (char c : AUTHORIZED_KEYS) {
            if (keyCode == c) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyPressHandler#onKeyPress(KeyPressEvent)
     */
    public void onKeyPress(KeyPressEvent event)
    {
        if (isFilteredKey(event.getNativeEvent().getKeyCode())) {
            // Suppress the current keyboard event.
            ((TextBox) event.getSource()).cancelKey();
        }
    }
}
