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
import org.xwiki.model.reference.EntityReferenceString;
import org.xwiki.model.reference.PageReference;
import org.xwiki.properties.annotation.PropertyAdvanced;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyDisplayHidden;
import org.xwiki.properties.annotation.PropertyDisplayType;
import org.xwiki.properties.annotation.PropertyFeature;
import org.xwiki.properties.annotation.PropertyGroup;
import org.xwiki.properties.annotation.PropertyName;

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
     * @see #isExcludeFirstHeading()
     */
    private boolean excludeFirstHeading;

    /**
     * @param reference the reference to display
     * @since 3.4M1
     */
    @PropertyDescription("the reference of the resource to display")
    @PropertyGroup("stringReference")
    @PropertyFeature("reference")
    @PropertyDisplayType(EntityReferenceString.class)
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
    @PropertyGroup("stringReference")
    @PropertyAdvanced
    // Marking it as Display Hidden because it's complex and we don't want to confuse our users.
    @PropertyDisplayHidden
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
    @PropertyDescription("an optional id of a section to display in the specified document, e.g. 'HMyHeading'")
    @PropertyAdvanced
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
     * @param excludeFirstHeading {@code true} to remove the first heading found inside
     *        the document or the section, {@code false} to keep it
     * @since 12.4RC1
     */
    @PropertyName("Exclude First Heading")
    @PropertyDescription("Exclude the first heading from the displayed document or section.")
    @PropertyAdvanced
    public void setExcludeFirstHeading(boolean excludeFirstHeading)
    {
        this.excludeFirstHeading = excludeFirstHeading;
    }

    /**
     * @return whether to exclude the first heading from the displayed document or section, or not.
     * @since 12.4RC1
     */
    public boolean isExcludeFirstHeading()
    {
        return this.excludeFirstHeading;
    }

    /**
     * @param page the reference of the page to display
     * @since 10.6RC1
     */
    @PropertyDescription("The reference of the page to display")
    @PropertyFeature("reference")
    @PropertyDisplayType(PageReference.class)
    // Display hidden because we don't want to confuse our users by proposing two ways to enter the reference to
    // display and ATM we don't have a picker for PageReference types and we do have a picker for EntityReference string
    // one so we choose to keep the other one visible and hide this one. We're keeping the property so that we don't
    // break backward compatibility when using the macro in wiki edit mode.
    @PropertyDisplayHidden
    public void setPage(String page)
    {
        this.reference = page;
        this.type = EntityType.PAGE;
    }
}
