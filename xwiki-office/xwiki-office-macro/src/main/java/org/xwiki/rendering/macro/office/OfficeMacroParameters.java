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
package org.xwiki.rendering.macro.office;

import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyMandatory;

/**
 * Parameters for the {@link org.xwiki.rendering.internal.macro.office.OfficeMacro}.
 * 
 * @version $Id$
 * @since 2.5M2
 */
public class OfficeMacroParameters
{
    /**
     * The office attachment to be viewed. Use an attachment string reference to specify which office file should be
     * viewed: {@code file.ppt}, {@code Page@file.doc}, {@code Space.Page@file.xls} or {@code wiki:Space.Page@file.odt}.
     */
    private String attachment;

    /**
     * Whether to filter in-line CSS styles present in the HTML content produced by the OpenOffice server. Office
     * content is usually better integrated in the host wiki page when styles are filtered.
     * <p>
     * Styles are filtered by default.
     */
    private boolean filterStyles = true;

    /**
     * @return a string reference to the office attachment to be viewed
     */
    public String getAttachment()
    {
        return attachment;
    }

    /**
     * Sets the office attachment to be viewed.
     * 
     * @param attachment an attachment string reference
     */
    @PropertyDescription("The office attachment to be viewed. Use an attachment string reference to specify which "
        + "office file should be viewed: file.ppt, Page@file.doc, Space.Page@file.xls or wiki:Space.Page@file.odt.")
    @PropertyMandatory
    public void setAttachment(String attachment)
    {
        this.attachment = attachment;
    }

    /**
     * @return {@code true} if the CSS styles present in the HTML content produces by the OpenOffice server are
     *         filtered, {@code false} otherwise
     */
    public boolean isFilterStyles()
    {
        return filterStyles;
    }

    /**
     * Sets whether to filter in-line CSS styles present in the HTML content produced by the OpenOffice server.
     * 
     * @param filterStyles {@code true} to filter the CSS style present in the HTML content produces by the OpenOffice
     *            server, {@code false} otherwise
     */
    @PropertyDescription("Whether to filter in-line CSS styles present in the HTML content produced by the OpenOffice "
        + "server. Office content is usually better integrated in the host wiki page when styles are filtered.")
    public void setFilterStyles(boolean filterStyles)
    {
        this.filterStyles = filterStyles;
    }
}
