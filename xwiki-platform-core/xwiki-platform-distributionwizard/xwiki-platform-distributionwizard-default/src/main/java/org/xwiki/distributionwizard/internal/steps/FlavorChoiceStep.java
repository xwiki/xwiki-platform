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
import org.xwiki.distributionwizard.DistributionWizardException;
import org.xwiki.distributionwizard.DistributionWizardUIDefinition;
import org.xwiki.distributionwizard.internal.FlavorHelper;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.ResolveException;

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
    private FlavorHelper flavorHelper;

    @Inject
    private ExtensionManager extensionManager;

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
    public boolean isStepDone()
    {
        return flavorHelper.isFlavorInstalled() || flavorHelper.getSelectedFlavor().isPresent();
    }

    @Override
    protected DistributionWizardUIDefinition createUIDefinition()
    {
        return renderTemplate("flavorchoicestep.vm");
    }

    @Override
    public boolean needsInput()
    {
        return true;
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
                this.flavorHelper.selectFlavor(flavorExtension);
            } catch (ResolveException e) {
                throw new DistributionWizardException(
                    String.format("Error while resolving extension for the selected flavor: [%s]", selectedFlavor), e);
            }

            return true;
        }
        return false;
    }
}
