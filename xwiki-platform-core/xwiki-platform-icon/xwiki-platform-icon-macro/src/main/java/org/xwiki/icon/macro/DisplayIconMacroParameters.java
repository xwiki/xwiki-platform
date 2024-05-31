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
package org.xwiki.icon.macro;

import org.xwiki.icon.macro.internal.DisplayIconMacro;
import org.xwiki.properties.annotation.PropertyAdvanced;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.properties.annotation.PropertyName;
import org.xwiki.stability.Unstable;

/**
 * Parameters for the {@link DisplayIconMacro} Macro.
 *
 * @version $Id$
 * @since 14.10.6
 * @since 15.2RC1
 */
@Unstable
public class DisplayIconMacroParameters
{
    private String name;

    private String iconSet;

    private String textAlternative;

    private boolean fallback = true;

    /**
     * @return the name of the icon
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param name the name of the icon
     */
    @PropertyName("Name")
    @PropertyDescription("The name of the icon.")
    @PropertyMandatory
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the name of the icon theme
     */
    public String getIconSet()
    {
        return this.iconSet;
    }

    /**
     * @param iconSet the name of the icon theme
     */
    @PropertyName("Icon Set")
    @PropertyDescription("The name of the icon set")
    public void setIconSet(String iconSet)
    {
        this.iconSet = iconSet;
    }

    /**
     * @return if the icon shall be loaded from the default icon set when the icon or icon set is not available, true
     * by default
     */
    public boolean isFallback()
    {
        return this.fallback;
    }

    /**
     * @param fallback if the icon shall be loaded from the default icon set when the icon or icon set is not available
     * icon set
     */
    @PropertyName("Fallback")
    @PropertyDescription("If the icon shall be loaded from the default icon set when the icon or icon set is not "
        + "available, true by default")
    @PropertyAdvanced
    public void setFallback(boolean fallback)
    {
        this.fallback = fallback;
    }

    /**
     * @since 16.5.0RC1
     * @return the text alternative picked for the icon
     */
    public String getTextAlternative()
    {
        return this.textAlternative;
    }

    /**
     * @since 16.5.0RC1
     * @param textAlternative a text alternative for the icon
     */
    @PropertyName("Text Alternative")
    @PropertyDescription("A text alternative for this icon.")
    public void setTextAlternative(String textAlternative)
    {
        this.textAlternative = textAlternative;
    }
}
