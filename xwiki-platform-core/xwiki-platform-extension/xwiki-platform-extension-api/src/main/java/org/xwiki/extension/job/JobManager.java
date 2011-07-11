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
 * Proxy used to simplify execution of tasks.
 * 
 * @version $Id$
 */
@ComponentRole
public interface JobManager
{
    /**
     * @return the task currently running or the lastest task
     */
    Job getCurrentJob();

    /**
     * Start an extension installation task.
     * <p>
     * It's the same as calling {@link #executeJob(String, Request)} with identifier "install".
     * 
     * @param request the request
     * @return the created install task
     * @throws JobException error when trying to run install task
     */
    Job install(InstallRequest request) throws JobException;

    /**
     * Start an extension uninstall task.
     * <p>
     * It's the same as calling {@link #executeJob(String, Request)} with identifier "uninstall".
     * 
     * @param request the request
     * @return the created uninstall task
     * @throws JobException error when trying to run uninstall task
     */
    Job uninstall(UninstallRequest request) throws JobException;

    /**
     * Start a new task with teh provided identifier.
     * 
     * @param taskId the role hint of the task component
     * @param request the request
     * @return the created task
     * @throws JobException error when trying to run the task
     */
    Job executeJob(String taskId, Request request) throws JobException;
}
