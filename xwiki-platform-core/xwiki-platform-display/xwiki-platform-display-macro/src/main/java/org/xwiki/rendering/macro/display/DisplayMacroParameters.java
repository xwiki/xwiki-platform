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
package org.xwiki.rendering.macro.display;

import org.xwiki.model.EntityType;
import org.xwiki.properties.annotation.PropertyDescription;

/**
 * Parameters for the {@link org.xwiki.rendering.internal.macro.display.DisplayMacro} Macro.
 * 
 * @version $Id$
 * @since 3.4M1
 */
public class DisplayMacroParameters
{
    /**
     * @see #getReference()
     */
    private String reference;

    /**
     * @see #getType()
     */
    private EntityType type = EntityType.DOCUMENT;

    /**
     * @see #getSection()
     */
    private String section;

    /**
     * @param reference the reference to display
     * @since 3.4M1
     */
    @PropertyDescription("the reference of the resource to display")
    public void setReference(String reference)
    {
        this.reference = reference;
    }

    /**
     * @return the reference of the resource to display
     * @since 3.4M1
     */
    public String getReference()
    {
        return this.reference;
    }

    /**
     * @return the type of the reference
     * @since 3.4M1
     */
    @PropertyDescription("the type of the reference")
    public EntityType getType()
    {
        return this.type;
    }

    /**
     * @param type the type of the reference
     * @since 3.4M1
     */
    public void setType(EntityType type)
    {
        this.type = type;
    }

    /**
     * @param sectionId see {@link #getSection()}
     */
    @PropertyDescription("an optional id of a section to include in the specified document")
    public void setSection(String sectionId)
    {
        this.section = sectionId;
    }

    /**
     * @return the optional id of a section to include in the referenced document. If not specified the whole document
     *         is included.
     */
    public String getSection()
    {
        return this.section;
    }

    /**
     * @param page the reference of the page to display
     * @since 10.6RC1
     */
    @PropertyDescription("The reference of the page to display")
    public void setPage(String page)
    {
        this.reference = page;
        this.type = EntityType.PAGE;
    }
}
