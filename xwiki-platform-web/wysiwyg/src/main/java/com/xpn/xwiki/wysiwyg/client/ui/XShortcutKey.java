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
package com.xpn.xwiki.wysiwyg.client.ui;

public class XShortcutKey
{
    private final int keyCode;

    private final int modifiers;

    private final int hashCode;

    public XShortcutKey(int keyCode, int modifiers)
    {
        this.keyCode = keyCode;
        this.modifiers = modifiers;

        final int prime = 31;
        int hashCode = 1;
        hashCode = prime * hashCode + keyCode;
        hashCode = prime * hashCode + modifiers;
        this.hashCode = hashCode;
    }

    public int getKeyCode()
    {
        return keyCode;
    }

    public int getModifiers()
    {
        return modifiers;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        return hashCode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof XShortcutKey)) {
            return false;
        }
        final XShortcutKey other = (XShortcutKey) obj;
        if (keyCode != other.keyCode || modifiers != other.modifiers) {
            return false;
        }
        return true;
    }
}
