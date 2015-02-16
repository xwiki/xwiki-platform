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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.xwiki.gwt.dom.client.JavaScriptObject;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.internal.DefaultConfig;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardListener;
import org.xwiki.gwt.user.client.ui.wizard.WizardStepMap;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroCall;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroService;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroServiceAsync;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroServiceAsyncCacheProxy;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.ui.EditMacroWizardStep;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.ui.SelectMacroWizardStep;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;

/**
 * The Gadget Wizard, along with its javascript API, allowing to create macros insert and edit wizards, to be used as
 * gadget wizards. The javascript API allows this wizard to be used from javascript outside the WYSiWYG (e.g. the
 * dashboard).
 * 
 * @version $Id$
 * @since 3.0M3
 */
public class GadgetWizardApi implements WizardListener
{
    /**
     * The name of the edit gadget wizard step.
     */
    private static final String EDIT_STEP_NAME = "edit";

    /**
     * The name of the select gadget wizard step.
     */
    private static final String SELECT_STEP_NAME = "select";

    /**
     * The magic wand used to start the potion to create a macro call from user input.
     */
    private Wizard insertWizard;

    /**
     * The magic wand used to transform the macros in other things.
     */
    private Wizard editWizard;

    /**
     * The object used to configure this wizard.
     */
    private final Config config;

    /**
     * The macro service used to retrieve macro descriptors.
     */
    private final MacroServiceAsync macroService;

    /**
     * The javascript callback called when the insert is done.
     */
    private JavaScriptObject insertCallback;

    /**
     * The javascript callback called when the edit is done.
     */
    private JavaScriptObject editCallback;

    /**
     * Creates a new gadget wizard.
     *
     * @deprecated: Since 7.0M2. Use {@link #GadgetWizardApi(JavaScriptObject)} instead
     */
    @Deprecated
    public GadgetWizardApi()
    {
        this(JavaScriptObject.fromJson("{syntax:'xwiki/2.0'}"));
    }

    /**
     * Creates a new gadget wizard.
     *
     * @param jsConfig the {@link JavaScriptObject} used to configure the newly created gadget
     */
    public GadgetWizardApi(JavaScriptObject jsConfig)
    {
        this.config = new DefaultConfig(jsConfig);
        this.macroService = new MacroServiceAsyncCacheProxy((MacroServiceAsync) GWT.create(MacroService.class));
    }

    /**
     * Starts the insert wizard, which will guide the user into choosing a macro and defining its parameters.
     */
    public void insert()
    {
        // Compute the list of macros inserted in the edited document.
        List<String> usedMacroIds = new ArrayList<String>();
        // Cast the spell.
        getInsertWizard().start(SELECT_STEP_NAME, usedMacroIds);
    }

    /**
     * Sets the native js insert callback to be called when the insert wizard is done.
     * 
     * @param insertHandler the native js callback to call when the insert wizard is finished successfully.
     */
    public void setInsertCallback(JavaScriptObject insertHandler)
    {
        this.insertCallback = insertHandler;
    }

    /**
     * Handles the call of the js callback when the insert is done.
     * 
     * @param macroCall the macro call used as a content of this gadget
     * @param gadgetTitle the title of the gadget
     */
    protected native void onInsertDone(String macroCall, String gadgetTitle)
    /*-{
        var insertCallback = this.@org.xwiki.gwt.wysiwyg.client.gadget.GadgetWizardApi::insertCallback;
        if (typeof insertCallback == 'function') {
            var result = {'title' : gadgetTitle, 'content' : macroCall};
            insertCallback(result);
        }
    }-*/;

    /**
     * Starts the edit wizard, to edit the specified macro call.
     * 
     * @param macroCall the macro call to edit, passed as a html comment from the annotated syntax.
     * @param title the title of the gadget to edit
     */
    public void edit(String macroCall, String title)
    {
        GadgetInstance gadgetInstance = new GadgetInstance();
        gadgetInstance.setMacroCall(new MacroCall(macroCall));
        gadgetInstance.setTitle(title);
        getEditWizard().start(EDIT_STEP_NAME, gadgetInstance);
    }

    /**
     * Sets the native js edit callback to be called when the edit wizard is done.
     * 
     * @param editHandler the native js callback to call when the edit wizard is finished successfully.
     */
    public void setEditCallback(JavaScriptObject editHandler)
    {
        this.editCallback = editHandler;
    }

