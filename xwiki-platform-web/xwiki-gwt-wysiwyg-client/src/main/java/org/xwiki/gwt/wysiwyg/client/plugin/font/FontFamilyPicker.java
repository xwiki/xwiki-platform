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
package org.xwiki.gwt.wysiwyg.client.plugin.font;

import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.wysiwyg.client.Strings;

import com.google.gwt.dom.client.OptionElement;

/**
 * A widget used for choosing a font family.
 * 
 * @version $Id$
 */
public class FontFamilyPicker extends CachedListBoxPicker
{
    /**
     * The object used to match font families.
     */
    private final Matcher<String> matcher = new FontFamilyMatcher();

    /**
     * Creates a new empty font family picker.
     */
    public FontFamilyPicker()
    {
        addStyleName("xFontFamilyPicker");
        setAdditionalOptionGroupLabel(Strings.INSTANCE.fontNameOther());
    }

    /**
     * {@inheritDoc}
     * 
     * @see CachedListBoxPicker#setValue(int, String)
     */
    public void setValue(int index, String value)
    {
        super.setValue(index, value);
        OptionElement option = getSelectElement().getOptions().getItem(index);
        option.getStyle().setProperty(Style.FONT_FAMILY.getJSName(), value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see CachedListBoxPicker#setSelectedValue(String)
     */
    public void setSelectedValue(String value)
    {
        setSelectedValue(value, matcher);
    }
}
