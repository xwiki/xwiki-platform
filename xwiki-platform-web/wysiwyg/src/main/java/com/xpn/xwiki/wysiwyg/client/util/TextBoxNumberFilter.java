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
package com.xpn.xwiki.wysiwyg.client.util;

import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * @version $Id$
 */
public class TextBoxNumberFilter extends KeyboardListenerAdapter
{
    /**
     * List of forbidden keys in the TextBox.
     */
    private static final char[] AUTHORIZED_KEYS =
    {KEY_BACKSPACE, KEY_DELETE, KEY_HOME, KEY_END, KEY_LEFT, KEY_UP, KEY_RIGHT, KEY_DOWN};

    /**
     * Determine if a char must be filtered.
     * 
     * @param keyCode char to inspect.
     * @return true if the char needs to be filtered.
     */
    private boolean isFilteredKey(char keyCode)
    {
        if (Character.isDigit(keyCode)) {
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
     * @see KeyboardListenerAdapter#onKeyPress(Widget, char, int)
     */
    public void onKeyPress(Widget sender, char keyCode, int modifiers)
    {
        if (isFilteredKey(keyCode)) {
            // Suppress the current keyboard event.
            ((TextBox) sender).cancelKey();
        }
    }
}
