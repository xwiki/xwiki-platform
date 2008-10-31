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

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.KeyboardListener;

/**
 * Factory for {@link ShortcutKey}.
 * 
 * @version $Id$
 */
public final class ShortcutKeyFactory
{
    /**
     * This is a factory class so we protect it's constructor.
     */
    private ShortcutKeyFactory()
    {
    }

    /**
     * Creates a new shortcut key based on the given keyboard event.
     * 
     * @param event The keyboard event for which we create a shortcut key.
     * @return The newly created shortcut key.
     */
    public static ShortcutKey createShortcutKey(Event event)
    {
        int keyCode = event.getKeyCode();
        int modifiers = 0;
        modifiers |= event.getAltKey() ? KeyboardListener.MODIFIER_ALT : 0;
        modifiers |= event.getCtrlKey() ? KeyboardListener.MODIFIER_CTRL : 0;
        modifiers |= event.getMetaKey() ? KeyboardListener.MODIFIER_META : 0;
        modifiers |= event.getShiftKey() ? KeyboardListener.MODIFIER_SHIFT : 0;
        return new ShortcutKey(keyCode, modifiers);
    }

    /**
     * Creates a CTRL-based shortcut key with the given key code.
     * 
     * @param keyCode The key that should be pressed along with the CTRL key.
     * @return the newly creates CTRL-based shortcut key.
     */
    public static ShortcutKey createCtrlShortcutKey(int keyCode)
    {
        return new ShortcutKey(keyCode, KeyboardListener.MODIFIER_CTRL);
    }
}
