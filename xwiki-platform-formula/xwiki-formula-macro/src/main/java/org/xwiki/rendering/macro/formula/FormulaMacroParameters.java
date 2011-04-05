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
package org.xwiki.rendering.macro.formula;

import org.xwiki.formula.FormulaRenderer.FontSize;
import org.xwiki.formula.FormulaRenderer.Type;
import org.xwiki.properties.annotation.PropertyDescription;

/**
 * Parameters for the {@link org.xwiki.rendering.internal.macro.formula.FormulaMacro formula macro}.
 * 
 * @version $Id$
 * @since 2.0M3
 */
public class FormulaMacroParameters
{
    /** The font size to use for the image. */
    private FontSize size = FontSize.DEFAULT;

    /** The type of image to generate. */
    private Type type = Type.DEFAULT;

    /**
     * @param size indicate which font size to use
     */
    @PropertyDescription("adjust font size")
    public void setFontSize(FontSize size)
    {
        this.size = size;
    }

    /**
     * @return the font size to use
     */
    public FontSize getFontSize()
    {
        return this.size;
    }

    /**
     * @param type indicate which type of image to generate
     */
    @PropertyDescription("resulting image type")
    public void setImageType(Type type)
    {
        this.type = type;
    }

    /**
     * @return the image type to generate
     */
    public Type getImageType()
    {
        return this.type;
    }
}
