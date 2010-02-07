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
package org.xwiki.gwt.wysiwyg.client.plugin.macro.input;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.ListBox;

/**
 * A concrete input control that allows the user to choose one of the available options.
 * 
 * @version $Id$
 */
public class ChoiceInput extends AbstractInput
{
    /**
     * Creates a new choice input control that wraps the given {@link ListBox} widget.
     * 
     * @param list the list box widget to be wrapped
     */
    public ChoiceInput(ListBox list)
    {
        initWidget(list);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInput#setFocus(boolean)
     */
    public void setFocus(boolean focused)
    {
        ((Focusable) getWidget()).setFocus(focused);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInput#getValue()
     */
    public String getValue()
    {
        ListBox list = (ListBox) getWidget();
        return list.getSelectedIndex() < 0 ? null : list.getValue(list.getSelectedIndex());
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInput#setValue(String)
     */
    public void setValue(String value)
    {
        ((ListBox) getWidget()).setSelectedIndex(indexOf(value));
    }

    /**
     * Searches for the given value in the list of options.
     * 
     * @param value the value to search for
     * @return the index of the value in the list, if found, {@code -1} otherwise
     */
    protected int indexOf(String value)
    {
        ListBox list = (ListBox) getWidget();
        for (int i = 0; i < list.getItemCount(); i++) {
            if (list.getValue(i).equalsIgnoreCase(value)) {
                return i;
            }
        }
        return -1;
    }
}
