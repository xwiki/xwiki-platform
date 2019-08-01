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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.DistributionManager.DistributionState;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;

/**
 * @version $Id$
 * @since 4.2M3
 */
public class DistributionJobStatus extends DefaultJobStatus<DistributionRequest>
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

    private final transient Map<String, Object> properties = new ConcurrentHashMap<>();

    public DistributionJobStatus(JobStatus status, ObservationManager observationManager, LoggerManager loggerManager)
    {
        super(status.getJobType(), ObjectUtils.cloneIfPossible((DistributionRequest) status.getRequest()), null,
            observationManager, loggerManager);

        if (status instanceof DistributionJobStatus) {
            DistributionJobStatus distributionJobStatus = (DistributionJobStatus) status;

            this.previousDistributionExtension = distributionJobStatus.previousDistributionExtension;
            this.previousDistributionExtensionUi = distributionJobStatus.previousDistributionExtensionUi;
            this.distributionExtension = distributionJobStatus.distributionExtension;
            this.distributionExtensionUi = distributionJobStatus.distributionExtensionUi;
            this.stepList = distributionJobStatus.stepList != null ? new ArrayList<>(distributionJobStatus.stepList)
                : new ArrayList<>();
        }
    }

    public DistributionJobStatus(DistributionRequest request, ObservationManager observationManager,
        LoggerManager loggerManager, List<DistributionStep> steps)
    {
        super(DefaultDistributionJob.HINT, request, null, observationManager, loggerManager);

        this.stepList = steps;
    }

    public List<DistributionStep> getSteps()
    {
        return this.stepList != null ? this.stepList : Collections.<DistributionStep>emptyList();
    }

    public DistributionStep getStep(String stepId)
    {
        for (DistributionStep step : getSteps()) {
            if (StringUtils.equals(step.getId(), stepId)) {
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

    public static DistributionState getDistributionState(ExtensionId previousExtensionId,
        ExtensionId distributionExtensionId)
    {
        DistributionState distributionState;

        if (distributionExtensionId != null) {
            if (previousExtensionId == null) {
                distributionState = DistributionState.NEW;
            } else {
                if (previousExtensionId.equals(distributionExtensionId)) {
                    distributionState = DistributionState.SAME;
                } else if (!distributionExtensionId.getId().equals(previousExtensionId.getId())) {
                    distributionState = DistributionState.DIFFERENT;
                } else {
                    int diff = distributionExtensionId.getVersion().compareTo(previousExtensionId.getVersion());
                    if (diff > 0) {
                        distributionState = DistributionState.UPGRADE;
                    } else {
                        distributionState = DistributionState.DOWNGRADE;
                    }
                }
            }
        } else {
            distributionState = DistributionState.NONE;
        }

        return distributionState;
    }

    public DistributionState getDistributionState()
    {
        return getDistributionState(getPreviousDistributionExtension(), getDistributionExtension());
    }

    /**
     * @since 11.7RC1
     * @since 11.3.3
     * @since 10.11.10
     */
    void setProperty(String key, Object value)
    {
        this.properties.put(key, value);
    }

    /**
     * @since 11.7RC1
     * @since 11.3.3
     * @since 10.11.10
     */
    Object getproperty(String key)
    {
        return this.properties.get(key);
    }
}
