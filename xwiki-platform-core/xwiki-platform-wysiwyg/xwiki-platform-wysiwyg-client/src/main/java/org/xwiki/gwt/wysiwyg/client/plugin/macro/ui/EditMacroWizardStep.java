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
package org.xwiki.gwt.wysiwyg.client.plugin.macro.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroCall;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroDescriptor;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroServiceAsync;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.ParameterDescriptor;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.ParameterDisplayer;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;

/**
 * Wizard step for editing macro parameters and content.
 * 
 * @version $Id$
 */
public class EditMacroWizardStep extends AbstractMacroWizardStep
{
    /**
     * The call-back used by the edit dialog to be notified when a macro descriptor is being received from the server.
     * Only the last request for a macro descriptor triggers an update of the edit dialog.
     */
    private class MacroDescriptorAsyncCallback implements AsyncCallback<MacroDescriptor>
    {
        /**
         * The index of the request.
         */
        private int index;

        /**
         * The call-back used to notify the wizard that this wizard step has finished loading.
         */
        private final AsyncCallback< ? > wizardCallback;

        /**
         * Creates a new call-back for the request with the specified index.
         * 
         * @param wizardCallback the call-back used to notify the wizard that this wizard step has finished loading
         */
        public MacroDescriptorAsyncCallback(AsyncCallback< ? > wizardCallback)
        {
            this.wizardCallback = wizardCallback;
            this.index = ++macroDescriptorRequestIndex;
        }

        /**
         * {@inheritDoc}
         * 
         * @see AsyncCallback#onFailure(Throwable)
         */
        public void onFailure(Throwable caught)
        {
            wizardCallback.onFailure(caught);
        }

        /**
         * {@inheritDoc}
         * 
         * @see AsyncCallback#onSuccess(Object)
         */
        public void onSuccess(MacroDescriptor result)
        {
            // If this is the response to the last request and the wizard step wasn't canceled in the mean time then..
            if (index == macroDescriptorRequestIndex && macroCall != null) {
                fill(result);
                wizardCallback.onSuccess(null);
            }
        }
    }

    /**
     * The input and output of this wizard step. The macro call being edited.
     * 
     * @see #init(Object, AsyncCallback)
     * @see #getResult()
     */
    private MacroCall macroCall;

    /**
     * Describes the macro used in {@link #macroCall}.
     */
    private MacroDescriptor macroDescriptor;

    /**
     * The index of the last request for a macro descriptor.
     */
    private int macroDescriptorRequestIndex = -1;

    /**
     * Displays the content of a macro and allows us to edit it.
     */
    private ParameterDisplayer contentDisplayer;

    /**
     * A parameter displayer shows the value of a macro parameter and allows us to edit it.
     */
    private final List<ParameterDisplayer> parameterDisplayers = new ArrayList<ParameterDisplayer>();

    /**
     * Creates a new wizard step for editing macro parameters and content.
     * 
     * @param config the object used to configure the newly created wizard step
     * @param macroService the macro service used to retrieve macro descriptors
     */
    public EditMacroWizardStep(Config config, MacroServiceAsync macroService)
    {
        super(config, macroService);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMacroWizardStep#getResult()
     */
    public Object getResult()
    {
        return macroCall;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMacroWizardStep#getStepTitle()
     */
    public String getStepTitle()
    {
        String macroName =
            macroDescriptor != null ? macroDescriptor.getName() : (macroCall != null ? macroCall.getName() : null);
        return Strings.INSTANCE.macro() + (macroName != null ? " : " + macroName : "");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMacroWizardStep#init(Object, AsyncCallback)
     */
    public void init(Object data, AsyncCallback< ? > cb)
    {
        // Reset the model.
        macroCall = (MacroCall) data;
        macroDescriptor = null;

        // Reset the UI.
        display().clear();
        parameterDisplayers.clear();
        contentDisplayer = null;

        getMacroService().getMacroDescriptor(macroCall.getName(), getConfig().getParameter("syntax"),
            new MacroDescriptorAsyncCallback(cb));
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMacroWizardStep#onCancel()
     */
    public void onCancel()
    {
        macroCall = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMacroWizardStep#onSubmit(AsyncCallback)
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        if (validate()) {
            updateMacroCall();
            async.onSuccess(true);
        } else {
            async.onSuccess(false);
        }
    }

    /**
     * Validate all the parameters and focus the first that has an illegal value.
     * 
     * @return {@code true} if the current parameter values and the current content are valid, {@code false} otherwise
     */
    private boolean validate()
    {
        ParameterDisplayer failed = contentDisplayer == null || contentDisplayer.validate() ? null : contentDisplayer;
        for (int i = parameterDisplayers.size() - 1; i >= 0; i--) {
            ParameterDisplayer displayer = parameterDisplayers.get(i);
            if (!displayer.validate()) {
                failed = displayer;
            }
        }
        if (failed != null) {
            failed.setFocused(true);
            return false;
        }
        return true;
    }

    /**
     * Updates the macro call based on the current parameter values.
     */
    private void updateMacroCall()
    {
        for (ParameterDisplayer displayer : parameterDisplayers) {
            String value = displayer.getValue();
            ParameterDescriptor descriptor = displayer.getDescriptor();
            if (StringUtils.isEmpty(value)
                || (!descriptor.isMandatory() && value.equalsIgnoreCase(descriptor.getDefaultValue()))) {
                macroCall.removeArgument(descriptor.getId());
            } else {
                macroCall.setArgument(descriptor.getId(), value);
            }
        }
        if (contentDisplayer != null) {
            macroCall.setContent(contentDisplayer.getValue());
        }
    }

    /**
     * Fills the wizard step with the controls needed to edit the underlying macro.
     * 
     * @param macroDescriptor describes the edited macro
     */
    private void fill(MacroDescriptor macroDescriptor)
    {
        this.macroDescriptor = macroDescriptor;

        // Display the macro description.
        Label macroDescription = new Label(macroDescriptor.getDescription());
        macroDescription.addStyleName("xMacroDescription");
        display().add(macroDescription);

        // Display the macro parameters.
        for (Map.Entry<String, ParameterDescriptor> entry : macroDescriptor.getParameterDescriptorMap().entrySet()) {
            ParameterDisplayer displayer = new ParameterDisplayer(entry.getValue());
            String value = macroCall.getArgument(entry.getKey());
            if (value == null) {
                // Display the default value if the macro call doesn't specify one.
                value = entry.getValue().getDefaultValue();
            }
            displayer.setValue(value);
            parameterDisplayers.add(displayer);
            display().add(displayer.getWidget());
        }

        // Display the content of the macro.
        if (macroDescriptor.getContentDescriptor() != null) {
            contentDisplayer = new ParameterDisplayer(macroDescriptor.getContentDescriptor());
            contentDisplayer.setValue(macroCall.getContent());
            display().add(contentDisplayer.getWidget());
        }

        // Focus the first input control.
        if (parameterDisplayers.size() > 0) {
            parameterDisplayers.get(0).setFocused(true);
        } else if (contentDisplayer != null) {
            contentDisplayer.setFocused(true);
        }
    }

    /**
     * @return the macroDescriptor
     */
    protected MacroDescriptor getMacroDescriptor()
    {
        return macroDescriptor;
    }

    /**
     * @return the macroCall
     */
    protected MacroCall getMacroCall()
    {
        return macroCall;
    }
}
