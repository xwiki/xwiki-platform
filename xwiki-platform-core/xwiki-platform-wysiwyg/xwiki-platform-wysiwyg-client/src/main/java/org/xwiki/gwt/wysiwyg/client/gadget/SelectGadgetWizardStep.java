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
import org.xwiki.gwt.wysiwyg.client.plugin.macro.ui.SelectMacroWizardStep;

/**
 * The wizard step to select a gadget, extending the macro selecting step.
 * <p>
 * FIXME: remove some macros from the list, the ones that don't make sense as gadgets such as "dashboard", or that are
 * not part of the gadgets category. Add multiple categories to macros.
 * 
 * @version $Id$
 * @since 3.0M3
 */
public class SelectGadgetWizardStep extends SelectMacroWizardStep
{
    /**
     * Creates a gadget select wizard step.
     * 
     * @param config the configuration of the wysiwyg
     * @param macroService the macro service to get information about the macros from the server side
     */
    public SelectGadgetWizardStep(Config config, MacroServiceAsync macroService)
    {
        super(config, macroService);
        // TODO: change the labels of stuff with gadget labels
        getValidationMessage().setText(Strings.INSTANCE.gadgetNoGadgetSelected());
    }

    @Override
    public String getStepTitle()
    {
        return Strings.INSTANCE.gadgetInsertDialogTitle();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden here to send a GadgetInstance to the edit gadget step, which will also contain the title of the
     * gadget, not only the content.
     * </p>
     * 
     * @see org.xwiki.gwt.wysiwyg.client.plugin.macro.ui.SelectMacroWizardStep#getResult()
     */
    @Override
    public Object getResult()
    {
        MacroCall superResult = (MacroCall) super.getResult();
        GadgetInstance gadgetInstance = new GadgetInstance();
        gadgetInstance.setMacroCall(superResult);
        // prefill the title of the gadget with the name of the macro
        gadgetInstance.setTitle("$services.localization.render('rendering.macro." + superResult.getName() + ".name')");
        return gadgetInstance;
    }
}
