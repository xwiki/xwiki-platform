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
package com.xpn.xwiki.wysiwyg.client.plugin.macro.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.MacroCall;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.MacroDescriptor;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.ParameterDescriptor;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.ParameterDisplayer;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.widget.ComplexDialogBox;

/**
 * Dialog for editing macro parameters and content.
 * 
 * @version $Id$
 */
public class EditMacroDialog extends ComplexDialogBox implements ClickListener
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
         * Creates a new call-back for the request with the specified index.
         */
        public MacroDescriptorAsyncCallback()
        {
            this.index = ++EditMacroDialog.this.macroDescriptorRequestIndex;
        }

        /**
         * {@inheritDoc}
         * 
         * @see AsyncCallback#onFailure(Throwable)
         */
        public void onFailure(Throwable caught)
        {
            showError(caught);
        }

        /**
         * {@inheritDoc}
         * 
         * @see AsyncCallback#onSuccess(Object)
         */
        public void onSuccess(MacroDescriptor result)
        {
            if (index == EditMacroDialog.this.macroDescriptorRequestIndex) {
                fill(result);
            }
        }
    }

    /**
     * The button that triggers the update of the edited macro.
     */
    private final Button apply;

    /**
     * The title of the dialog. This is not the caption but the text placed in the header of the dialog.
     */
    private final Label title;

    /**
     * The object used to configure the dialog.
     */
    private final Config config;

    /**
     * The list of parameter displayers.
     */
    private final List<ParameterDisplayer> parameterDisplayers = new ArrayList<ParameterDisplayer>();

    /**
     * Displays the content of a macro and allows us to edit it.
     */
    private ParameterDisplayer contentDisplayer;

    /**
     * @see #getMacroCall()
     */
    private MacroCall macroCall;

    /**
     * The index of the last request for a macro descriptor.
     */
    private int macroDescriptorRequestIndex = -1;

    /**
     * Creates a new dialog for editing macro parameters and content. The dialog is modal.
     * 
     * @param config the object used to configure the newly created dialog
     */
    public EditMacroDialog(Config config)
    {
        super(false, true);

        this.config = config;

        getDialog().setIcon(Images.INSTANCE.macroEdit().createImage());
        getDialog().setCaption(Strings.INSTANCE.macroEditDialogCaption());

        title = new Label();
        getHeader().add(title);

        apply = new Button(Strings.INSTANCE.apply());
        apply.addClickListener(this);
        getFooter().add(apply);
    }

    /**
     * Fills the dialog with the controls needed to edit the underlying macro.
     * 
     * @param macroDescriptor describes the edited macro
     */
    private void fill(MacroDescriptor macroDescriptor)
    {
        // First get the dialog out of the loading state.
        setLoading(false);

        // Set the macro description as the tool tip for the title.
        title.setTitle(macroDescriptor.getDescription());

        // Display the macro parameters.
        for (Map.Entry<String, ParameterDescriptor> entry : macroDescriptor.getParameterDescriptorMap().entrySet()) {
            ParameterDisplayer displayer = new ParameterDisplayer(entry.getValue());
            displayer.setValue(macroCall.getArgument(entry.getKey()));
            parameterDisplayers.add(displayer);
            getBody().add(displayer.getWidget());
        }

        // Display the content of the macro.
        // The rendering doesn't provide a content descriptor so we fake one.
        ParameterDescriptor contentDescriptor = new ParameterDescriptor();
        contentDescriptor.setDescription("");
        // The macro descriptor doesn't specify if the content is mandatory or not. We suppose it isn't.
        contentDescriptor.setMandatory(false);
        contentDescriptor.setName("content");
        // Just a hack to distinguish between regular strings and large strings.
        contentDescriptor.setType(StringBuffer.class.getName());

        contentDisplayer = new ParameterDisplayer(contentDescriptor);
        contentDisplayer.setValue(macroCall.getContent());
        getBody().add(contentDisplayer.getWidget());

        // Focus the first input control.
        if (parameterDisplayers.size() > 0) {
            parameterDisplayers.get(0).setFocused(true);
        } else {
            contentDisplayer.setFocused(true);
        }

        // Now that all the controls are in place, let's enable the apply button.
        apply.setEnabled(true);
    }

    /**
     * @return the macro call being edited
     */
    public MacroCall getMacroCall()
    {
        return macroCall;
    }

    /**
     * Sets the macro call to be edited.
     * 
     * @param macroCall the macro call to be edited
     */
    public void setMacroCall(MacroCall macroCall)
    {
        this.macroCall = macroCall;

        // Clear the dialog.
        getBody().clear();
        parameterDisplayers.clear();

        // Update the title of the dialog.
        title.setText("Macro : " + macroCall.getName());

        // Disable the apply button till we receive the macro descriptor from the server.
        apply.setEnabled(false);

        // Put the dialog in loading state.
        setLoading(true);

        // Request macro descriptor.
        WysiwygService.Singleton.getInstance().getMacroDescriptor(macroCall.getName(), config.getParameter("syntax"),
            new MacroDescriptorAsyncCallback());
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == apply && validate()) {
            setCanceled(false);
            updateMacroCall();
            hide();
        }
    }

    /**
     * Validate all the parameters and focus the first that has an illegal value.
     * 
     * @return {@code true} if the current parameter values and the current content are valid, {@code false} otherwise
     */
    private boolean validate()
    {
        ParameterDisplayer failed = contentDisplayer.validate() ? null : contentDisplayer;
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
            if (StringUtils.isEmpty(displayer.getValue())) {
                macroCall.removeArgument(displayer.getDescriptor().getName());
            } else {
                macroCall.setArgument(displayer.getDescriptor().getName(), displayer.getValue());
            }
        }
        macroCall.setContent(contentDisplayer.getValue());
    }
}
