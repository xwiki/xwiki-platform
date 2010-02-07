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
 * A widget used for choosing a font size.
 * 
 * @version $Id$
 */
public class FontSizePicker extends CachedListBoxPicker
{
    /**
     * The object used to compare font sizes.
     */
    private final Matcher<String> matcher = new FontSizeComparator();

    /**
     * Creates a new empty font size picker.
     */
    public FontSizePicker()
    {
        addStyleName("xFontSizePicker");
        setAdditionalOptionGroupLabel(Strings.INSTANCE.fontSizeOther());
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
        option.getStyle().setProperty(Style.FONT_SIZE.getJSName(), value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see CachedListBoxPicker#setSelectedValue(String)
     */
    public void setSelectedValue(String value)
    {
        if (isRelative(value)) {
            super.setSelectedValue(value);
        } else {
            setSelectedValue(value, matcher);
        }
    }

    /**
     * @param value the value of the font-size CSS property
     * @return {@code true} if the given value expresses a relative font size, {@code false} otherwise
     */
    private boolean isRelative(String value)
    {
        return value == null || value.endsWith("%") || Style.FontSize.SMALLER.equalsIgnoreCase(value)
            || Style.FontSize.LARGER.equalsIgnoreCase(value);
    }
}
