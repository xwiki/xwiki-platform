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

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardListener;
import org.xwiki.gwt.wysiwyg.client.plugin.importer.ui.ImportWizard;
import org.xwiki.gwt.wysiwyg.client.plugin.importer.ui.ImportWizard.ImportWizardStep;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;


/**
 * Plugin responsible for importing external content into wysiwyg editor.
 * 
 * @version $Id$
 */
public class ImportPlugin extends AbstractPlugin implements WizardListener
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

    /**
     * {@inheritDoc}
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);
        this.importMenuExtension = new ImportMenuExtension(this);
        getUIExtensionList().add(importMenuExtension);
    }

    /**
     * Method invoked by {@link ImportMenuExtension} when "Import -> Office File" menu item is clicked.
     */
    public void onImportOfficeFile()
    {
        getImportWizard().start(ImportWizardStep.OFFICE_FILE.toString(), null);
    }

    /**
     * Method invoked by {@link ImportMenuExtension} when "Import -> Office Content" menu item is clicked.
     */
    public void onImportOfficePaste()
    {
        getImportWizard().start(ImportWizardStep.OFFICE_PASTE.toString(), null);
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel(Wizard sender)
    {
        getTextArea().setFocus(true);
    }

    /**
     * {@inheritDoc}
     */
    public void onFinish(Wizard sender, Object result)
    {
        getTextArea().setFocus(true);
        getTextArea().getCommandManager().execute(Command.INSERT_HTML, result.toString());
    }

    /**
     * {@inheritDoc}
     */
    public void destroy()
    {
        importMenuExtension.destroy();
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
}
