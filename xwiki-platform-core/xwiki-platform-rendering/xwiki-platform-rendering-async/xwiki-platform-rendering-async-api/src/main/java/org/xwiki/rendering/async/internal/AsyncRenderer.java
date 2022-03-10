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

import org.xwiki.job.JobGroupPath;
import org.xwiki.rendering.RenderingException;

/**
 * Execute a task and return a {@link AsyncRendererResult} containing the resulting String (and possibly other more
 * specific results by extending AsyncRendererResult).
 * <p>
 * It also give various information about the constraints of that task to {@link AsyncRendererExecutor} (the id prefix
 * to use to generate the id of the execution, should the result be cached, is asynchronous execution allowed, etc.).
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public interface AsyncRenderer
{
    /**
     * @return the id used as prefix (concatenated with contextual information) for the actual job identifier
     */
    List<String> getId();

    /**
     * @param async true if the renderer is executed asynchronously
     * @param cached true of the result of the renderer will be cached
     * @return the resulting {@link String}
     * @throws RenderingException when failing to execute the renderer
     */
    AsyncRendererResult render(boolean async, boolean cached) throws RenderingException;

    /**
     * @return true if the execution should be asynchronous
     */
    boolean isAsyncAllowed();

    /**
     * @return true if the result be reused several times
     */
    boolean isCacheAllowed();

    /**
     * This allows to specify if the job associated to this renderer should be a {@link org.xwiki.job.GroupedJob} or
     * not. A GroupedJob benefits from being executed in a specific pool of threads that can be customized with a
     * {@link org.xwiki.job.GroupedJobInitializer}. By returning {@code null} here, you decide to not use a GroupedJob
     * and to use a single job executor.
     *
     * @return the {@link JobGroupPath} to be used for this renderer: if null a single job executor will be used.
     * @since 12.5RC1
     */
    default JobGroupPath getJobGroupPath()
    {
        return null;
    }
}
