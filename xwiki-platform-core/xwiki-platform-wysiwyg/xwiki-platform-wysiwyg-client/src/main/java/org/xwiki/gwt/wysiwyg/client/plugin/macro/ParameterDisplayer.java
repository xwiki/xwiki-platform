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
package org.xwiki.gwt.wysiwyg.client.plugin.macro;

import org.xwiki.gwt.user.client.FocusCommand;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.input.HasFocus;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.input.HasValue;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.input.InputFactory;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

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

        Panel label = new FlowPanel();
        label.setStylePrimaryName("xMacroParameterLabel");
        label.add(new InlineLabel(descriptor.getName()));
        if (descriptor.isMandatory()) {
            InlineLabel mandatoryLabel = new InlineLabel(Strings.INSTANCE.mandatory());
            mandatoryLabel.addStyleName("xMandatory");
            label.add(mandatoryLabel);
        }

        Label description = new Label(descriptor.getDescription());
        description.addStyleName("xMacroParameterDescription");

        input = InputFactory.createInput(descriptor.getType());
        // Specify an id for debugging and testing.
        if (StringUtils.isEmpty(input.getElement().getId())) {
            input.getElement().setId("pd-" + descriptor.getId() + "-input");
        }

        validationMessage = new Label();
        validationMessage.setVisible(false);
        validationMessage.addStyleName("xErrorMsg");

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
    public void setFocused(final boolean focused)
    {
        // FIXME: hack to avoid changing the whole inputs class hierarchy to implement Focusable and to allow callers
        // to still use setFocused
        // also, extend FocusCommand anonymously instead of just Command so that we find this piece here in usages of
        // FocusCommand, for future maintenance
        Scheduler.get().scheduleDeferred(new FocusCommand(null)
        {
            @Override
            public void execute()
            {
                ((HasFocus) input).setFocus(focused);
            }
        });
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
        String errorFieldStyle = "xErrorField";
        // Let's suppose the current value is valid.
        validationMessage.setVisible(false);
        input.removeStyleName(errorFieldStyle);

        if (descriptor.isMandatory() && StringUtils.isEmpty(getValue())) {
            validationMessage.setText(Strings.INSTANCE.macroParameterMandatory());
            validationMessage.setVisible(true);
            input.addStyleName(errorFieldStyle);
            return false;
        }

        return true;
    }
}
