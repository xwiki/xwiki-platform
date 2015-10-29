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

package org.xwiki.gwt.wysiwyg.client.plugin.importer.ui;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.user.client.ui.wizard.WizardStepProvider;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.importer.ImportServiceAsync;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.ui.Image;

/**
 * Import wizard responsible for performing various content import operation into wysiwyg editor.
 * 
 * @version $Id$
 * @since 2.0.1
 */
public class ImportWizard extends Wizard implements WizardStepProvider
{
    /**
     * Enumeration of steps comprising the import wizard.
     * 
     * @version $Id$
     * @since 2.0.1
     */
    public enum ImportWizardStep
    {
        /**
         * Office file import wizard step.
         */
        OFFICE_FILE,

        /**
         * Office paste import wizard step.
         */
        OFFICE_PASTE
    };

    /**
     * Map with the instantiated steps to return. Will be lazily initialized upon request.
     */
    private Map<ImportWizardStep, WizardStep> stepsMap = new HashMap<ImportWizardStep, WizardStep>();

    /**
     * The object used to configure this wizard.
     */
    private final Config config;

    /**
     * The component used to clean content pasted from office documents and to import office documents.
     */
    private final ImportServiceAsync importService;

    /**
     * The service used to access the import attachments.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Instantiates the import wizard.
     * 
     * @param config the object used to configure this wizard
     * @param importService the component used to clean content pasted from office documents and to import office
     *            documents
     * @param wikiService the service used to access the import attachments
     */
    public ImportWizard(Config config, ImportServiceAsync importService, WikiServiceAsync wikiService)
    {
        super(Strings.INSTANCE.importWizardTitle(), new Image(Images.INSTANCE.importWizardIcon()));
        this.config = config;
        this.importService = importService;
        this.wikiService = wikiService;
        this.setProvider(this);
    }

    /**
     * {@inheritDoc}
     */
    public WizardStep getStep(String name)
    {
        ImportWizardStep requestedStep = parseStepName(name);
        WizardStep step = stepsMap.get(requestedStep);
        if (null == step) {
            switch (requestedStep) {
                case OFFICE_FILE:
                    step = new ImportOfficeFileWizardStep(config, wikiService, importService);
                    break;
                case OFFICE_PASTE:
                    step = new ImportOfficePasteWizardStep(importService);
                    break;
                default:
                    // nothing here, leave it null
                    break;
            }
            // if something has been created, add it in the map
            if (step != null) {
                stepsMap.put(requestedStep, step);
            }
        }
        // return the found or newly created step
        return step;
    }

    /**
     * Parses the specified step name in a {@link ImportWizardStep} value.
     * 
     * @param name the name of the step to parse
     * @return the {@link ImportWizardStep} {@code enum} value corresponding to the passed name, or {@code null} if no
     *         such value exists.
     */
    private ImportWizardStep parseStepName(String name)
    {
        // let's be careful about this
        ImportWizardStep requestedStep = null;
        try {
            requestedStep = ImportWizardStep.valueOf(name);
        } catch (IllegalArgumentException e) {
            // nothing, just leave it null if it cannot be found in the enum
        }
        return requestedStep;
    }
}
