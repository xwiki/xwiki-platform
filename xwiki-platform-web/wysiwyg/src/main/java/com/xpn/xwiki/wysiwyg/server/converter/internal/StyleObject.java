/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author Christian Gmeiner
 */
package com.xpn.xwiki.wysiwyg.server.converter.internal;

public class StyleObject
{
    /**
     * @see #isBold()
     */
    private boolean bold;

    /**
     * @see #isItalic()
     */
    private boolean italic;

    /**
     * @see #isDirty()
     */
    private boolean dirty;

    /**
     * @see #isStyleTag()
     */
    private boolean styleTag;

    /**
     * @see #getFontSize()
     */
    private String fontSize;

    /**
     * @see #getFontFamily()
     */
    private String fontFamily;

    /**
     * @see #getColor()
     */
    private String color;

    /**
     * @see #getWidth()
     */
    private String width;

    /**
     * @see #getHight()
     */
    private String hight;

    /**
     * Constructor.
     */
    public StyleObject()
    {
        this.bold = false;
        this.italic = false;
        this.dirty = false;
        this.styleTag = false;
    }

    /**
     * @return the bold
     */
    public boolean isBold()
    {
        return bold;
    }

    /**
     * @param bold the bold to set
     */
    public void setBold(boolean bold)
    {
        this.dirty = true;
        this.bold = bold;
    }

    /**
     * @return the italic
     */
    public boolean isItalic()
    {
        return italic;
    }

    /**
     * @param italic the italic to set
     */
    public void setItalic(boolean italic)
    {
        this.dirty = true;
        this.italic = italic;
    }

    /**
     * @return if our StyleObject is dirty
     */
    public boolean isDirty()
    {
        return dirty;
    }

    public boolean isStyleTag()
    {
        return styleTag;
    }

    public void setStyleTag(boolean styleTag)
    {
        this.styleTag = styleTag;
    }

    public String getFontSize()
    {
        return fontSize;
    }

    public void setFontSize(String fontSize)
    {
        this.fontSize = fontSize;
        this.styleTag = true;
        this.dirty = true;
    }

    public String getFontFamily()
    {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily)
    {
        this.fontFamily = fontFamily;
        this.styleTag = true;
        this.dirty = true;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
        this.styleTag = true;
        this.dirty = true;
    }

    public String getWidth()
    {
        return width;
    }

    public void setWidth(String width)
    {
        this.width = width;
        this.styleTag = true;
        this.dirty = true;
    }

    public String getHight()
    {
        return hight;
    }

    public void setHight(String hight)
    {
        this.hight = hight;
        this.styleTag = true;
        this.dirty = true;
    }
}
