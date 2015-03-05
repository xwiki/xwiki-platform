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
package org.xwiki.platform.wiki.creationjob.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.job.internal.AbstractJob;
import org.xwiki.job.internal.DefaultJobStatus;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.platform.wiki.creationjob.WikiCreationStep;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;

/**
 * Job that create a wiki and execute the WikiCreationSteps.
 *
 * @version $Id$
 * @since 7.0M2
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(WikiCreationJob.JOB_TYPE)
public class WikiCreationJob extends AbstractJob<WikiCreationRequest, DefaultJobStatus<WikiCreationRequest>>
{
    /**
     * The prefix put behind all job ids.
     */
    public static final String JOB_ID_PREFIX = "wikicreation";

    /**
     * The job type.
     */
    public static final String JOB_TYPE = "wikicreationjob";

    @Override
    protected void runInternal() throws Exception
    {
        try {
            List<WikiCreationStep> wikiCreationStepList =
                    componentManager.getInstanceList(WikiCreationStep.class);
            // Some extra steps needs to be executed AFTER some others, so we have introduce a getOrder() method in the
            // interface. We use this method to sort the list of extra steps by this order.
            Collections.sort(wikiCreationStepList, new Comparator<WikiCreationStep>()
            {
                @Override
                public int compare(WikiCreationStep o1, WikiCreationStep o2)
                {
                    return o1.getOrder() - o2.getOrder();
                }
            });
            // Now we can execute these extra steps
            this.notifyPushLevelProgress(wikiCreationStepList.size());
            for (WikiCreationStep step : wikiCreationStepList) {
                step.execute(request);
                this.notifyStepPropress();
            }
            this.notifyPopLevelProgress();
        } catch (WikiCreationException | ComponentLookupException e) {
            throw new WikiCreationException(
                String.format("Failed to execute creation steps on the wiki [%s].", request.getWikiId()), e);
        }
    }

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }
}
