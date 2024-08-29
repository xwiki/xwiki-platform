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
package org.xwiki.rendering.macro.include;

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
 * Parameters for the {@link org.xwiki.rendering.internal.macro.include.IncludeMacro} Macro.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class IncludeMacroParameters
{
    /**
     * @version $Id$
     */
    public enum Context
    {
        /**
         * Macro executed in its own context.
         */
        NEW,

        /**
         * Macro executed in the context of the current page.
         */
        CURRENT
    }

    /**
     * Control which author to execute the included content with.
     * 
     * @since 15.0RC1
     * @since 14.10.2
     */
    public enum Author
    {
        /**
         * Before 15.0RC1 always apply {@link #CURRENT} but starting with 15.0RC1 apply {@link #TARGET} option unless
         * the target author has programming right in which case it applies {@link #CURRENT} for retro-compatibility
         * reasons).
         */
        AUTO,

        /**
         * The content is executed with the right of the current author which uses the include macro.
         */
        CURRENT,

        /**
         * The content is executed with the right of the included content author.
         */
        TARGET
    }

    /**
     * @see #getReference()
     */
    private String reference;

    /**
     * @see #getType()
     */
    private EntityType type = EntityType.DOCUMENT;

    /**
     * Defines whether the included page is executed in its separated execution context or whether it's executed in the
     * context of the current page.
     */
    private Context context = Context.CURRENT;

    /**
     * @see #getSection()
     */
    private String section;

    /**
     * @see #isExcludeFirstHeading()
     */
    private boolean excludeFirstHeading;

    private Author author = Author.AUTO;

    /**
     * @param reference the reference of the resource to include
     * @since 3.4M1
     */
    @PropertyDescription("the reference of the resource to display")
    @PropertyDisplayType(EntityReferenceString.class)
    @PropertyFeature("reference")
    @PropertyGroup("stringReference")
    public void setReference(String reference)
    {
        this.reference = reference;
    }

    /**
     * @return the reference of the resource to include
     * @since 3.4M1
     */
    public String getReference()
    {
        return this.reference;
    }

    /**
     * @param sectionId see {@link #getSection()}
     */
    @PropertyDescription("an optional id of a section to include in the specified document, e.g. 'HMyHeading'")
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
     * @param excludeFirstHeading {@code true} to remove the first heading found inside the document or the section,
     *            {@code false} to keep it
     * @since 12.4RC1
     */
    @PropertyName("Exclude First Heading")
    @PropertyDescription("Exclude the first heading from the included document or section.")
    @PropertyAdvanced
    public void setExcludeFirstHeading(boolean excludeFirstHeading)
    {
        this.excludeFirstHeading = excludeFirstHeading;
    }

    /**
     * @return whether to exclude the first heading from the included document or section, or not.
     * @since 12.4RC1
     */
    public boolean isExcludeFirstHeading()
    {
        return this.excludeFirstHeading;
    }

    /**
     * @param context defines whether the included page is executed in its separated execution context or whether it's
     *            executed in the context of the current page.
     */
    @PropertyDescription("defines whether the included page is executed in its separated execution context"
        + " or whether it's executed in the context of the current page")
    @PropertyAdvanced
    // Marked deprecated since there's now a Display macro instead.
    @Deprecated
    public void setContext(Context context)
    {
        this.context = context;
    }

    /**
     * @return defines whether the included page is executed in its separated execution context or whether it's executed
     *         in the context of the current page.
     */
    public Context getContext()
    {
        return this.context;
    }

    /**
     * @param type the type of the reference
     * @since 3.4M1
     */
    @PropertyDescription("the type of the reference")
    @PropertyGroup("stringReference")
    @PropertyAdvanced
    // Marking it as Display Hidden because it's complex and we don't want to confuse our users.
    @PropertyDisplayHidden
    public void setType(EntityType type)
    {
        this.type = type;
    }

    /**
     * @return the type of the reference
     * @since 3.4M1
     */
    public EntityType getType()
    {
        return this.type;
    }

    /**
     * @param page the reference of the page to include
     * @since 10.6RC1
     */
    @PropertyDescription("The reference of the page to include")
    @PropertyDisplayType(PageReference.class)
    @PropertyFeature("reference")
    // Display hidden because we don't want to confuse our users by proposing two ways to enter the reference to
    // include and ATM we don't have a picker for PageReference types and we do have a picker for EntityReference string
    // one so we choose to keep the other one visible and hide this one. We're keeping the property so that we don't
    // break backward compatibility when using the macro in wiki edit mode.
    @PropertyDisplayHidden
    public void setPage(String page)
    {
        this.reference = page;
        this.type = EntityType.PAGE;
    }

    /**
     * @return the author to use to execute the content when {@link #getContext()} is {@link Context#CURRENT}
     * @since 15.0RC1
     * @since 14.10.2
     */
    public Author getAuthor()
    {
        return this.author;
    }

    /**
     * @param author the author to use to execute the content when {@link #getContext()} is {@link Context#CURRENT}
     * @since 15.0RC1
     * @since 14.10.2
     */
    @PropertyDescription("The author to use to execute the content when context is \"Current\"")
    @PropertyAdvanced
    public void setAuthor(Author author)
    {
        this.author = author;
    }
}
