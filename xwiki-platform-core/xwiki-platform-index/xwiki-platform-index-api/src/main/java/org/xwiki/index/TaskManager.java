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
package org.xwiki.index;

import java.util.concurrent.CompletableFuture;

import org.xwiki.component.annotation.Role;
import org.xwiki.index.internal.TaskData;
import org.xwiki.stability.Unstable;

/**
 * Provide the operations to interact with the task manager.
 *
 * @version $Id$
 * @since 14.1RC1
 */
@Role
@Unstable
public interface TaskManager
{
    /**
     * Add a task to the queue.
     *
     * @param wikiId the wiki containing the document
     * @param docId the document id
     * @param version the document version
     * @param type the type of task to add
     * @return a completable future for this task
     */
    CompletableFuture<TaskData> addTask(String wikiId, long docId, String version, String type);

    /**
     * Replace all the tasks of the queue with the same document and task type with the new task.
     *
     * @param wikiId the wiki containing the document
     * @param docId the document id
     * @param version the document version
     * @param type the type of task to add
     * @return a completable future for this task
     */
    CompletableFuture<TaskData> replaceTask(String wikiId, long docId, String version, String type);

    /**
     * @return the number of tasks in the queue
     */
    long getQueueSize();

    /**
     * @param type the type of task to count
     * @return the number of  tasks of a given type in the queue
     */
    long getQueueSize(String type);
}
