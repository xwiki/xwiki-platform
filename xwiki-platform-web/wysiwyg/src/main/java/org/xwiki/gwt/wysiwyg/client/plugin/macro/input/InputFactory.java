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

import java.util.Map;
import java.util.Map.Entry;

import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.ParameterType;

import com.google.gwt.user.client.ui.ListBox;
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
     * @param type the type of a macro parameter
     * @return the newly created input control
     */
    public static Widget createInput(ParameterType type)
    {
        // TODO: This needs to be improved!
        // NOTE: We hard-code the class names to be able to compile the GWT code with the disableClassMetadata flag on.
        // This way we can reduce the size of the generated JavaScript code.
        String className = type.getName();
        if ("java.lang.StringBuffer".equals(className)) {
            // Large string, let's use a text area.
            return new TextInput(new TextArea());
        } else if ("boolean".equals(className) || "java.lang.Boolean".equals(className)) {
            return createBooleanInput();
        } else if (type.isEnum()) {
            return createChoiceInput(type.getEnumConstants());
        } else {
            // By default we use an input box.
            Widget input = new TextInput(new TextBox());
            input.addStyleName("textInput");
            return input;
        }
    }

    /**
     * Creates a choice input based on the given options.
     * 
     * @param options the options the user has to choose from
     * @return the newly created choice input
     */
    protected static ChoiceInput createChoiceInput(Map<String, String> options)
    {
        ListBox list = new ListBox();
        for (Entry<String, String> option : options.entrySet()) {
            list.addItem(option.getValue(), option.getKey());
        }
        return new ChoiceInput(list);
    }

    /**
     * @return a new boolean input
     */
    protected static ChoiceInput createBooleanInput()
    {
        ListBox list = new ListBox();
        list.addItem(Strings.INSTANCE.yes(), String.valueOf(true));
        list.addItem(Strings.INSTANCE.no(), String.valueOf(false));
        return new ChoiceInput(list);
    }
}
