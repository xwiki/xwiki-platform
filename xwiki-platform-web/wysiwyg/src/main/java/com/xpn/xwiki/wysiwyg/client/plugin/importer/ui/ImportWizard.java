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

package com.xpn.xwiki.wysiwyg.client.plugin.importer.ui;

import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.Wizard;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStep;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStepProvider;

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
    public static enum ImportWizardStep
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
     * The wysiwyg configuration.
     */
    private Config config;

    /**
     * Instantiates the import wizard.
     * 
     * @param config wysiwyg configuration.
     */
    public ImportWizard(Config config)
    {
        super(Strings.INSTANCE.importWizardTitle(), Images.INSTANCE.importWizardIcon().createImage());
        this.config = config;
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
                    step = new ImportOfficeFileWizardStep(this.config);
                    break;
                case OFFICE_PASTE:
                    step = new ImportOfficePasteWizardStep();
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
