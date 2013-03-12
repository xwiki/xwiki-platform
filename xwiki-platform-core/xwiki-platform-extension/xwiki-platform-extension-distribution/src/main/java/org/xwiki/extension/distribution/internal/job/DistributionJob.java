package org.xwiki.extension.distribution.internal.job;

import org.xwiki.extension.distribution.internal.job.step.DistributionStep;
import org.xwiki.job.Job;

public interface DistributionJob extends Job
{
    DistributionStep getCurrentStep();
}
