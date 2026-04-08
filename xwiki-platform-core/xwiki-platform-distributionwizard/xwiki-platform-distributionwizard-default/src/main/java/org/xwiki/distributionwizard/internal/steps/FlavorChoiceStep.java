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
package org.xwiki.distributionwizard.internal.steps;

import java.io.Serializable;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.distributionwizard.DistributionWizardException;
import org.xwiki.distributionwizard.DistributionWizardUIDefinition;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.platform.flavor.FlavorManager;
import org.xwiki.skinx.RequiredSkinExtensionsRecorder;
import org.xwiki.template.TemplateManager;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Component
@Singleton
@Named("FlavorChoiceStep")
public class FlavorChoiceStep extends AbstractStep
{
    private static final String FLAVOR_KEY = "flavor";

    @Inject
    private FlavorManager flavorManager;

    @Inject
    private DistributionManager distributionManager;

    @Inject
    private ExtensionManager extensionManager;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private RequiredSkinExtensionsRecorder requiredSkinExtensionsRecorder;

    private DistributionWizardUIDefinition uiDefinition;

    @Override
    public String getTitle()
    {
        return "Flavor Choice";
    }

    @Override
    public int getIndex()
    {
        return 2;
    }

    @Override
    public boolean isHidden()
    {
        return false;
    }

    @Override
    public boolean isOptional()
    {
        return false;
    }

    @Override
    public boolean isStepDone()
    {
        // TODO: handle automatic flavor choice
        DistributionJob distributionJob = this.distributionManager.getCurrentDistributionJob();
        String wiki = distributionJob.getRequest().getWiki();
        Namespace namespace = new Namespace("wiki", wiki);
        InstalledExtension flavor = this.flavorManager.getFlavorExtension(namespace);
        return (flavor != null && flavor.isValid(namespace.toString()));
    }

    @Override
    public DistributionWizardUIDefinition getUIDefinition()
    {
        if (uiDefinition == null) {
            this.requiredSkinExtensionsRecorder.start();
            String html = this.templateManager.renderNoException("flavorchoicestep.vm");
            String requiredSkinExtension = this.requiredSkinExtensionsRecorder.stop();
            this.uiDefinition = new DistributionWizardUIDefinition(null, WEBJAR_NAME, html, requiredSkinExtension);
        }
        return uiDefinition;
    }

    @Override
    public boolean handleAnswer(Map<String, Serializable> data) throws DistributionWizardException
    {
        if (data.containsKey(FLAVOR_KEY)) {
            // FIXME: handle no flavor
            String selectedFlavor = (String) data.get(FLAVOR_KEY);
            // FIXME: would be cleaner and more robust with a regex and checks
            String[] splittedFlavor = selectedFlavor.split(":::");
            String flavorId = splittedFlavor[0];
            String flavorVersion = splittedFlavor[1];
            try {
                Extension flavorExtension =
                    this.extensionManager.resolveExtension(new ExtensionId(flavorId, flavorVersion));
                DistributionJob job = this.distributionManager.getCurrentDistributionJob();
                job.setProperty("flavor.selected", flavorExtension);
            } catch (ResolveException e) {
                throw new DistributionWizardException(
                    String.format("Error while resolving extension for the selected flavor: [%s]", selectedFlavor), e);
            }

            return true;
        }
        return false;
    }
}
