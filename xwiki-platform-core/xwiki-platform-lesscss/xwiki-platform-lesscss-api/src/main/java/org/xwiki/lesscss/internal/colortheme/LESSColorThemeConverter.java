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

import org.xwiki.component.annotation.Role;
import org.xwiki.lesscss.compiler.LESSCompilerException;

/**
 * Component to parse a LESS skin file and to return a Color Theme from it.
 *
 * This component must cache its results in an instance of {@link org.xwiki.lesscss.internal.cache.ColorThemeCache}.
 *
 * @since 7.0RC1
 * @version $Id$
 */
@Role
public interface LESSColorThemeConverter
{
    /**
     * Get a color theme from a LESS file, located in the current skin.
     * @param fileName name of the LESS file
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @return the computed Color Theme
     * @throws org.xwiki.lesscss.compiler.LESSCompilerException if problem occurs
     */
    ColorTheme getColorThemeFromSkinFile(String fileName, boolean force) throws LESSCompilerException;

    /**
     * Get a color theme from a LESS file, located in the specified skin.
     * @param fileName name of the LESS file
     * @param skin name of the skin where the LESS file is located
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @return the computed Color Theme
     * @throws LESSCompilerException if problem occurs
     */
    ColorTheme getColorThemeFromSkinFile(String fileName, String skin, boolean force) throws LESSCompilerException;
}
