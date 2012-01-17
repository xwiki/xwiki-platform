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
package org.xwiki.extension.job;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Proxy used to simplify execution of jobs.
 * 
 * @version $Id$
 */
@ComponentRole
public interface JobManager
{
    /**
     * @return the job currently running or the latest job
     */
    Job getCurrentJob();

    /**
     * Start an extension installation job.
     * <p>
     * It's the same as calling {@link #executeJob(String, Request)} with identifier "install".
     * 
     * @param request the request
     * @return the created install job
     * @throws JobException error when trying to run install job
     */
    Job install(InstallRequest request) throws JobException;

    /**
     * Start an extension uninstall job.
     * <p>
     * It's the same as calling {@link #executeJob(String, Request)} with identifier "uninstall".
     * 
     * @param request the request
     * @return the created uninstall job
     * @throws JobException error when trying to run uninstall job
     */
    Job uninstall(UninstallRequest request) throws JobException;

    /**
     * Start a new job with the provided identifier.
     * 
     * @param jobId the role hint of the job component
     * @param request the request
     * @return the created job
     * @throws JobException error when trying to run the job
     */
    Job executeJob(String jobId, Request request) throws JobException;
}
