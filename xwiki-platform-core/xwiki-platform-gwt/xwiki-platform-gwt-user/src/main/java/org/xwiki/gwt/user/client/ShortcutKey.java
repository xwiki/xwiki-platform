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

import java.util.Collections;
import java.util.Set;

/**
 * Represents a combination of a keyboard key and one or more modifier keys, that could trigger a specific action.
 * 
 * @version $Id$
 */
public class ShortcutKey
{
    /**
     * A modifier key.
     */
    public static enum ModifierKey
    {
        /**
         * Common modifier keys.
         */
        SHIFT, CTRL, ALT, META
    }

    /**
     * Specifies the key that must be pressed in order to activate this shortcut.
     * {@link com.google.gwt.event.dom.client.KeyCodes#KEY_UP} is such an example.
     */
    private final int keyCode;

    /**
     * The modifier keys that have to be pressed in order to activate this shortcut.
     */
    private final Set<ModifierKey> modifiers;

    /**
     * We store the hash code of this object so we don't have to compute it many times.
     */
    private final int hashCode;

    /**
     * Creates a new shortcut key.
     * 
     * @param keyCode specifies the key that must be pressed in order to activate the shortcut
     * @param modifiers the modifier keys that have to be pressed in order to activate the shortcut
     */
    public ShortcutKey(int keyCode, Set<ModifierKey> modifiers)
    {
        this.keyCode = keyCode;
        this.modifiers = Collections.unmodifiableSet(modifiers);

        final int prime = 31;
        int thisHashCode = 1;
        thisHashCode = prime * thisHashCode + keyCode;
        thisHashCode = prime * thisHashCode + modifiers.hashCode();
        this.hashCode = thisHashCode;
    }

    /**
     * @return {@link #keyCode}
     */
    public int getKeyCode()
    {
        return keyCode;
    }

    /**
     * @return {@link #modifiers}
     */
    public Set<ModifierKey> getModifiers()
    {
        return modifiers;
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof ShortcutKey)) {
            return false;
        }
        final ShortcutKey other = (ShortcutKey) obj;
        return keyCode == other.keyCode && modifiers.equals(other.modifiers);
    }
}
