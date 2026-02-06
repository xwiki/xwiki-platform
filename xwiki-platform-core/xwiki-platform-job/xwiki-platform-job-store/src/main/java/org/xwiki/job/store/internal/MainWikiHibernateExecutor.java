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
package org.xwiki.job.store.internal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;

import org.xwiki.job.store.internal.hibernate.JobStatusHibernateExecutor;

/**
 * Executes Hibernate operations in the main wiki.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@Component(roles = MainWikiHibernateExecutor.class)
@Singleton
public class MainWikiHibernateExecutor
{
    @Inject
    private JobStatusHibernateExecutor hibernateExecutor;

    /**
     * Execute a read-only Hibernate action.
     *
     * @param action the Hibernate action to execute
     * @param <T> the return type of the action
     * @return the result of the action
     * @throws JobStatusStoreException if an error occurs during the action execution
     */
    public <T> T executeRead(JobStatusHibernateExecutor.HibernateCallback<T> action) throws JobStatusStoreException
    {
        return this.hibernateExecutor.executeRead(action);
    }

    /**
     * Execute a write Hibernate action.
     *
     * @param action the Hibernate action to execute
     * @throws JobStatusStoreException if an error occurs during the action execution
     */
    public void executeWrite(JobStatusHibernateExecutor.HibernateCallback<Void> action) throws JobStatusStoreException
    {
        this.hibernateExecutor.executeWrite(action);
    }

}
