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

import org.xwiki.extension.ExtensionId;
import org.xwiki.job.internal.DefaultJobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;

public class DistributionJobStatus extends DefaultJobStatus<DistributionRequest>
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    public enum UpdateState
    {
        CANCELED,
        COMPLETE
    }

    private UpdateState updateState;

    private ExtensionId distributionExtension;

    private ExtensionId distributionExtensionUi;

    public DistributionJobStatus(DistributionRequest request, ObservationManager observationManager,
        LoggerManager loggerManager)
    {
        super(request, observationManager, loggerManager);
    }
    
    public UpdateState getUpdateState()
    {
        return this.updateState;
    }
    
    public void setUpdateState(UpdateState updateState)
    {
        this.updateState = updateState;
    }

    public ExtensionId getDistributionExtension()
    {
        return this.distributionExtension;
    }

    public void setDistributionExtension(ExtensionId distributionExtension)
    {
        this.distributionExtension = distributionExtension;
    }

    public ExtensionId getDistributionExtensionUi()
    {
        return this.distributionExtensionUi;
    }

    public void setDistributionExtensionUi(ExtensionId distributionExtensionUi)
    {
        this.distributionExtensionUi = distributionExtensionUi;
    }
}
