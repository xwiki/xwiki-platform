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
package com.xpn.xwiki.wysiwyg.client.plugin.macro;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.input.HasFocus;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.input.HasValue;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.input.InputFactory;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;

/**
 * Displays a macro parameter allowing us to change its value.
 * 
 * @version $Id$
 */
public class ParameterDisplayer
{
    /**
     * Describes the displayed parameter.
     */
    private final ParameterDescriptor descriptor;

    /**
     * The container of the label and input control used to edit the parameter.
     */
    private final FlowPanel container;

    /**
     * Holds the text displayed after the validation, if the input value is not valid.
     */
    private final Label validationMessage;

    /**
     * The input control used to edit the value of this parameter.
     */
    private final Widget input;

    /**
     * Creates a new displayer for the specified parameter.
     * 
     * @param descriptor describes the parameter to be displayed
     */
    public ParameterDisplayer(ParameterDescriptor descriptor)
    {
        this.descriptor = descriptor;

        Label label = new Label(descriptor.getName());
        label.setStylePrimaryName("xMacroParameterLabel");
        if (descriptor.isMandatory()) {
            label.addStyleDependentName("mandatory");
        }

        Label description = new Label(descriptor.getDescription());
        description.addStyleName("xMacroParameterDescription");

        input = InputFactory.createInput(descriptor.getType());
        // Specify an id for debugging and testing.
        if (StringUtils.isEmpty(input.getElement().getId())) {
            input.getElement().setId("pd-" + descriptor.getName() + "-input");
        }

        validationMessage = new Label();
        validationMessage.setVisible(false);
        validationMessage.addStyleName("xMacroParameterError");

        container = new FlowPanel();
        container.addStyleName("xMacroParameter");
        container.add(label);
        container.add(description);
        container.add(validationMessage);
        container.add(input);
    }

    /**
     * @return {@link #descriptor}
     */
    public ParameterDescriptor getDescriptor()
    {
        return descriptor;
    }

    /**
     * @return the widget used to display and edit the underlying macro parameter
     */
    public Widget getWidget()
    {
        return container;
    }

    /**
     * Explicitly focus/unfocus the input control. Only one widget can have focus at a time, and the widget that does
     * will receive all keyboard events.
     * 
     * @param focused whether the input control should take focus or release it
     */
    public void setFocused(boolean focused)
    {
        ((HasFocus) input).setFocus(focused);
    }

    /**
     * @return the current value of the underlying macro parameter
     */
    public String getValue()
    {
        return ((HasValue) input).getValue();
    }

    /**
     * Sets the displayed value of the underlying macro parameter.
     * 
     * @param value the value to set
     */
    public void setValue(String value)
    {
        ((HasValue) input).setValue(value);
    }

    /**
     * Validates the current value of the displayed macro parameter and shows a notification message near the input
     * control if the value is illegal.
     * 
     * @return {@code true} if the current value is legal, {@code false} otherwise
     */
    public boolean validate()
    {
        // Let's suppose the current value is valid.
        validationMessage.setVisible(false);

        if (descriptor.isMandatory() && StringUtils.isEmpty(getValue())) {
            validationMessage.setText(Strings.INSTANCE.macroParameterMandatory());
            validationMessage.setVisible(true);
            return false;
        }

        return true;
    }
}
