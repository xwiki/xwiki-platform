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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Factory to create the appropriate reference depending on a color theme name.
 *
 * @since 7.0RC1
 * @version $Id$
 */
@Role
@Unstable
public interface ColorThemeReferenceFactory
{
    /**
     * @param colorThemeName name of the color theme
     * @return the appropriate reference depending on the color theme name
     * @throws LESSCompilerException if problem occurs
     */
    ColorThemeReference createReference(String colorThemeName) throws LESSCompilerException;

    /**
     * @param documentReference reference to the document holding the color theme
     * @return the appropriate reference depending on the color theme name
     */
    ColorThemeReference createReference(DocumentReference documentReference);
}
