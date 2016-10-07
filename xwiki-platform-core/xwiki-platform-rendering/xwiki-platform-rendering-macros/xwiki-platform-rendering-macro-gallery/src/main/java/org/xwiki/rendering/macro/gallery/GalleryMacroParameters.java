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
package org.xwiki.rendering.macro.gallery;

import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyId;

/**
 * Parameters for the {@link org.xwiki.rendering.internal.macro.gallery.GalleryMacro}.
 * 
 * @version $Id$
 * @since 8.3RC1
 */
public class GalleryMacroParameters
{
    private String width;

    private String height;

    private String classNames;

    /**
     * @return the gallery width
     */
    public String getWidth()
    {
        return width;
    }

    /**
     * Sets the gallery width.
     * 
     * @param width the gallery width
     */
    @PropertyDescription("The gallery width")
    public void setWidth(String width)
    {
        this.width = width;
    }

    /**
     * @return the gallery height
     */
    public String getHeight()
    {
        return height;
    }

    /**
     * Sets the gallery height.
     * 
     * @param height the gallery height
     */
    @PropertyDescription("The gallery height")
    public void setHeight(String height)
    {
        this.height = height;
    }

    /**
     * @return the CSS class names
     */
    public String getClassNames()
    {
        return classNames;
    }

    /**
     * Sets some custom CSS class names.
     * 
     * @param classNames the custom CSS class names
     */
    @PropertyId("class")
    @PropertyDescription("Custom CSS class names to set on the gallery container. "
        + "Use this parameter if you want to customize the appearance of a gallery instance.")
    public void setClassNames(String classNames)
    {
        this.classNames = classNames;
    }
}
