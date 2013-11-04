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

import org.xwiki.component.annotation.Role;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.template.WikiTemplateManagerException;

/**
 * Executor that manage job that provision wikis.
 *
 * @since 5.3M3
 * @version $Id$
 */
@Role
@Unstable
public interface TemplateWikiProvisionerExecutor
{
    /**
     * Create a job and execute it.
     *
     * @param wikiId id of the wiki to provision
     * @param templateId id of the template to use
     * @throws WikiTemplateManagerException if problems occur
     */
    void createAndExecuteJob(String wikiId, String templateId) throws WikiTemplateManagerException;

    /**
     * Get the status of a job.
     *
     * @param wikiId Id od the wiki concerned by the job
     * @return the job status
     * @throws WikiTemplateManagerException if problems occur
     */
    JobStatus getJobStatus(String wikiId) throws WikiTemplateManagerException;
}
