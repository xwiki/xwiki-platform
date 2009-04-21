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
package com.xpn.xwiki.wysiwyg.client.plugin.font;

import com.google.gwt.user.client.ui.ListBox;

/**
 * Abstract {@link Picker} that uses a {@link ListBox}.
 * 
 * @version $Id$
 */
public abstract class AbstractListBoxPicker extends ListBox implements Picker
{
    /**
     * Creates a new empty list box picker.
     */
    public AbstractListBoxPicker()
    {
        super(false);
        setVisibleItemCount(1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ListBox#insertItem(String, String, int)
     */
    public void insertItem(String item, String value, int index)
    {
        super.insertItem(item, value, index);
        setValue(index == -1 ? getItemCount() - 1 : index, value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Picker#getSelectedValue()
     */
    public String getSelectedValue()
    {
        return getSelectedIndex() < 0 ? null : getValue(getSelectedIndex());
    }
}
