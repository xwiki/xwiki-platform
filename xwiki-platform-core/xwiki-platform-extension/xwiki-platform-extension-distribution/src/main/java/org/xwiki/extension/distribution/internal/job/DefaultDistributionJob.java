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
package org.xwiki.extension.distribution.internal.job;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.job.step.CleanExtensionsDistributionStep;
import org.xwiki.extension.distribution.internal.job.step.DefaultUIDistributionStep;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep;
import org.xwiki.extension.distribution.internal.job.step.FirstAdminUserStep;
import org.xwiki.extension.distribution.internal.job.step.FlavorDistributionStep;
import org.xwiki.extension.distribution.internal.job.step.OutdatedExtensionsDistributionStep;
import org.xwiki.extension.distribution.internal.job.step.WikisDefaultUIDistributionStep;

/**
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named(DefaultDistributionJob.HINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultDistributionJob extends AbstractDistributionJob<DistributionRequest>
{
    /**
     * The role hint of the component.
     */
    public static final String HINT = "distribution";

    @Override
    protected List<DistributionStep> createSteps()
    {
        List<DistributionStep> steps = new ArrayList<>();

        boolean isMainWiki = isMainWiki();

        // Create admin user if needed
        if (isMainWiki) {
            try {
                steps.add(
                    this.componentManager.<DistributionStep>getInstance(DistributionStep.class, FirstAdminUserStep.ID));
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to get first admin step instance", e);
            }
        }

        // Install/upgrade main wiki UI
        addDefaultUIStep(steps, isMainWiki);

        // Clean leftovers
        addCleanStep(steps);

        // Upgrade other wikis
        if (isMainWiki) {
            ExtensionId wikiUI = this.distributionManager.getWikiUIExtensionId();
            if (wikiUI != null && StringUtils.isNotBlank(wikiUI.getId())) {
                // ... but only if the wiki extension ID is defined
                try {
                    steps.add(this.componentManager.<DistributionStep>getInstance(DistributionStep.class,
                        WikisDefaultUIDistributionStep.ID));
                } catch (ComponentLookupException e) {
                    this.logger.error("Failed to get all in one default UI step instance", e);
                }
            } else {
                // TODO: Display the wikis flavor step
            }
        }

        // Upgrade outdated extensions
        try {
            steps.add(this.componentManager.<DistributionStep>getInstance(DistributionStep.class,
                OutdatedExtensionsDistributionStep.ID));
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to get outdated extensions step instance", e);
        }

        return steps;
    }

    private void addCleanStep(List<DistributionStep> steps)
    {
        try {
            steps.add(this.componentManager.<DistributionStep>getInstance(DistributionStep.class,
                CleanExtensionsDistributionStep.ID));
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to get a clean extensions step instance", e);
        }
    }

    private void addDefaultUIStep(List<DistributionStep> steps, boolean isMainWiki)
    {
        ExtensionId ui;
        if (isMainWiki) {
            ui = getUIExtensionId();
        } else {
            ui = this.distributionManager.getWikiUIExtensionId();
        }

        if (ui != null && StringUtils.isNotBlank(ui.getId())) {
            // ... but only if the main extension ID is defined
            try {
                steps.add(this.componentManager.<DistributionStep>getInstance(DistributionStep.class,
                    DefaultUIDistributionStep.ID));
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to get default UI step instance", e);
            }
        } else {
            // Display the flavor step
            try {
                steps.add(this.componentManager.<DistributionStep>getInstance(DistributionStep.class,
                    FlavorDistributionStep.ID));
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to get flavor step instance", e);
            }
        }
    }

    @Override
    public DistributionJobStatus getPreviousStatus()
    {
        if (isMainWiki()) {
            return this.distributionManager.getPreviousFarmJobStatus();
        } else {
            return this.distributionManager.getPreviousWikiJobStatus(getRequest().getWiki());
        }
    }

    @Override
    public ExtensionId getUIExtensionId()
    {
        if (isMainWiki()) {
            return this.distributionManager.getMainUIExtensionId();
        } else {
            return this.distributionManager.getWikiUIExtensionId();
        }
    }
}
