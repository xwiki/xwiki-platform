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
import javax.inject.Singleton;

import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.distribution.internal.DistributionManager.DistributionState;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;

/**
 * Trigger distribution jobs.
 * 
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("DistributionInitializerListener")
@Singleton
public class DistributionInitializerListener implements EventListener
{
    /**
     * The list of events to listen to.
     */
    private static final List<Event> EVENTS = Arrays.<Event>asList(new ActionExecutingEvent("view"),
        new ActionExecutingEvent("distribution"), new ApplicationReadyEvent());

    /**
     * The component used to get information about the current distribution.
     */
    @Inject
    private DistributionManager distributionManager;

    @Inject
    private DistributionConfiguration distributionConfiguration;

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
        if (event instanceof ApplicationReadyEvent) {
            onApplicationReady();
        } else {
            onView((XWikiContext) arg2);
        }
    }

    private void onApplicationReady()
    {
        // If the DW is not interactive, trigger it right away
        if (!this.distributionConfiguration.isInteractiveDistributionWizardEnabledForMainWiki()) {
            DistributionState distributionState = this.distributionManager.getFarmDistributionState();

            if (distributionState != DistributionState.NONE) {
                startFarmJob();
            }
        }
    }

    private void onView(XWikiContext xcontext)
    {
        // Do nothing if the current distribution job was triggered already
        // Do nothing if the automatic start of DW is disabled
        if (this.distributionManager.getCurrentDistributionJob() != null
            || !isAutoDistributionWizardEnabled(xcontext)) {
            return;
        }

        DistributionState distributionState = this.distributionManager.getFarmDistributionState();

        // Start the Distribution Wizard only if the current user has the right to access it
        if (distributionState != DistributionState.NONE && this.distributionManager.canDisplayDistributionWizard()) {
            if (xcontext.isMainWiki()) {
                if (this.distributionManager.getFarmJob() == null) {
                    startFarmJob();
                }
            } else {
                String wiki = xcontext.getWikiId();
                if (this.distributionManager.getWikiJob(wiki) == null) {
                    startWikiJob(wiki);
                }
            }
        }
    }

    /**
     * @return if the automatic launch of DW is enabled
     */
    private boolean isAutoDistributionWizardEnabled(XWikiContext xcontext)
    {
        return xcontext.isMainWiki() ? this.distributionConfiguration.isAutoDistributionWizardEnabledForMainWiki()
            : this.distributionConfiguration.isAutoDistributionWizardEnabledForWiki();
    }

    private synchronized void startFarmJob()
    {
        if (this.distributionManager.getFarmJob() == null) {
            this.distributionManager.startFarmJob();
        }
    }

    /**
     * @param wiki the wiki for which to start the distribution job
     */
    private synchronized void startWikiJob(String wiki)
    {
        if (this.distributionManager.getWikiJob(wiki) == null) {
            this.distributionManager.startWikiJob(wiki, true);
        }
    }
}
