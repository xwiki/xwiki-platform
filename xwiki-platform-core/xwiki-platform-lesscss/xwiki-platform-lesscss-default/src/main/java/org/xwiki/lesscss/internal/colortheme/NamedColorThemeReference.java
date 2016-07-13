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
package org.xwiki.lesscss.internal.colortheme;

/**
 * Specialized implementation of {@link org.xwiki.lesscss.internal.colortheme.ColorThemeReference} for color themes that
 * are not stored in the wiki (currently, it concerns the "default" color theme only).
 *
 * @since 6.4M2
 * @version $Id$
 */
public class NamedColorThemeReference implements ColorThemeReference
{
    private String colorThemeName;

    /**
     * Construct a new reference to a color theme that is not stored in the wiki.
     * @param colorThemeName name of the color theme
     */
    public NamedColorThemeReference(String colorThemeName)
    {
        this.colorThemeName = colorThemeName;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof NamedColorThemeReference) {
            NamedColorThemeReference namedColorThemeReference = (NamedColorThemeReference) o;
            return colorThemeName.equals(namedColorThemeReference.colorThemeName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return colorThemeName.hashCode();
    }

    @Override
    public String serialize()
    {
        return String.format("ColorThemeFS[%s]", colorThemeName);
    }

    @Override
    public String toString()
    {
        return serialize();
    }
}
