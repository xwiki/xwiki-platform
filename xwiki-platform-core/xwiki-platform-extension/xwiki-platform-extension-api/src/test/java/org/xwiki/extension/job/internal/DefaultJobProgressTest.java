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
package org.xwiki.extension.job.internal;

import org.junit.Test;
import org.xwiki.extension.job.event.status.PopLevelProgressEvent;
import org.xwiki.extension.job.event.status.PushLevelProgressEvent;
import org.xwiki.extension.job.event.status.StepProgressEvent;
import org.xwiki.extension.test.RepositoryUtil;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.AbstractComponentTestCase;

import junit.framework.Assert;

public class DefaultJobProgressTest extends AbstractComponentTestCase
{
    private ObservationManager observation;

    private DefaultJobProgress progress;

    private RepositoryUtil repositoryUtil;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        this.repositoryUtil =
            new RepositoryUtil(getClass().getSimpleName(), getConfigurationSource(), getComponentManager());
        this.repositoryUtil.setup();

        this.observation = getComponentManager().lookup(ObservationManager.class);
        this.progress = new DefaultJobProgress("id");
        this.observation.addListener(this.progress);
    }

    @Test
    public void testProgressSteps()
    {
        Assert.assertEquals(0D, this.progress.getOffset());
        Assert.assertEquals(0D, this.progress.getCurrentLevelOffset());

        this.observation.notify(new PushLevelProgressEvent(4), null, null);

        Assert.assertEquals(0D, this.progress.getOffset());
        Assert.assertEquals(0D, this.progress.getCurrentLevelOffset());

        this.observation.notify(new StepProgressEvent(), null, null);

        Assert.assertEquals(0.25D, this.progress.getOffset());
        Assert.assertEquals(0.25D, this.progress.getCurrentLevelOffset());

        this.observation.notify(new PushLevelProgressEvent(2), null, null);

        Assert.assertEquals(0.25D, this.progress.getOffset());
        Assert.assertEquals(0.0D, this.progress.getCurrentLevelOffset());

        this.observation.notify(new StepProgressEvent(), null, null);

        Assert.assertEquals(0.375D, this.progress.getOffset());
        Assert.assertEquals(0.5D, this.progress.getCurrentLevelOffset());

        this.observation.notify(new PopLevelProgressEvent(), null, null);

        Assert.assertEquals(0.5D, this.progress.getOffset());
        Assert.assertEquals(0.5D, this.progress.getCurrentLevelOffset());
    }
}
