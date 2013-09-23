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
package org.xwiki.gwt.wysiwyg.client.gadget;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroCall;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroServiceAsync;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.input.TextInput;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.ui.EditMacroWizardStep;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Wizard step to edit the parameters of the selected gadget.
 * 
 * @version $Id$
 * @since 3.0M3
 */
public class EditGadgetWizardStep extends EditMacroWizardStep
{
    /**
     * The widget holding the input for the title of this gadget.
     */
    protected TextInput titleInput;

    /**
     * Creates a gadget edit wizard step.
     * 
     * @param config the configuration of the wysiwyg
     * @param macroService the macro service to get information about the macros from the server side
     */
    public EditGadgetWizardStep(Config config, MacroServiceAsync macroService)
    {
        super(config, macroService);
    }

    /**
     * {@inheritDoc}
     * 
     * @see EditMacroWizardStep#getStepTitle()
     */
    @Override
    public String getStepTitle()
    {
        String macroName =
            getMacroDescriptor() != null ? getMacroDescriptor().getName() : (getMacroCall() != null ? getMacroCall()
                .getName() : null);
        return Strings.INSTANCE.gadget() + (macroName != null ? " : " + macroName : "");
    }

    /**
     * {@inheritDoc}
     * 
     * @see EditMacroWizardStep#init(java.lang.Object, AsyncCallback)
     */
    @Override
    public void init(Object data, AsyncCallback< ? > cb)
    {
        GadgetInstance gadgetInstance = (GadgetInstance) data;
        super.init(gadgetInstance.getMacroCall(), cb);

        // insert the title on the first position in the content panel
        display().insert(getTitlePanel(gadgetInstance.getTitle()), 0);
        // and focus the title input
        // FIXME: this doesn't work, need to hook on the insert of all the parameter editors in the param displayers
        titleInput.setFocus(true);
    }

    /**
     * @param title the title to fill in the title input by default
     * @return the panel for the title input for this gadget
     */
    private Panel getTitlePanel(String title)
    {
        Panel container = new FlowPanel();
        container.addStyleName("xMacroParameter");
        Panel label = new FlowPanel();
        label.setStylePrimaryName("xMacroParameterLabel");
        label.add(new InlineLabel(Strings.INSTANCE.gadgetTitleLabel()));
        container.add(label);
        Label description = new Label(Strings.INSTANCE.gadgetTitleDescription());
        description.addStyleName("xMacroParameterDescription");
        container.add(description);
        titleInput = new TextInput(new TextBox());
        titleInput.setValue(title);
        titleInput.addStyleName("textInput");
        container.add(titleInput);

        return container;
    }

    /**
     * {@inheritDoc}
     * 
     * @see EditMacroWizardStep#getResult()
     */
    @Override
    public Object getResult()
    {
        // build a macro instance from the macro call build by the super class and the title filled in in the title
        // widget
        GadgetInstance gadgetInstance = new GadgetInstance();
        gadgetInstance.setMacroCall((MacroCall) super.getResult());
        gadgetInstance.setTitle(titleInput.getValue());

        return gadgetInstance;
    }
}
