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

import java.util.List;

import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep;
import org.xwiki.job.internal.DefaultJobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;

/**
 * @version $Id$
 * @since 4.2M3
 */
public class DistributionJobStatus<R extends DistributionRequest> extends DefaultJobStatus<DistributionRequest>
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 2L;

    /**
     * Note that this field should not be renamed since it's expected by job status parser.
     */
    private ExtensionId previousDistributionExtension;

    /**
     * Note that this field should not be renamed since it's expected by job status parser.
     */
    private ExtensionId previousDistributionExtensionUi;

    /**
     * Note that this field should not be renamed since it's expected by job status parser.
     */
    private ExtensionId distributionExtension;

    /**
     * Note that this field should not be renamed since it's expected by job status parser.
     */
    private ExtensionId distributionExtensionUi;

    private List<DistributionStep> stepList;

    private int currentStateIndex;

    public DistributionJobStatus(DistributionJobStatus<R> status, ObservationManager observationManager,
        LoggerManager loggerManager)
    {
        super(status.getRequest(), observationManager, loggerManager, false);

        this.previousDistributionExtension = status.previousDistributionExtension;
        this.previousDistributionExtensionUi = status.previousDistributionExtensionUi;
        this.distributionExtension = status.distributionExtension;
        this.distributionExtensionUi = status.distributionExtensionUi;
        this.stepList = status.stepList;
    }

    public DistributionJobStatus(R request, ObservationManager observationManager, LoggerManager loggerManager,
        List<DistributionStep> steps)
    {
        super(request, observationManager, loggerManager, false);

        this.stepList = steps;
    }

    public List<DistributionStep> getSteps()
    {
        return this.stepList;
    }

    public DistributionStep getStep(String stepId)
    {
        for (DistributionStep step : getSteps()) {
            if (step.getId().equals(stepId)) {
                return step;
            }
        }

        return null;
    }

    public int getCurrentStateIndex()
    {
        return this.currentStateIndex;
    }

    public DistributionStep getCurrentStep()
    {
        return getCurrentStateIndex() < getSteps().size() ? getSteps().get(getCurrentStateIndex()) : null;
    }

    public void setCurrentStateIndex(int currentStateIndex)
    {
        this.currentStateIndex = currentStateIndex;
    }

    // Distribution informations

    public ExtensionId getPreviousDistributionExtension()
    {
        return this.previousDistributionExtension;
    }

    public void setPreviousDistributionExtension(ExtensionId previousDistributionExtension)
    {
        this.previousDistributionExtension = previousDistributionExtension;
    }

    public ExtensionId getPreviousDistributionExtensionUI()
    {
        return this.previousDistributionExtensionUi;
    }

    public void setPreviousDistributionExtensionUI(ExtensionId previousDistributionExtensionUI)
    {
        this.previousDistributionExtensionUi = previousDistributionExtensionUI;
    }

    public ExtensionId getDistributionExtension()
    {
        return this.distributionExtension;
    }

    public void setDistributionExtension(ExtensionId distributionExtension)
    {
        this.distributionExtension = distributionExtension;
    }

    public ExtensionId getDistributionExtensionUI()
    {
        return this.distributionExtensionUi;
    }

    public void setDistributionExtensionUI(ExtensionId distributionExtensionUI)
    {
        this.distributionExtensionUi = distributionExtensionUI;
    }
}
