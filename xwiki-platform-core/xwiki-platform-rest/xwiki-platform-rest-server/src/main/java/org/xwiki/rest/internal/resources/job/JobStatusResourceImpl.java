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
package org.xwiki.rest.internal.resources.job;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiJobResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.JobStatus;
import org.xwiki.rest.resources.job.JobStatusResource;

/**
 * @version $Id$
 * @since 7.2M3
 */
@Component
@Named("org.xwiki.rest.internal.resources.job.JobStatusResourceImpl")
public class JobStatusResourceImpl extends XWikiJobResource implements JobStatusResource
{
    @Inject
    private ModelFactory factory;

    @Override
    public JobStatus getJobStatus(String jobId, boolean request, boolean progress, boolean log, String logFromLevel)
        throws XWikiRestException
    {
        return this.factory.toRestJobStatus(getRealJobStatus(jobId), uriInfo.getAbsolutePath(), request, progress, log,
            logFromLevel);
    }
}
