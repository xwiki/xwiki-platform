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
import org.xwiki.extension.distribution.internal.job.step.WelcomeDistributionStep;
import org.xwiki.rendering.block.Block;

import jakarta.annotation.Priority;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Component
@Singleton
@Named("WelcomeStep")
@Priority(0)
public class WelcomeStep extends AbstractStep
{
    @Override
    public boolean isStepDone() throws DistributionWizardException
    {
        return false;
    }

    @Override
    public boolean startsOnDisplay()
    {
        return true;
    }

    @Override
    protected String getJobStepId()
    {
        return WelcomeDistributionStep.ID;
    }

    @Override
    public boolean isSkippable()
    {
        return true;
    }

    @Override
    public boolean isRedoable()
    {
        return true;
    }
}
