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
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.TextBoxBase;

/**
 * A concrete input control used to collect text from the user.
 * 
 * @version $Id$
 */
public class TextInput extends AbstractInput
{
    /**
     * Creates a new text input that wraps a text box.
     * 
     * @param textBox the text box used to collect test from the user
     */
    public TextInput(TextBoxBase textBox)
    {
        initWidget(textBox);
    }

    @Override
    public void setFocus(boolean focused)
    {
        ((Focusable) getWidget()).setFocus(focused);
    }

    @Override
    public String getValue()
    {
        return ((HasText) getWidget()).getText();
    }

    @Override
    public void setValue(String value)
    {
        ((HasText) getWidget()).setText(value);
    }
}
