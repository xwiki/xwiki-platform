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
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyHidden;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Parameters for the Box macro.
 *
 * @version $Id$
 */
public class BoxMacroParameters
{
    /**
     * @see @getClassProperty
     */
    private String cssClass = StringUtils.EMPTY;

    /**
     * @see #getTitle()
     */
    private String title = StringUtils.EMPTY;

    /**
     * @see #getImage()
     */
    private ResourceReference imageReference;

    /**
     * @see #getWidth()
     */
    private String width = StringUtils.EMPTY;

    /**
     * @see #getTitle()
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
    @PropertyHidden
    public void setBlockTitle(List< ? extends Block> blockTitle)
    {
        this.blockTitle = blockTitle;
    }

    /**
     * @return the title to be displayed in the message box. Note that it can contain content in the current syntax
     *         and that text which will be parsed and rendered as any syntax content
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * @param title refer to {@link #getTitle()}
     */
    @PropertyDescription("the title which is to be displayed in the message box")
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * @return the reference to the image to display in the message box.
     */
    public ResourceReference getImage()
    {
        return this.imageReference;
    }

    /**
     * @param imageReference see {@link #getImage()}
     */
    @PropertyDescription("the reference to the image to display in the message box")
    public void setImage(ResourceReference imageReference)
    {
        this.imageReference = imageReference;
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
     * @param cssClass refer to {@link BoxMacroParameters#getCssClass()}
     */
    @PropertyDescription("A CSS class to add to the box element")
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
     * @param width refer to {@link BoxMacroParameters#getWidth()}
     */
    @PropertyDescription("An optional width for the box, expressed in px or %")
    public void setWidth(String width)
    {
        this.width = width;
    }
}
