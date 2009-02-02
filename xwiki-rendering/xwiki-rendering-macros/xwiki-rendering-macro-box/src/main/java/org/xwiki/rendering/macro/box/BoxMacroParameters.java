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

import org.apache.commons.lang.StringUtils;
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
    @ParameterDescription("the CSS sheet used for rendering the document")
    public void setCssClass(String cssClass)
    {
        this.cssClass = cssClass;
    }

}
