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

import java.util.ArrayList;
import java.util.List;

import org.xwiki.extension.task.Request;
import org.xwiki.extension.task.Task;

/**
 * Base class for {@link Task} implementations.
 * 
 * @param <R> the request type associated to the task
 * @version $Id$
 */
public abstract class AbstractTask<R extends Request> implements Task
{
    /**
     * @see #getStatus()
     */
    private Status status = Status.NONE;

    /**
     * @see #getRequest()
     */
    private R request;

    /**
     * @see #getExceptions()
     */
    private List<Exception> exceptions;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.task.Task#getStatus()
     */
    public Status getStatus()
    {
        return this.status;
    }

    /**
     * @param status the status of the task
     */
    public void setStatus(Status status)
    {
        this.status = status;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.task.Task#getRequest()
     */
    public R getRequest()
    {
        return this.request;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.task.Task#getExceptions()
     */
    public List<Exception> getExceptions()
    {
        return this.exceptions;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.task.Task#start(org.xwiki.extension.task.Request)
     */
    public void start(Request request)
    {
        this.request = (R) request;

        try {
            start();
        } catch (Exception e) {
            this.exceptions = new ArrayList<Exception>(1);
            this.exceptions.add(e);
        }

        setStatus(Status.FINISHED);
    }

    /**
     * Should be implemented by {@link Task} implementations.
     * 
     * @throws Exception errors during task execution
     */
    protected abstract void start() throws Exception;
}
