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
package com.xpn.xwiki.wysiwyg.client.plugin.link.ui;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Widget;

/**
 * Keyboard listener adapter to be able to automatically click a button when the enter key is pressed inside a
 * {@link SourcesKeyboardEvents}.
 * 
 * @version $Id$
 */
public class EnterListener extends KeyboardListenerAdapter
{
    /**
     * The button which will be click when enter key will be press.
     */
    private Button button;

    /**
     * Class constructor.
     * 
     * @param button which will be click when enter key will be press.
     */
    public EnterListener(Button button)
    {
        this.button = button;
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListenerAdapter#onKeyPress(Widget, char, int)
     */
    public void onKeyPress(Widget sender, char c, int i)
    {
        super.onKeyPress(sender, c, i);
        // Check if it's the enter key that has been pressed
        if (c == 13) {
            button.click();
        }
    }
}
