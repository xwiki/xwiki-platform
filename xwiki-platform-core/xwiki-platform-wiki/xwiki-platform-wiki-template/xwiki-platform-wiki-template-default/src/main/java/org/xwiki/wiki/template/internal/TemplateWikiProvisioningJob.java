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
package org.xwiki.wiki.template.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.bridge.event.WikiCopiedEvent;
import org.xwiki.bridge.event.WikiProvisionedEvent;
import org.xwiki.bridge.event.WikiProvisioningEvent;
import org.xwiki.bridge.event.WikiProvisioningFailedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.job.internal.AbstractJob;
import org.xwiki.job.internal.DefaultJobStatus;
import org.xwiki.wiki.provisioning.WikiCopier;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.provisioning.WikiProvisioningJob;
import org.xwiki.wiki.provisioning.WikiProvisioningJobRequest;

import com.xpn.xwiki.XWikiContext;

/**
 * Component that createAndExecuteJob a wiki with the content of a template wiki.
 *
 * @since 5.3M2
 * @version $Id$
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(TemplateWikiProvisioningJob.JOBTYPE)
public class TemplateWikiProvisioningJob extends AbstractJob<WikiProvisioningJobRequest,
        DefaultJobStatus<WikiProvisioningJobRequest>> implements WikiProvisioningJob
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "wikiprovisioning.template";

    @Inject
    private WikiCopier wikiCopier;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    protected void runInternal() throws Exception
    {
        WikiProvisioningJobRequest request = getRequest();
        if (!(request.getProvisioningJobParameter() instanceof String)) {
            throw new Exception("The provisioning parameter is not a valid String.");
        }

        XWikiContext xcontext = xcontextProvider.get();
        String wikiId = request.getWikiId();
        String templateId = (String) request.getProvisioningJobParameter();

        // Set the user actually doing the action in the context
        xcontext.setUserReference(request.getProvisioningUser());

        try {
            observationManager.notify(new WikiProvisioningEvent(wikiId), wikiId, xcontext);
            wikiCopier.copyDocuments(templateId, wikiId, false);
            observationManager.notify(new WikiProvisionedEvent(wikiId), wikiId, xcontext);
            observationManager.notify(new WikiCopiedEvent(templateId, wikiId), templateId, xcontext);
        } catch (WikiManagerException e) {
            logger.error("Failed to provision wiki [{}] from template [{}].", wikiId, templateId, e);
            observationManager.notify(new WikiProvisioningFailedEvent(wikiId), wikiId, xcontext);
        }
    }

    @Override
    public String getType()
    {
        return JOBTYPE;
    }
}
