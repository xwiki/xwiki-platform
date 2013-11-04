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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.job.internal.AbstractJob;
import org.xwiki.job.internal.DefaultJobStatus;
import org.xwiki.wiki.internal.manager.WikiCopier;
import org.xwiki.wiki.provisioning.WikiProvisioner;
import org.xwiki.wiki.provisioning.WikiProvisionerRequest;

/**
 * Component that createAndExecuteJob a wiki with the content of a template wiki.
 *
 * @since 5.3M2
 * @version $Id :$
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(TemplateWikiProvisioner.JOBTYPE)
public class TemplateWikiProvisioner extends AbstractJob<TemplateWikiProvisionerRequest,
        DefaultJobStatus<WikiProvisionerRequest>> implements WikiProvisioner
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "wikiprovisioner.template";

    @Inject
    private WikiCopier wikiCopier;

    @Override
    protected void runInternal() throws Exception
    {
        TemplateWikiProvisionerRequest request = getRequest();
        wikiCopier.copyDocuments(request.getTemplateId(), request.getWikiId(), false);
    }

    @Override
    public String getType()
    {
        return JOBTYPE;
    }
}
