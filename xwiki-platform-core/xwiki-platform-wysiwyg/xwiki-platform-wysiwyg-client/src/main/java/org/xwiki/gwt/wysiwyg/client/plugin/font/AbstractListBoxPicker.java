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

import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.user.client.ui.ListBox;

/**
 * Abstract {@link Picker} that uses a {@link ListBox}.
 * 
 * @version $Id$
 */
public abstract class AbstractListBoxPicker extends ListBox implements Picker
{
    /**
     * The object used to match list item.
     */
    private final Matcher<String> matcher = new DefaultStringMatcher();

    /**
     * Creates a new empty list box picker.
     */
    public AbstractListBoxPicker()
    {
        super(false);
        setVisibleItemCount(1);
    }

    @Override
    public void insertItem(String item, String value, int index)
    {
        super.insertItem(item, value, index);
        setValue(index == -1 ? getItemCount() - 1 : index, value);
    }

    @Override
    public String getSelectedValue()
    {
        return getSelectedIndex() < 0 ? null : getValue(getSelectedIndex());
    }

    @Override
    public void setSelectedValue(String value)
    {
        setSelectedValue(value, matcher);
    }

    /**
     * Looks for a list item that matches the given value and selects the one that is found.
     * 
     * @param value the value to look for
     * @param matcher the object used to match the given value with the list item values
     */
    protected void setSelectedValue(String value, Matcher<String> matcher)
    {
        // First, see if the given value is already selected.
        if (getSelectedIndex() >= 0 && matcher.match(getValue(getSelectedIndex()), value)) {
            return;
        }
        // Look for a matching value.
        for (int i = getItemCount() - 1; i >= 0; i--) {
            if (matcher.match(getValue(i), value)) {
                setSelectedIndex(i);
                return;
            }
        }
        // No matching value found.
        setSelectedIndex(-1);
    }

    /**
     * NOTE: We added this method because it is declared as private in the base class.
     * 
     * @return the select HTML element used by this list box picker
     */
    protected SelectElement getSelectElement()
    {
        return getElement().cast();
    }
}
