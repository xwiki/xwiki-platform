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

import java.util.Collection;

import javax.inject.Named;

import org.xwiki.job.event.status.JobStatus;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.rest.XWikiJobResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.model.jaxb.JobLog;
import org.xwiki.rest.resources.job.JobLogResource;

/**
 * @version $Id$
 * @since 7.2M3 
 */
@Named("org.xwiki.rest.internal.resources.job.JobLogResourceImpl")
public class JobLogResourceImpl extends XWikiJobResource implements JobLogResource
{
    @Override
    public JobLog getJobLog(String jobId, String level, String fromLevel) throws XWikiRestException
    {
        JobStatus jobStatus = getRealJobStatus(jobId);

        Collection<LogEvent> log;
        if (level != null) {
            log = jobStatus.getLog().getLogs(LogLevel.valueOf(level.toUpperCase()));
        } else if (fromLevel != null) {
            log = jobStatus.getLog().getLogsFrom(LogLevel.valueOf(fromLevel.toUpperCase()));
        } else {
            log = jobStatus.getLog();
        }
        return DomainObjectFactory.createLog(objectFactory, uriInfo.getAbsolutePath(), log);
    }
}
