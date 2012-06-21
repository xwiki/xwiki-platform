package org.xwiki.extension.distribution.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.extension.distribution.internal.job.DistributionJobStatus;

@Role
public interface DistributionManager
{
    enum DistributionState
    {
        // Nothing to do
        SAME,

        // No idea what do to
        NONE,

        // Probably something to do
        NEW,
        UPGRADE,
        DOWNGRADE,
        DIFFERENT
    }

    DistributionState getDistributionState();

    CoreExtension getDistributionExtension();

    ExtensionId getUIExtensionId();

    DistributionJobStatus getPreviousJobStatus();

    DistributionJob getJob();
}
