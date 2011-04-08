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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardListener;
import org.xwiki.gwt.user.client.ui.wizard.WizardStepMap;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.ui.EditMacroWizardStep;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.ui.SelectMacroWizardStep;

import com.google.gwt.user.client.ui.Image;

/**
 * A wizard that can be used to cast macro spells on a rich text area.
 * 
 * @version $Id$
 */
public class MacroWizard implements WizardListener
{
    /**
     * Rich text area command for inserting a macro in place of the current selection.
     */
    public static final Command INSERT = new Command("macroInsert");

    /**
     * The name of the edit macro wizard step.
     */
    private static final String EDIT_STEP_NAME = "edit";

    /**
     * The name of the select macro wizard step.
     */
    private static final String SELECT_STEP_NAME = "select";

    /**
     * The magic wand used to cast the <em>edit macro</em> spell on the underlying rich text area, allowing the user to
     * edit the parameters and the content of the currently selected macro.
     */
    private Wizard editWizard;

    /**
     * The magic wand used to cast the <em>insert macro</em> spell on the underlying rich text area, allowing the user
     * to insert a macro in place of the current selection or at the caret position.
     */
    private Wizard insertWizard;

    /**
     * The object used to get information about the displayed macros and to access the rich text area on which the
     * spells are cast.
     */
    private final MacroDisplayer displayer;

    /**
     * The object used to configure this wizard.
     */
    private final Config config;

    /**
     * The macro service used to retrieve macro descriptors.
     */
    private final MacroServiceAsync macroService;

    /**
     * Creates a new macro wizard.
     * 
     * @param displayer the object used to get information about the displayed macros and to access the rich text area
     *            on which the spells are cast
     * @param config the object used to configure this wizard
     * @param macroService the macro service used to retrieve macro descriptors
     */
    public MacroWizard(MacroDisplayer displayer, Config config, MacroServiceAsync macroService)
    {
        this.displayer = displayer;
        this.config = config;
        this.macroService = macroService;
    }

    /**
     * Casts the <em>edit macro</em> spell on the underlying rich text area, allowing the user to edit the parameters
     * and the content of the currently selected macro.
     */
    public void edit()
    {
        getEditWizard().start(EDIT_STEP_NAME,
            new MacroCall(displayer.getTextArea().getCommandManager().getStringValue(INSERT)));
    }

    /**
     * Casts the <em>insert macro</em> spell on the underlying rich text area, allowing the user to insert a new macro
     * in place of the current selection or at the current caret position.
     */
    public void insert()
    {
        // Compute the list of macros inserted in the edited document.
        List<String> usedMacroIds = new ArrayList<String>();
        Element root = (Element) displayer.getTextArea().getDocument().getBody().cast();
        for (Element container : displayer.getMacroContainers(root)) {
            usedMacroIds.add(new MacroCall(displayer.getSerializedMacroCall(container)).getName());
        }
        // Cast the spell.
        getInsertWizard().start(SELECT_STEP_NAME, usedMacroIds);
    }

    /**
     * Casts the <em>insert macro</em> spell on the underlying rich text area, allowing the user to insert the specified
     * macro in place of the current selection or at the current caret position.
     * 
     * @param macroId a macro identifier
     */
    public void insert(String macroId)
    {
        MacroCall macroCall = new MacroCall();
        macroCall.setName(macroId);
        getInsertWizard().start(EDIT_STEP_NAME, macroCall);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WizardListener#onCancel(Wizard)
     */
    public void onCancel(Wizard sender)
    {
        displayer.getTextArea().setFocus(true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WizardListener#onFinish(Wizard, Object)
     */
    public void onFinish(Wizard sender, Object result)
    {
        displayer.getTextArea().setFocus(true);
        displayer.getTextArea().getCommandManager().execute(INSERT, ((MacroCall) result).toString());
    }

    /**
     * @return {@link #editWizard}
     */
    private Wizard getEditWizard()
    {
        if (editWizard == null) {
            EditMacroWizardStep editStep = new EditMacroWizardStep(config, macroService);
            editStep.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.apply());
            editStep.setValidDirections(EnumSet.of(NavigationDirection.FINISH));

            WizardStepMap editSteps = new WizardStepMap();
            editSteps.put(EDIT_STEP_NAME, editStep);

            editWizard = new Wizard(Strings.INSTANCE.macroEditDialogCaption(), new Image(Images.INSTANCE.macroEdit()));
            editWizard.setProvider(editSteps);
            editWizard.addWizardListener(this);
        }
        return editWizard;
    }

    /**
     * @return {@link #insertWizard}
     */
    private Wizard getInsertWizard()
    {
        if (insertWizard == null) {
            SelectMacroWizardStep selectStep = new SelectMacroWizardStep(config, macroService);
            selectStep.setNextStep(EDIT_STEP_NAME);
            selectStep.setValidDirections(EnumSet.of(NavigationDirection.NEXT));
            selectStep.setDirectionName(NavigationDirection.NEXT, Strings.INSTANCE.select());

            EditMacroWizardStep editStep = new EditMacroWizardStep(config, macroService);
            editStep.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.macroInsertActionLabel());
            editStep.setValidDirections(EnumSet.of(NavigationDirection.PREVIOUS, NavigationDirection.FINISH));

            WizardStepMap insertSteps = new WizardStepMap();
            insertSteps.put(SELECT_STEP_NAME, selectStep);
            insertSteps.put(EDIT_STEP_NAME, editStep);

            insertWizard =
                new Wizard(Strings.INSTANCE.macroInsertDialogCaption(), new Image(Images.INSTANCE.macroInsert()));
            insertWizard.setProvider(insertSteps);
            insertWizard.addWizardListener(this);
        }
        return insertWizard;
    }

    /**
     * Destroy this wizard.
     */
    public void destroy()
    {
        if (editWizard != null) {
            editWizard.removeWizardListener(this);
            editWizard.onDirection(NavigationDirection.CANCEL);
            editWizard = null;
        }

        if (insertWizard != null) {
            insertWizard.removeWizardListener(this);
            insertWizard.onDirection(NavigationDirection.CANCEL);
            insertWizard = null;
        }
    }
}
