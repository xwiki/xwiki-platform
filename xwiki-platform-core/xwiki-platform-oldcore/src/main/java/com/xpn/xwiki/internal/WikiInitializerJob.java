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

import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.job.AbstractJob;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Job dedicated to wiki initialization.
 * 
 * @version $Id$
 * @since 8.4RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(WikiInitializerJob.JOBTYPE)
public class WikiInitializerJob extends AbstractJob<WikiInitializerRequest, WikiInitializerJobStatus>
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "wiki.init";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ObservationManager observation;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> resolver;

    @Override
    protected WikiInitializerJobStatus createNewStatus(WikiInitializerRequest request)
    {
        return new WikiInitializerJobStatus(request, this.observationManager, this.loggerManager);
    }

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        String wikiId = getRequest().getWikiId();

        this.logger.info("Start initialization of wiki [{}]", wikiId);

        XWikiContext xcontext = this.xcontextProvider.get();

        // Set proper context
        xcontext.setWikiId(wikiId);
        xcontext.setOriginalWikiId(wikiId);

        this.progressManager.pushLevelProgress(3, this);

        try {
            this.progressManager.startStep(this, "Initialize mandatory document");

            // Initialize mandatory document
            xcontext.getWiki().initializeMandatoryDocuments(xcontext);

            this.progressManager.startStep(this, "Initialize plugins");

            // Initialize plugins
            xcontext.getWiki().getPluginManager().virtualInit(xcontext);

            this.logger.info("Initialization of wiki [{}] done", wikiId);

            this.progressManager.startStep(this, "Call listeners");

            // Send event to notify listeners that the subwiki is ready
            this.observation.notify(new WikiReadyEvent(wikiId), wikiId, xcontext);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }
}
