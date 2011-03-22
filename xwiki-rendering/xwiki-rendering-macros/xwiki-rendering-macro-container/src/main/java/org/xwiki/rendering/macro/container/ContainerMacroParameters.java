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
package org.xwiki.rendering.macro.container;

import org.xwiki.properties.annotation.PropertyDescription;

/**
 * Parameters of the container macro, specifying the layout of the container, for the moment. To be completed with other
 * properties for the container.
 * 
 * @version $Id$
 * @since 2.5M2
 */
public class ContainerMacroParameters
{
    /**
     * Flag specifying whether the groups inside this macro are displayed as justified or not.
     */
    private boolean justify;

    /**
     * The style of the layout of this container.
     */
    private String layoutStyle;

    /**
     * The css class of this container.
     */
    private String cssClass;

    /**
     * @return the string identifying the layout style for this container.
     */
    public String getLayoutStyle()
    {
        return layoutStyle;
    }

    /**
     * Sets the layout style of this container.
     * 
     * @param layoutStyle the style to set, e.g. {@code columns}
     */
    @PropertyDescription("The identifier of the container layout (e.g. \"columns\"). If no style is provided, the "
        + "container content will be rendered as is.")
    public void setLayoutStyle(String layoutStyle)
    {
        this.layoutStyle = layoutStyle;
    }

    /**
     * @return {@code true} whether the content in this container is justified, {@code false} otherwise
     */
    public boolean isJustify()
    {
        return justify;
    }

    /**
     * Set if the content in this container is justified.
     * 
     * @param justify {@code true} whether the content in this container is aligned "justify", {@code false} otherwise
     */
    @PropertyDescription("Flag specifying whether the content in this container is justified or not.")
    public void setJustify(boolean justify)
    {
        this.justify = justify;
    }

    /**
     * @return the CSS class to add to this container
     * @since 3.0M1
     */
    public String getCssClass()
    {
        return cssClass;
    }

    /**
     * @param cssClass the value of the class attribute to set for CSS styling
     * @since 3.0M1
     */
    @PropertyDescription("Value of the HTML class attribute to add to this container, used to style in CSS.")
    public void setCssClass(String cssClass)
    {
        this.cssClass = cssClass;
    }
}
