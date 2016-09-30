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
package org.xwiki.gwt.wysiwyg.client.plugin.importer;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardListener;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.importer.ui.ImportWizard;
import org.xwiki.gwt.wysiwyg.client.plugin.importer.ui.ImportWizard.ImportWizardStep;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

/**
 * Plugin responsible for importing external content into wysiwyg editor.
 * 
 * @version $Id$
 */
public class ImportPlugin extends AbstractPlugin implements WizardListener, ClickHandler
{
    /**
     * Import wizard.
     */
    private Wizard importWizard;

    /**
     * Import menu extension.
     */
    private ImportMenuExtension importMenuExtension;

    /**
     * The component used to clean content pasted from office documents and to import office documents.
     */
    private final ImportServiceAsync importService;

    /**
     * The service used to access the import attachments.
     */
    private final WikiServiceAsync wikiService;

    /**
     * The association between tool bar buttons and the wizard steps that are loaded when these buttons are clicked.
     */
    private final Map<PushButton, ImportWizardStep> wizardSteps = new HashMap<PushButton, ImportWizardStep>();

    /**
     * User interface extension for the editor tool bar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * Creates a new import plug-in that used the given import service.
     * 
     * @param importService the component used to clean content pasted from office documents and to import office
     *            documents
     * @param wikiService the component used to access the import attachments
     */
    public ImportPlugin(ImportServiceAsync importService, WikiServiceAsync wikiService)
    {
        this.importService = importService;
        this.wikiService = wikiService;
    }

    @Override
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        if (Boolean.valueOf(config.getParameter("cleanPaste", "true"))) {
            PasteManager pasteManager = GWT.create(PasteManager.class);
            saveRegistrations(pasteManager.initialize(textArea, importService));
        }

        this.importMenuExtension = new ImportMenuExtension(this);
        getUIExtensionList().add(importMenuExtension);

        addFeature("import:officefile", ImportWizardStep.OFFICE_FILE, Images.INSTANCE.importOfficeFileMenuEntryIcon(),
            Strings.INSTANCE.importOfficeFileMenuItemCaption());
        addFeature("paste", ImportWizardStep.OFFICE_PASTE, Images.INSTANCE.paste(), Strings.INSTANCE.paste());
        if (toolBarExtension.getFeatures().length > 0) {
            getUIExtensionList().add(toolBarExtension);
        }
    }

    /**
     * Creates a tool bar feature and adds it to the tool bar.
     * 
     * @param name the feature name
     * @param wizardStep the wizard step that is triggered by this feature
     * @param imageResource the image displayed on the tool bar
     * @param title the tool tip used on the tool bar button
     */
    private void addFeature(String name, ImportWizardStep wizardStep, ImageResource imageResource, String title)
    {
        if (getTextArea().getCommandManager().isSupported(Command.INSERT_HTML)) {
            PushButton button = new PushButton(new Image(imageResource));
            saveRegistration(button.addClickHandler(this));
            button.setTitle(title);
            toolBarExtension.addFeature(name, button);
            wizardSteps.put(button, wizardStep);
        }
    }

    /**
     * Method invoked by {@link ImportMenuExtension} when "Import -&amp; Office File" menu item is clicked.
     */
    public void onImportOfficeFile()
    {
        getImportWizard().start(ImportWizardStep.OFFICE_FILE.toString(), null);
    }

    @Override
    public void onCancel(Wizard sender)
    {
        getTextArea().setFocus(true);
    }

    @Override
    public void onFinish(Wizard sender, Object result)
    {
        getTextArea().setFocus(true);
        getTextArea().getCommandManager().execute(Command.INSERT_HTML, result.toString());
    }

    @Override
    public void destroy()
    {
        for (PushButton button : wizardSteps.keySet()) {
            button.removeFromParent();
        }
        wizardSteps.clear();

        toolBarExtension.clearFeatures();
        importMenuExtension.clearFeatures();

        super.destroy();
    }

    /**
     * Creates and returns the import wizard.
     * 
     * @return import wizard instance.
     */
    private Wizard getImportWizard()
    {
        if (null == importWizard) {
            importWizard = new ImportWizard(getConfig(), importService, wikiService);
            importWizard.addWizardListener(this);
        }
        return importWizard;
    }

    @Override
    public void onClick(ClickEvent event)
    {
        Widget sender = (Widget) event.getSource();
        ImportWizardStep wizardStep = wizardSteps.get(sender);
        if (wizardStep != null) {
            getImportWizard().start(wizardStep.toString(), null);
        }
    }
}
