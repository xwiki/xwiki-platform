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
package org.xwiki.rendering.async.internal.block;

import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.job.JobException;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

/**
 * Start and cache asynchronous {@link Block} based rendering.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Role
@Unstable
public interface BlockAsyncRendererExecutor
{
    /**
     * Start and cache or return the status of the job corresponding to the passed renderer.
     * 
     * @param configuration the configuration of the execution
     * @param contextEntries the list of context entries to take remember for the execution
     * @return the {@link JobStatus}
     * @throws JobException when failing to start the job
     */
    Block execute(BlockAsyncRendererConfiguration configuration, Set<String> contextEntries) throws JobException;

    /**
     * Start and cache or return the status of the job corresponding to the passed renderer.
     * 
     * @param configuration the configuration of the execution
     * @param contextEntries the list of context entries to take remember for the execution
     * @param right the right required to access the result
     * @param rightEntity the reference on which the right is required to access the result
     * @return the {@link JobStatus}
     * @throws JobException when failing to start the job
     */
    Block execute(BlockAsyncRendererConfiguration configuration, Set<String> contextEntries, Right right,
        EntityReference rightEntity) throws JobException;
}
