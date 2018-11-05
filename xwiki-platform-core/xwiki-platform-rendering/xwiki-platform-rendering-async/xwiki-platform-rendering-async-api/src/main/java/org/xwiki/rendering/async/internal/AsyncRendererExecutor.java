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
package org.xwiki.rendering.async.internal;

import java.util.List;
import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.job.JobException;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.RenderingException;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

/**
 * Start and cache asynchronous rendering.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Role
@Unstable
public interface AsyncRendererExecutor
{
    /**
     * @param id the identifier of the execution
     * @param wait true if the method should wait for the execution to be finished, otherwise return null
     * @return the result of the {@link AsyncRenderer} execution for the passed id
     * @throws InterruptedException when the thread is interrupted while waiting
     */
    AsyncRendererResult getResult(List<String> id, boolean wait) throws InterruptedException;

    /**
     * Start and cache or return the status of the job corresponding to the passed renderer.
     * 
     * @param renderer the execution
     * @param contextEntries the list of context entries to take remember for the execution
     * @return the {@link JobStatus}
     * @throws JobException when failing to start the job
     * @throws RenderingException when failing to execute the renderer (in case asynchronous execution is disabled)
     */
    AsyncRendererJobStatus render(AsyncRenderer renderer, Set<String> contextEntries)
        throws JobException, RenderingException;

    /**
     * Start and cache or return the status of the job corresponding to the passed renderer.
     * 
     * @param renderer the execution
     * @param contextEntries the list of context entries to take remember for the execution
     * @param right the right required to access the result
     * @param rightEntity the reference on which the right is required to access the result
     * @return the {@link JobStatus}
     * @throws JobException when failing to start the job
     * @throws RenderingException when failing to execute the renderer (in case asynchronous execution is disabled)
     */
    AsyncRendererJobStatus render(AsyncRenderer renderer, Set<String> contextEntries, Right right,
        EntityReference rightEntity) throws JobException, RenderingException;
}
