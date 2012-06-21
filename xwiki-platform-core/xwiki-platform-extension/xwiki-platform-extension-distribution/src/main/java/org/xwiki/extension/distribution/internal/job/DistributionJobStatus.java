package org.xwiki.extension.distribution.internal.job;

import org.xwiki.extension.ExtensionId;
import org.xwiki.job.internal.DefaultJobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;

public class DistributionJobStatus extends DefaultJobStatus<DistributionRequest>
{
    public enum UpdateState
    {
        NONE,
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
