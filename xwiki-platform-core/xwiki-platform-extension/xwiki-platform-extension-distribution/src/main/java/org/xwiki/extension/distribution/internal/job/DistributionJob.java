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

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.distribution.internal.job.DistributionJobStatus.UpdateState;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.internal.AbstractJobStatus;

@Component
@Named("distribution")
public class DistributionJob extends AbstractJob<DistributionRequest>
{
    @Inject
    private DistributionManager distributionManager;

    @Override
    public String getType()
    {
        return "distribution";
    }

    @Override
    protected AbstractJobStatus<DistributionRequest> createNewStatus(DistributionRequest request)
    {
        DistributionJobStatus status = new DistributionJobStatus(request, observationManager, loggerManager);

        status.setDistributionExtension(this.distributionManager.getDistributionExtension().getId());
        status.setDistributionExtensionUi(this.distributionManager.getUIExtensionId());

        return status;
    }

    protected DistributionJobStatus getDistributionJobStatus()
    {
        return (DistributionJobStatus) getStatus();
    }

    @Override
    protected void start() throws Exception
    {
        DistributionQuestion question = new DistributionQuestion();

        // Waiting to start
        getStatus().ask(question);

        if (question.isSave()) {
            switch (question.getAction()) {
                case CANCEL:
                    getDistributionJobStatus().setUpdateState(UpdateState.CANCELED);
                    break;
                case CONTINUE:
                    getDistributionJobStatus().setUpdateState(UpdateState.COMPLETED);
                    break;
                default:
                    break;
            }
        }
    }
}
