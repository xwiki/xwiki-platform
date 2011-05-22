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
package org.xwiki.extension.task;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Proxy used to simplify execution of tasks.
 * 
 * @version $Id$
 */
@ComponentRole
public interface TaskManager
{
    /**
     * @return the task currently running or the lastest task
     */
    Task getCurrentTask();

    /**
     * Start an extension installation task.
     * <p>
     * It's the same as calling {@link #executeTask(String, Request)} with identifier "install".
     * 
     * @param request the request
     * @return the created install task
     * @throws TaskException error when trying to run install task
     */
    Task install(InstallRequest request) throws TaskException;

    /**
     * Start an extension uninstall task.
     * <p>
     * It's the same as calling {@link #executeTask(String, Request)} with identifier "uninstall".
     * 
     * @param request the request
     * @return the created uninstall task
     * @throws TaskException error when trying to run uninstall task
     */
    Task uninstall(UninstallRequest request) throws TaskException;

    /**
     * Start a new task with teh provided identifier.
     * 
     * @param taskId the role hint of the task component
     * @param request the request
     * @return the created task
     * @throws TaskException error when trying to run the task
     */
    Task executeTask(String taskId, Request request) throws TaskException;
}
