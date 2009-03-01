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

import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Creates input controls for specific data types.
 * 
 * @version $Id$
 */
public final class InputFactory
{
    /**
     * Default constructor.
     */
    private InputFactory()
    {
        // Utility classes must not have a public or default constructor.
    }

    /**
     * Creates a new input control that collects user data of the specified type. Common types are {@code
     * java.lang.String} and {@code boolean}.
     * 
     * @param type a fully qualified class name or the name of a primitive type
     * @return the newly created input control
     */
    public static Widget createInput(String type)
    {
        // TODO: This needs to be improved!
        if (StringBuffer.class.getName().equals(type)) {
            // Large string, let's use a text area.
            return new TextInput(new TextArea());
        } else if (boolean.class.getName().equals(type) || Boolean.class.getName().equals(type)) {
            return new BooleanInput();
        } else {
            // By default we use an input box.
            Widget input = new TextInput(new TextBox());
            input.addStyleName("textInput");
            return input;
        }
    }
}