    /**
     * Handles the call of the js callback when the edit is done.
     * 
     * @param macroCall the new macro call used as a content of the edited gadget
     * @param gadgetTitle the title of the gadget
     */
    protected native void onEditDone(String macroCall, String gadgetTitle)
    /*-{
        var editCallback = this.@org.xwiki.gwt.wysiwyg.client.gadget.GadgetWizardApi::editCallback;
        if (typeof editCallback == 'function') {
            var result = {'title' : gadgetTitle, 'content' : macroCall};
            editCallback(result);
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see WizardListener#onCancel(Wizard)
     */
    public void onCancel(Wizard sender)
    {
        // nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see WizardListener#onFinish(Wizard, Object)
     */
    public void onFinish(Wizard sender, Object result)
    {
        if (sender == getInsertWizard()) {
            GadgetInstance gadgetResult = (GadgetInstance) result;
            this.onInsertDone(gadgetResult.getMacroCall().toString(), gadgetResult.getTitle());
        }
        if (sender == getEditWizard()) {
            GadgetInstance gadgetResult = (GadgetInstance) result;
            this.onEditDone(gadgetResult.getMacroCall().toString(), gadgetResult.getTitle());
        }
    }

    /**
     * @return {@link #insertWizard}
     */
    private Wizard getInsertWizard()
    {
        if (insertWizard == null) {
            // create a gadgets select step
            SelectMacroWizardStep selectStep = new SelectGadgetWizardStep(config, macroService);
            selectStep.setNextStep(EDIT_STEP_NAME);
            selectStep.setValidDirections(EnumSet.of(NavigationDirection.NEXT));
            selectStep.setDirectionName(NavigationDirection.NEXT, Strings.INSTANCE.select());

            // and a gadgets parameters edit step
            EditMacroWizardStep editStep = new EditGadgetWizardStep(config, macroService);
            editStep.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.gadgetInsertActionLabel());
            editStep.setValidDirections(EnumSet.of(NavigationDirection.PREVIOUS, NavigationDirection.FINISH));

            // ... and put them all together in a wizard
            WizardStepMap insertSteps = new WizardStepMap();
            insertSteps.put(SELECT_STEP_NAME, selectStep);
            insertSteps.put(EDIT_STEP_NAME, editStep);

            insertWizard =
                new Wizard(Strings.INSTANCE.gadgetInsertDialogCaption(), new Image(Images.INSTANCE.macroInsert()));
            insertWizard.setProvider(insertSteps);
            insertWizard.addWizardListener(this);
        }
        return insertWizard;
    }

    /**
     * @return {@link #editWizard}
     */
    private Wizard getEditWizard()
    {
        if (editWizard == null) {
            EditMacroWizardStep editStep = new EditGadgetWizardStep(config, macroService);
            editStep.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.gadgetInsertActionLabel());
            editStep.setValidDirections(EnumSet.of(NavigationDirection.FINISH));

            WizardStepMap editSteps = new WizardStepMap();
            editSteps.put(EDIT_STEP_NAME, editStep);

            editWizard = new Wizard(Strings.INSTANCE.gadgetEditDialogCaption(), new Image(Images.INSTANCE.macroEdit()));
            editWizard.setProvider(editSteps);
            editWizard.addWizardListener(this);
        }

        return editWizard;
    }

    /**
     * Destroy this wizard.
     */
    public void destroy()
    {
        if (insertWizard != null) {
            insertWizard.removeWizardListener(this);
            insertWizard.onDirection(NavigationDirection.CANCEL);
            insertWizard = null;
        }

        if (editWizard != null) {
            editWizard.removeWizardListener(this);
            editWizard.onDirection(NavigationDirection.CANCEL);
            editWizard = null;
        }
    }

    /**
     * Publishes the JavaScript API that can be used to create and control {@link GadgetWizardApi}s.
     */
    public static native void publish()
    /*-{
        $wnd.XWiki = $wnd.XWiki || {}

        // Attach the Gadget wizard
        $wnd.XWiki.GadgetWizard = function(config) {
            // Make sure we always have a config passed, even if an empty/default one, for backwards compatibility.
            if (typeof config != 'object') {
                config = { syntax : 'xwiki/2.0' };
            }
            this.instance = @org.xwiki.gwt.wysiwyg.client.gadget.GadgetWizardApi::new(Lorg/xwiki/gwt/dom/client/JavaScriptObject;)(config);
        }
        // attach the add function, which will start the insert wizard
        $wnd.XWiki.GadgetWizard.prototype.add = function(onComplete) {
            this.instance.@org.xwiki.gwt.wysiwyg.client.gadget.GadgetWizardApi::setInsertCallback(Lorg/xwiki/gwt/dom/client/JavaScriptObject;)(onComplete);
            this.instance.@org.xwiki.gwt.wysiwyg.client.gadget.GadgetWizardApi::insert()();
        }

        // attach the edit function, which will start the edit wizard
        $wnd.XWiki.GadgetWizard.prototype.edit = function(macroCall, macroTitle, onComplete) {
            this.instance.@org.xwiki.gwt.wysiwyg.client.gadget.GadgetWizardApi::setEditCallback(Lorg/xwiki/gwt/dom/client/JavaScriptObject;)(onComplete);
            this.instance.@org.xwiki.gwt.wysiwyg.client.gadget.GadgetWizardApi::edit(Ljava/lang/String;Ljava/lang/String;)(macroCall, macroTitle);
        }        
    }-*/;
}
