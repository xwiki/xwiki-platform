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
package com.xpn.xwiki.wysiwyg.client.plugin.link.ui;

import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.Wizard;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStep;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStepProvider;

/**
 * The link wizard, used to configure link parameters in a {@link com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig}
 * object, in successive steps. This class extends the {@link Wizard} class by encapsulating {@link WizardStepProvider}
 * behavior specific to links.
 * 
 * @version $Id$
 */
public class LinkWizard extends Wizard implements WizardStepProvider
{
    /**
     * Map with the instantiated steps to return. Will be lazily initialized upon request.
     */
    private Map<String, WizardStep> stepsMap = new HashMap<String, WizardStep>();

    /**
     * Default constructor.
     */
    public LinkWizard()
    {
        super(Strings.INSTANCE.link(), Images.INSTANCE.link().createImage());
        this.setProvider(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WizardStepProvider#getStep(String)
     */
    public WizardStep getStep(String name)
    {
        WizardStep existingStep = stepsMap.get(name);
        if (existingStep == null) {
            if ("webpage".equals(name)) {
                existingStep = new WebPageLinkWizardStep();
            }
            if ("email".equals(name)) {
                existingStep = new EmailAddressLinkWizardStep();
            }
            if ("wikipage".equals(name)) {
                existingStep = new SelectorWizardStep();
            }
            if ("wikipageconfig".equals(name)) {
                existingStep = new WikiLinkConfigWizardStep();
            }
            // if something has been created, add it in the map
            if (existingStep != null) {
                stepsMap.put(name, existingStep);
            }
        }
        // return the found or newly created step
        return existingStep;
    }
}
