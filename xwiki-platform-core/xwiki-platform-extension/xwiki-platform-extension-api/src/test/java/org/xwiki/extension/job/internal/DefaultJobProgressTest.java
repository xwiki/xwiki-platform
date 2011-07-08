package org.xwiki.extension.job.internal;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.extension.job.PopLevelProgressEvent;
import org.xwiki.extension.job.PushLevelProgressEvent;
import org.xwiki.extension.job.StepProgressEvent;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.AbstractComponentTestCase;

public class DefaultJobProgressTest extends AbstractComponentTestCase
{
    private ObservationManager observation;

    private DefaultJobProgress progress;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.observation = getComponentManager().lookup(ObservationManager.class);
        this.progress = new DefaultJobProgress("id");
        this.observation.addListener(this.progress);
    }

    @Test
    public void testProgressSteps()
    {
        Assert.assertEquals(0D, this.progress.getOffset());
        Assert.assertEquals(0, this.progress.getPercent());

        this.observation.notify(new PushLevelProgressEvent(4), null, null);

        Assert.assertEquals(0D, this.progress.getOffset());
        Assert.assertEquals(0, this.progress.getPercent());

        this.observation.notify(new StepProgressEvent(), null, null);

        Assert.assertEquals(0.25D, this.progress.getOffset());
        Assert.assertEquals(25, this.progress.getPercent());

        this.observation.notify(new PushLevelProgressEvent(2), null, null);

        Assert.assertEquals(0.25D, this.progress.getOffset());
        Assert.assertEquals(25, this.progress.getPercent());

        this.observation.notify(new StepProgressEvent(), null, null);

        Assert.assertEquals(0.375D, this.progress.getOffset());
        Assert.assertEquals(37, this.progress.getPercent());

        this.observation.notify(new PopLevelProgressEvent(), null, null);

        Assert.assertEquals(0.5D, this.progress.getOffset());
        Assert.assertEquals(50, this.progress.getPercent());
    }
}
