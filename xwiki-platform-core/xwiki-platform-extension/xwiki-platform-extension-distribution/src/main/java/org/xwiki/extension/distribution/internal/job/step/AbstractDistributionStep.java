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
package org.xwiki.extension.distribution.internal.job.step;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.extension.distribution.internal.job.DistributionJobStatus;
import org.xwiki.rendering.block.Block;
import org.xwiki.template.TemplateManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

public abstract class AbstractDistributionStep implements DistributionStep
{
    @Inject
    protected transient TemplateManager renderer;

    @Inject
    protected transient Provider<WikiDescriptorManager> wikiDescriptorManagerProvider;

    private final String stepId;

    protected DistributionJob distributionJob;

    private State state;

    public AbstractDistributionStep(String stepId)
    {
        this.stepId = stepId;
    }

    @Override
    public void initialize(DistributionJob distributionJob)
    {
        this.distributionJob = distributionJob;

        // Remember previous state
        DistributionJobStatus< ? > previousStatus = this.distributionJob.getPreviousStatus();

        if (previousStatus != null
            && previousStatus.getDistributionExtension().equals(
                this.distributionJob.getStatus().getDistributionExtension())) {
            DistributionStep previousStep = previousStatus.getStep(getId());

            if (previousStep != null) {
                setState(previousStep.getState());
            }
        }

        // Custom preparation

        if (getState() == null) {
            prepare();
        }
    }

    @Override
    public String getId()
    {
        return this.stepId;
    }

    @Override
    public State getState()
    {
        return this.state;
    }

    @Override
    public void setState(State stepState)
    {
        this.state = stepState;
    }

    protected String getWiki()
    {
        return this.distributionJob.getRequest().getWiki();
    }

    protected boolean isMainWiki()
    {
        return this.wikiDescriptorManagerProvider.get().getMainWikiId().equals(getWiki());
    }

    protected String getNamespace()
    {
        String wiki = getWiki();

        return wiki == null ? null : "wiki:" + getWiki();
    }

    protected String getTemplate()
    {
        return "distribution/" + getId() + ".wiki";
    }

    @Override
    public Block execute()
    {
        return this.renderer.executeNoException(getTemplate());
    }
}
