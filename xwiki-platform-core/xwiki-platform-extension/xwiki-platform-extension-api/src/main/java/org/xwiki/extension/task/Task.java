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

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

/**
 * @version $Id$
 */
@ComponentRole
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public interface Task
{
    /**
     * Taks status.
     * 
     * @version $Id$
     */
    enum Status
    {
        /**
         * Default status, generally mean that the task has not been started yet.
         */
        NONE,

        /**
         * The task has been paused.
         */
        PAUSED,

        /**
         * The task is running.
         */
        RUNNING,

        /**
         * The task is done.
         */
        FINISHED
    }

    /**
     * @return the status of the task
     */
    Status getStatus();

    /**
     * @return the exceptions raised during the task execution, null of all went well
     */
    List<Exception> getExceptions();

    /**
     * @return the task request
     */
    Request getRequest();

    /**
     * @param request start the task with provided request
     */
    void start(Request request);
}
