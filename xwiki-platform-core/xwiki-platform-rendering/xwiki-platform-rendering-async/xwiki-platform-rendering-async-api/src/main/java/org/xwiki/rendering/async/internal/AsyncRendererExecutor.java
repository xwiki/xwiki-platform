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
import java.util.concurrent.TimeUnit;

import org.xwiki.component.annotation.Role;
import org.xwiki.job.JobException;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.rendering.RenderingException;
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
     * @param clientId the identifier of the client associated to the result
     * @return the status corresponding to the passed id or null if not could be found
     * @since 10.11RC1
     */
    AsyncRendererJobStatus getAsyncStatus(List<String> id, long clientId);

    /**
     * @param id the identifier of the execution
     * @param clientId the identifier of the client associated to the result
     * @param time the maximum time to wait
     * @param unit the time unit of the {@code time} argument
     * @return the result of the {@link AsyncRenderer} execution for the passed id
     * @throws InterruptedException when the thread is interrupted while waiting
     * @since 10.11RC1
     */
    AsyncRendererJobStatus getAsyncStatus(List<String> id, long clientId, long time, TimeUnit unit)
        throws InterruptedException;

    /**
     * Start and cache or return the status of the job corresponding to the passed renderer.
     * 
     * @param renderer the execution
     * @param configuration the configuration of the execution
     * @return the {@link JobStatus}
     * @throws JobException when failing to start the job
     * @throws RenderingException when failing to execute the renderer (in case asynchronous execution is disabled)
     * @since 10.11RC1
     */
    AsyncRendererExecutorResponse render(AsyncRenderer renderer, AsyncRendererConfiguration configuration)
        throws JobException, RenderingException;
}
