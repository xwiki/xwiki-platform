package org.xwiki.extension.distribution.internal.job;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.distribution.internal.DistributionManager;
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

    @Override
    protected void start() throws Exception
    {
        // Waiting to start
        getStatus().ask(0);
    }
}
