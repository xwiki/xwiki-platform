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
package com.xpn.xwiki.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * Job dedicated to XWiki initialization.
 *
 * @version $Id$
 * @since 6.1M1
 */
@Component
@Named(XWikiInitializerJob.JOBTYPE)
@Singleton
public class XWikiInitializerJob extends AbstractJob<XWikiInitializerRequest, XWikiInitializerJobStatus>
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "xwiki.init";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private XWikiStubContextProvider stubContextProvider;

    @Inject
    private ObservationManager observation;

    private Thread thread;

    @Override
    protected XWikiInitializerJobStatus createNewStatus(XWikiInitializerRequest request)
    {
        return new XWikiInitializerJobStatus(request, this.observationManager, this.loggerManager);
    }

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    /**
     * Start the job in a thread.
     */
    public synchronized void startAsync()
    {
        if (this.thread == null) {
            initialize(new XWikiInitializerRequest());

            this.thread = new Thread(this);

            this.thread.setDaemon(true);
            this.thread.setName("XWiki initialization");
            this.thread.start();
        }
    }

    @Override
    protected void runInternal() throws Exception
    {
        this.logger.info("Start XWiki initialization");

        this.progressManager.pushLevelProgress(2, this);

        try {
            this.progressManager.startStep(this);

            XWikiContext xcontext = this.xcontextProvider.get();

            XWiki xwiki = new XWiki(xcontext, xcontext.getEngineContext(), true);

            // initialize stub context here instead of during Execution context initialization because
            // during Execution context initialization, the XWikiContext is not fully initialized (does not
            // contains XWiki object) which make it unusable
            this.stubContextProvider.initialize(xcontext);

            this.progressManager.endStep(this);

            this.progressManager.startStep(this);

            this.logger.info("XWiki initialization done");

            // Send Event to signal that the application is ready to service requests.
            this.observation.notify(new ApplicationReadyEvent(), xwiki, xcontext);

            // Make XWiki class available to others (among other things it unlock page loading)
            xcontext.getEngineContext().setAttribute(XWiki.DEFAULT_MAIN_WIKI, xwiki);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    @Override
    protected void jobFinished(Throwable exception)
    {
        super.jobFinished(exception);

        this.thread = null;
    }
}
