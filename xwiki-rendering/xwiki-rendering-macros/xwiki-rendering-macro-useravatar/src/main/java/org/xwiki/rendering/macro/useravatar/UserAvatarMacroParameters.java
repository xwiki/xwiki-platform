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
package org.xwiki.rendering.macro.useravatar;

import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyMandatory;

/**
 * Parameters for the {@link org.xwiki.rendering.internal.macro.useravatar.UserAvatarMacro} Macro.
 * 
 * @version $Id$
 * @since 1.8RC2
 */
public class UserAvatarMacroParameters
{
    /**
     * The name of the user whose avatar is to be displayed.
     */
    private String username;

    /**
     * The width of the image (optional).
     */
    private Integer width;

    /**
     * The height of the image.
     */
    private Integer height;

    /**
     * @return the name of the user whose avatar is to be displayed
     */
    public String getUsername()
    {
        return username;
    }

    /**
     * @param username the name of the user whose avatar is to be displayed
     */
    @PropertyMandatory
    @PropertyDescription("the name of the user whose avatar is to be displayed")
    public void setUsername(String username)
    {
        this.username = username;
    }

    /**
     * @return the width of the image.
     */
    public Integer getWidth()
    {
        return width;
    }

    /**
     * @param width the width of the image
     */
    @PropertyDescription("the image's width")
    public void setWidth(Integer width)
    {
        this.width = width;
    }

    /**
     * @return the height of the image
     */
    public Integer getHeight()
    {
        return height;
    }

    /**
     * @param height the height of the image
     */
    @PropertyDescription("the image's height")
    public void setHeight(Integer height)
    {
        this.height = height;
    }
}
