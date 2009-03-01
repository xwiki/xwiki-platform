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
package com.xpn.xwiki.wysiwyg.client.plugin.macro.input;

import com.google.gwt.user.client.ui.ListBox;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;

/**
 * A concrete input control used to collect a boolean value from the user.
 * 
 * @version $Id$
 */
public class BooleanInput extends AbstractInput
{
    /**
     * Creates a new boolean input control.
     */
    public BooleanInput()
    {
        ListBox list = new ListBox();
        list.addItem("");
        list.addItem(Strings.INSTANCE.yes());
        list.addItem(Strings.INSTANCE.no());

        initWidget(list);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInput#setFocus(boolean)
     */
    public void setFocus(boolean focused)
    {
        ((com.google.gwt.user.client.ui.HasFocus) getWidget()).setFocus(focused);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInput#getValue()
     */
    public String getValue()
    {
        ListBox list = (ListBox) getWidget();
        switch (list.getSelectedIndex()) {
            case 1:
                return Boolean.TRUE.toString();
            case 2:
                return Boolean.FALSE.toString();
            default:
                return "";
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInput#setValue(String)
     */
    public void setValue(String value)
    {
        ListBox list = (ListBox) getWidget();
        if (StringUtils.isEmpty(value)) {
            list.setSelectedIndex(0);
        } else if (Boolean.valueOf(value)) {
            list.setSelectedIndex(1);
        } else {
            list.setSelectedIndex(2);
        }
    }
}
