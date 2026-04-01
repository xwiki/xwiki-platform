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
import org.xwiki.distributionwizard.DistributionWizardStep;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.platform.flavor.FlavorManager;
import org.xwiki.rendering.block.Block;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Component
@Singleton
@Named("UIDistributionStep")
public class UIDistributionStep extends AbstractStep
{
    @Inject
    private FlavorManager flavorManager;

    @Inject
    private DistributionManager distributionManager;

    @Override
    public String getTitle()
    {
        return "UI Distribution Choice";
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
        DistributionJob distributionJob = this.distributionManager.getCurrentDistributionJob();
        String wiki = distributionJob.getRequest().getWiki();
        Namespace namespace = wiki == null ? null : new Namespace("wiki", wiki);
        InstalledExtension flavor = this.flavorManager.getFlavorExtension(namespace);
        return  (flavor != null && flavor.isValid(namespace.toString()));
    }

    @Override
    public boolean handleAnswer(Map<String, Serializable> data) throws DistributionWizardException
    {
        return false;
    }
}
