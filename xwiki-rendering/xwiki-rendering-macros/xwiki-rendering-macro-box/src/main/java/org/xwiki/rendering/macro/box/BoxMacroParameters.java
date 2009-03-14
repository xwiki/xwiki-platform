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
package org.xwiki.rendering.macro.box;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.descriptor.ParameterDescription;

/**
 * Parameters for macro box.
 * 
 * @version $Id$
 */
public class BoxMacroParameters
{
    /**
     * Refer to {@link BoxMacroParameters#getCssClass()}.
     */
    private String cssClass = StringUtils.EMPTY;

    /**
     * Refer to {@link #getTitle()}.
     */
    private String title = StringUtils.EMPTY;

    /**
     * Refer to {@link #getImage()}.
     */
    private String image = StringUtils.EMPTY;

    /**
     * Refer to {@link #getWidth()}.
     */
    private String width = StringUtils.EMPTY;

    /**
     * @return the title to be displayed in the message box. Note that it can be specified using Wiki 2.0 syntax.
     */
    private List< ? extends Block> blockTitle;

    /**
     * Optionally, the title can contain a list of Blocks, for more flexibility, instead of storing only ordinary text.
     * 
     * @return the title represented as a list of Blocks
     */
    public List< ? extends Block> getBlockTitle()
    {
        return blockTitle;
    }

    /**
     * @param blockTitle - refer to {@link #getBlockTitle()}
     */
    public void setBlockTitle(List< ? extends Block> blockTitle)
    {
        this.blockTitle = blockTitle;
    }

    /**
     * @return the title to be displayed in the message box. Note that it can be specified using Wiki 2.0 syntax.
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * @param title - refer to {@link #getTitle()}
     */
    @ParameterDescription("the title which is to be displayed in the message box")
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * @return the image to be displayed in the message box. It can be specified as attachment name or as an absolute
     *         URL.
     */
    public String getImage()
    {
        return image;
    }

    /**
     * @param image - refer to {@link #getImage()}
     */
    @ParameterDescription("the image which is to be displayed in the message box")
    public void setImage(String image)
    {
        this.image = image;
    }

    /**
     * @return an optional CSS sheet to be used when rendering this macro. If no sheet is specified, the
     *         <code>BoxMacro.getClassProperty()</code> is used to provide a default one.
     */
    public String getCssClass()
    {
        return cssClass;
    }

    /**
     * @param cssClass - refer to {@link BoxMacroParameters#getCssClass()}
     */
    @ParameterDescription("A CSS class to add to the box element")
    public void setCssClass(String cssClass)
    {
        this.cssClass = cssClass;
    }

    /**
     * @return an optional width to enforce as an inline style on the DIV element the box will be formed of.
     */
    public String getWidth()
    {
        return this.width;
    }

    /**
     * @param width - refer to {@link BoxMacroParameters#getWidth()}
     */
    @ParameterDescription("An optional width for the box, expressed in px or %")
    public void setWidth(String width)
    {
        this.width = width;
    }
}
