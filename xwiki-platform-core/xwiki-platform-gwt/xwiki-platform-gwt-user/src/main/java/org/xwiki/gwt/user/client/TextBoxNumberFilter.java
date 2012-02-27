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

import java.util.Arrays;
import java.util.List;

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
     * Opera handles these keys as character keys, i.e. charCode > 0.
     */
    private static final List<Integer> SPECIAL_KEY_CODES = Arrays.asList(KeyCodes.KEY_BACKSPACE, KeyCodes.KEY_TAB);

    @Override
    public void onKeyPress(KeyPressEvent event)
    {
        int keyCode = event.getNativeEvent().getKeyCode();
        int codePoint = event.getUnicodeCharCode();
        if (codePoint > 0 && !Character.isDigit((char) codePoint) && !SPECIAL_KEY_CODES.contains(keyCode)) {
            // Suppress the current keyboard event.
            ((TextBox) event.getSource()).cancelKey();
        }
    }
}
