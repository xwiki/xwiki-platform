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
package org.xwiki.image.style.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * The image style POJO. Contains the list of all the configurable properties of an image style.
 *
 * @version $Id$
 * @since 14.3RC1
 */
public class ImageStyle
{
    private String identifier;

    private String prettyName;

    private String type;

    private Boolean adjustableSize;

    private Long defaultWidth;

    private Long defaultHeight;

    private Boolean adjustableBorder;

    private Boolean defaultBorder;

    private Boolean adjustableAlignment;

    private String defaultAlignment;

    private Boolean adjustableTextWrap;

    private Boolean defaultTextWrap;

    /**
     * @return the unique identifier of the style (e.g., "thumbnail")
     */
    public String getIdentifier()
    {
        return this.identifier;
    }

    /**
     * @param identifier the unique identifier of the style (e.g., "thumbnail")
     * @return the current object
     */
    public ImageStyle setIdentifier(String identifier)
    {
        this.identifier = identifier;
        return this;
    }

    /**
     * @return the pretty name of the style (e.g., "Thumbnail")
     */
    public String getPrettyName()
    {
        return this.prettyName;
    }

    /**
     * @param prettyName the pretty name of the style (e.g., "Thumbnail")
     * @return the current object
     */
    public ImageStyle setPrettyName(String prettyName)
    {
        this.prettyName = prettyName;
        return this;
    }

    /**
     * @return the type of the style (e.g., "thumbnail-style")
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param type the type of the style (e.g., "thumbnail-style")
     * @return the current object
     */
    public ImageStyle setType(String type)
    {
        this.type = type;
        return this;
    }

    /**
     * @return {@code true} if the style allows user to adjust the size of the image, {@code false} otherwise
     */
    public Boolean getAdjustableSize()
    {
        return this.adjustableSize;
    }

    /**
     * @param adjustableSize {@code true} if the style allows user to adjust the size of the image, {@code false}
     *     otherwise
     * @return the current object
     */
    public ImageStyle setAdjustableSize(Boolean adjustableSize)
    {
        this.adjustableSize = adjustableSize;
        return this;
    }

    /**
     * @return the default image width size in pixels, or {@code null} if undefined
     */
    public Long getDefaultWidth()
    {
        return this.defaultWidth;
    }

    /**
     * @param defaultWidth the default image width size in pixels, or {@code null} if undefined
     * @return the current object
     */
    public ImageStyle setDefaultWidth(Long defaultWidth)
    {
        this.defaultWidth = defaultWidth;
        return this;
    }

    /**
     * @return the default image height size in pixels, or {@code null} if undefined
     */
    public Long getDefaultHeight()
    {
        return this.defaultHeight;
    }

    /**
     * @param defaultHeight the default image height size in pixels, or {@code null} if undefined
     * @return the current object
     */
    public ImageStyle setDefaultHeight(Long defaultHeight)
    {
        this.defaultHeight = defaultHeight;
        return this;
    }

    /**
     * @return {@code true} if the style allows user to adjust the border configuration of the image, {@code false}
     *     otherwise
     */
    public Boolean getAdjustableBorder()
    {
        return this.adjustableBorder;
    }

    /**
     * @param adjustableBorder {@code true} if the style allows user to adjust the border configuration of the
     *     image, {@code false}
     * @return the current object
     */
    public ImageStyle setAdjustableBorder(Boolean adjustableBorder)
    {
        this.adjustableBorder = adjustableBorder;
        return this;
    }

    /**
     * @return {@code true} when the image has a border, {@code false} otherwise
     */
    public Boolean getDefaultBorder()
    {
        return this.defaultBorder;
    }

    /**
     * @param defaultBorder {@code true} when the image has a border, {@code false} otherwise
     * @return the current object
     */
    public ImageStyle setDefaultBorder(Boolean defaultBorder)
    {
        this.defaultBorder = defaultBorder;
        return this;
    }

    /**
     * @return {@code true} when the alignment is adjustable, {@code false} otherwise
     */
    public Boolean getAdjustableAlignment()
    {
        return this.adjustableAlignment;
    }

    /**
     * @param adjustableAlignment {@code true} when the alignment is adjustable, {@code false} otherwise
     * @return the current object
     */
    public ImageStyle setAdjustableAlignment(Boolean adjustableAlignment)
    {
        this.adjustableAlignment = adjustableAlignment;
        return this;
    }

    /**
     * @return the default alignment of the image, {@code null} if undefined
     */
    public String getDefaultAlignment()
    {
        return this.defaultAlignment;
    }

    /**
     * @param defaultAlignment the default alignment of the image, {@code null} if undefined
     * @return the current object
     */
    public ImageStyle setDefaultAlignment(String defaultAlignment)
    {
        this.defaultAlignment = defaultAlignment;
        return this;
    }

    /**
     * @return {@code true} if the text wrap is adjustable, {@code false} otherwise
     */
    public Boolean getAdjustableTextWrap()
    {
        return this.adjustableTextWrap;
    }

    /**
     * @param adjustableTextWrap {@code true} if the text wrap is adjustable, {@code false} otherwise
     * @return the current object
     */
    public ImageStyle setAdjustableTextWrap(Boolean adjustableTextWrap)
    {
        this.adjustableTextWrap = adjustableTextWrap;
        return this;
    }

    /**
     * @return the default text wrap configuration, {@code null} if undefined
     */
    public Boolean getDefaultTextWrap()
    {
        return this.defaultTextWrap;
    }

    /**
     * @param defaultTextWrap the default text wrap configuration, {@code null} if undefined
     * @return the current object
     */
    public ImageStyle setDefaultTextWrap(Boolean defaultTextWrap)
    {
        this.defaultTextWrap = defaultTextWrap;
        return this;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ImageStyle that = (ImageStyle) o;

        return new EqualsBuilder()
            .append(this.identifier, that.identifier)
            .append(this.prettyName, that.prettyName)
            .append(this.type, that.type)
            .append(this.adjustableSize, that.adjustableSize)
            .append(this.defaultWidth, that.defaultWidth)
            .append(this.defaultHeight, that.defaultHeight)
            .append(this.adjustableBorder, that.adjustableBorder)
            .append(this.defaultBorder, that.defaultBorder)
            .append(this.adjustableAlignment, that.adjustableAlignment)
            .append(this.defaultAlignment, that.defaultAlignment)
            .append(this.adjustableTextWrap, that.adjustableTextWrap)
            .append(this.defaultTextWrap, that.defaultTextWrap)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.identifier)
            .append(this.prettyName)
            .append(this.type)
            .append(this.adjustableSize)
            .append(this.defaultWidth)
            .append(this.defaultHeight)
            .append(this.adjustableBorder)
            .append(this.defaultBorder)
            .append(this.adjustableAlignment)
            .append(this.defaultAlignment)
            .append(this.adjustableTextWrap)
            .append(this.defaultTextWrap)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("identifier", this.identifier)
            .append("prettyName", this.prettyName)
            .append("type", this.type)
            .append("adjustableSize", this.adjustableSize)
            .append("defaultWidth", this.defaultWidth)
            .append("defaultHeight", this.defaultHeight)
            .append("adjustableBorder", this.adjustableBorder)
            .append("defaultBorder", this.defaultBorder)
            .append("adjustableAlignment", this.adjustableAlignment)
            .append("defaultAlignment", this.defaultAlignment)
            .append("adjustableTextWrap", this.adjustableTextWrap)
            .append("defaultTextWrap", this.defaultTextWrap)
            .toString();
    }
}
