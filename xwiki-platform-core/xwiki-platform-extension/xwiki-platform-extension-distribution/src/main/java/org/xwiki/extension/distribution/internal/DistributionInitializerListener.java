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
package org.xwiki.extension.distribution.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.distribution.internal.DistributionManager.DistributionState;
import org.xwiki.extension.distribution.internal.job.DistributionJobStatus;
import org.xwiki.extension.distribution.internal.job.WikiDistributionJobStatus;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("DistributionInitializerListener")
public class DistributionInitializerListener implements EventListener
{
    /**
     * The list of events to listen to.
     */
    private static final List<Event> EVENTS = Arrays
        .<Event> asList(new ApplicationStartedEvent(), new WikiReadyEvent());

    /**
     * The component used to get information about the current distribution.
     */
    @Inject
    private DistributionManager distributionManager;

    /**
     * The object used to log messages.
     */
    @Inject
    private Logger logger;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return "DistributionInitializerListener";
    }

    @Override
    public void onEvent(Event event, Object arg1, Object arg2)
    {
        DistributionState distributionState = this.distributionManager.getDistributionState();

        // Is install already done (allow to cancel stuff for example)
        if (distributionState == DistributionState.SAME) {
            if (event instanceof ApplicationStartedEvent) {
                DistributionJobStatus< ? > status = this.distributionManager.getPreviousFarmJobStatus();

                for (DistributionStep step : status.getSteps()) {
                    if (step.getState() == null) {
                        this.distributionManager.startFarmJob();
                        break;
                    }
                }
            } else {
                String wiki = ((WikiReadyEvent) event).getWikiId();

                if (!((XWikiContext) arg2).isMainWiki()) {
                    WikiDistributionJobStatus status = this.distributionManager.getPreviousWikiJobStatus(wiki);

                    for (DistributionStep step : status.getSteps()) {
                        if (step.getState() == null) {
                            this.distributionManager.startWikiJob(wiki);
                            break;
                        }
                    }
                }
            }
        } else {
            this.logger.info("Distribution state: {}", distributionState);
            this.distributionManager.startFarmJob();
        }
    }
}
