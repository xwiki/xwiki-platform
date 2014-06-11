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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.concurrent.ExecutionContextRunnable;
import org.xwiki.job.Request;
import org.xwiki.job.internal.AbstractJob;
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.XWikiEngineContext;

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
    protected XWikiInitializerRequest castRequest(Request request)
    {
        XWikiInitializerRequest indexerRequest;
        if (request instanceof XWikiInitializerRequest) {
            indexerRequest = (XWikiInitializerRequest) request;
        } else {
            indexerRequest = new XWikiInitializerRequest(request);
        }

        return indexerRequest;
    }

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
            String xwikiname = XWiki.DEFAULT_MAIN_WIKI;

            XWikiContext xcontext = this.xcontextProvider.get();
            XWikiEngineContext econtext = xcontext.getEngineContext();

            XWiki xwiki = new XWiki(xcontext, xcontext.getEngineContext(), true);

            // initialize stub context here instead of during Execution context initialization because
            // during Execution context initialization, the XWikiContext is not fully initialized (does not
            // contains XWiki object) which make it unusable
            this.stubContextProvider.initialize(xcontext);

            this.progressManager.stepPropress(this);

            this.logger.info("XWiki initialization done");

            // Send Event to signal that the application is ready to service requests.
            this.observation.notify(new ApplicationReadyEvent(), xwiki, xcontext);

            // Make XWiki class available to others (among other things it unlock page loading)
            econtext.setAttribute(xwikiname, xwiki);
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

    /**
     * First try to find the configuration file pointed by the passed location as a file. If it does not exist or if the
     * file cannot be read (for example if the security manager doesn't allow it), then try to load the file as a
     * resource using the Servlet Context and failing that from the classpath.
     * 
     * @param configurationLocation the location where the XWiki configuration file is located (either an absolute or
     *            relative file path or a resource location)
     * @return the configuration data
     * @todo this code should be moved to a Configuration class proper
     */
    private XWikiConfig readXWikiConfiguration(String configurationLocation, XWikiEngineContext econtext,
        XWikiContext xcontext)
    {
        InputStream xwikicfgis = null;

        // First try loading from a file.
        File f = new File(configurationLocation);
        try {
            if (f.exists()) {
                xwikicfgis = new FileInputStream(f);
            }
        } catch (Exception e) {
            // Error loading the file. Most likely, the Security Manager prevented it.
            // We'll try loading it as a resource below.
            this.logger.debug("Failed to load the file [" + configurationLocation + "] using direct "
                + "file access. The error was [" + e.getMessage() + "]. Trying to load it "
                + "as a resource using the Servlet Context...");
        }

        // Second, try loading it as a resource using the Servlet Context
        if (xwikicfgis == null) {
            xwikicfgis = econtext.getResourceAsStream(configurationLocation);
            this.logger.debug("Failed to load the resource [" + configurationLocation + "] as a resource "
                + "using the Servlet Context. Trying to load it as classpath resource...");
        }

        // Third, try loading it from the classloader used to load this current class
        if (xwikicfgis == null) {
            // TODO: Verify if checking on MODE_GWT is correct. I think we should only check for
            // the debug mode and even for that we need to find some better way of doing it so
            // that we don't have hardcoded code only for development debugging purposes.
            if (xcontext.getMode() == XWikiContext.MODE_GWT || xcontext.getMode() == XWikiContext.MODE_GWT_DEBUG) {
                xwikicfgis = XWiki.class.getClassLoader().getResourceAsStream("xwiki-gwt.cfg");
            } else {
                xwikicfgis = XWiki.class.getClassLoader().getResourceAsStream("xwiki.cfg");
            }
        }

        if (xwikicfgis == null) {
            this.logger.warn("Failed to load the configuration [" + configurationLocation + "] using any method.");

            return null;
        }

        XWikiConfig config;
        try {
            config = new XWikiConfig(xwikicfgis);
        } catch (XWikiException e) {
            this.logger.error("Failed to load configuration [{}]", configurationLocation, e);
            config = new XWikiConfig();
        } finally {
            IOUtils.closeQuietly(xwikicfgis);
        }

        return config;
    }
}
