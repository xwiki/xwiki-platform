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

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.wizard.AbstractInteractiveWizardStep;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroServiceAsync;

import com.google.gwt.user.client.ui.FlowPanel;

/**
 * An abstract wizard step to serve as a base class for all macro wizard steps.
 * 
 * @version $Id$
 */
public abstract class AbstractMacroWizardStep extends AbstractInteractiveWizardStep
{
    /**
     * The object used to configure the wizard step.
     */
    private final Config config;

    /**
     * The macro service used to retrieve macro descriptors.
     */
    private final MacroServiceAsync macroService;

    /**
     * Creates a new macro wizard step.
     * 
     * @param config the object used to configure the newly created wizard step
     * @param macroService the macro service used to retrieve macro descriptors
     */
    public AbstractMacroWizardStep(Config config, MacroServiceAsync macroService)
    {
        super();

        this.config = config;
        this.macroService = macroService;
    }

    /**
     * Creates a new macro wizard step that uses the given panel to hold its widgets.
     * 
     * @param config the object used to configure the newly created wizard step
     * @param macroService the macro service used to retrieve macro descriptors
     * @param panel the panel where this wizard step will add its widgets
     */
    public AbstractMacroWizardStep(Config config, MacroServiceAsync macroService, FlowPanel panel)
    {
        super(panel);

        this.config = config;
        this.macroService = macroService;
    }

    /**
     * @return the object used to configure the wizard step
     */
    public Config getConfig()
    {
        return config;
    }

    /**
     * @return the macro service used to retrieve macro descriptors
     */
    public MacroServiceAsync getMacroService()
    {
        return macroService;
    }
}
