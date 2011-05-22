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
package org.xwiki.extension.task.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.task.InstallRequest;
import org.xwiki.extension.task.Request;
import org.xwiki.extension.task.Task;
import org.xwiki.extension.task.Task.Status;
import org.xwiki.extension.task.TaskException;
import org.xwiki.extension.task.TaskManager;
import org.xwiki.extension.task.UninstallRequest;

/**
 * Default implementation of {@link TaskManager}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultTaskManager implements TaskManager
{
    /**
     * Used to lookup {@link Task} implementations.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * @see #getCurrentTask()
     */
    private Task currentTask;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.task.TaskManager#getCurrentTask()
     */
    public Task getCurrentTask()
    {
        return this.currentTask;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.task.TaskManager#install(org.xwiki.extension.task.InstallRequest)
     */
    public Task install(InstallRequest request) throws TaskException
    {
        return executeTask("install", request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.task.TaskManager#uninstall(org.xwiki.extension.task.UninstallRequest)
     */
    public Task uninstall(UninstallRequest request) throws TaskException
    {
        return executeTask("uninstall", request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.task.TaskManager#executeTask(java.lang.String, org.xwiki.extension.task.Request)
     */
    public synchronized Task executeTask(String taskId, Request request) throws TaskException
    {
        if (this.currentTask != null && this.currentTask.getStatus() != Status.FINISHED) {
            throw new TaskException("A task is already running");
        }

        try {
            this.currentTask = this.componentManager.lookup(Task.class, taskId);
        } catch (ComponentLookupException e) {
            throw new TaskException("Failed to lookup any Task for role hint [" + taskId + "]", e);
        }

        this.currentTask.start(request);

        return this.currentTask;
    }
}
