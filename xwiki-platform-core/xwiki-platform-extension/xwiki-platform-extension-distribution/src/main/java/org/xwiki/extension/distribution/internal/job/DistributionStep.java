package org.xwiki.extension.distribution.internal.job;

public interface DistributionStep
{
    void prepare();

    Object ask();

    DistributionStepStatus getStatus();
}
