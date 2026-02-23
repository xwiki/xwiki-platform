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
package org.xwiki.job.store.internal.hibernate;

import javax.inject.Inject;
import javax.inject.Singleton;

import jakarta.inject.Provider;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.store.internal.JobStatusStoreException;

/**
 * Executes Hibernate operations for the job status store, using the dedicated {@link JobStatusHibernateStore}.
 * <p>
 * Each invocation uses its own {@link Session}/{@link Transaction} so that job logs are not rolled back together with
 * XWiki's main transactions.
 *
 * @version $Id$
 * @since 18.2.0RC1
 */
@Component(roles = JobStatusHibernateExecutor.class)
@Singleton
public class JobStatusHibernateExecutor
{
    @Inject
    private Provider<JobStatusHibernateStore> hibernateStoreProvider;

    /**
     * Execute a read-only Hibernate action.
     *
     * @param action the Hibernate action to execute
     * @param <T> the return type of the action
     * @return the result of the action
     * @throws JobStatusStoreException if an error occurs during the action execution
     */
    public <T> T executeRead(HibernateCallback<T> action) throws JobStatusStoreException
    {
        return execute(action, false);
    }

    /**
     * Execute a write Hibernate action.
     *
     * @param action the Hibernate action to execute
     * @param <T> the return type of the action
     * @return the result of the action
     * @throws JobStatusStoreException if an error occurs during the action execution
     */
    public <T> T executeWrite(HibernateCallback<T> action) throws JobStatusStoreException
    {
        return execute(action, true);
    }

    private <T> T execute(HibernateCallback<T> action, boolean write)
    {
        JobStatusHibernateStore jobStatusHibernateStore = this.hibernateStoreProvider.get();
        Transaction transaction = null;
        try (Session session = jobStatusHibernateStore.getSessionFactory().openSession()) {
            // Explicitly select the main wiki database/schema.
            jobStatusHibernateStore.setMainWiki(session);

            if (write) {
                transaction = session.beginTransaction();
            }

            T result = action.doInHibernate(session);

            if (transaction != null) {
                transaction.commit();
            }

            return result;
        } catch (Exception e) {
            throw new JobStatusStoreException("Failed to execute job status Hibernate operation.", e);
        } finally {
            rollbackQuietly(transaction);
        }
    }

    private void rollbackQuietly(Transaction transaction)
    {
        if (transaction != null && !transaction.getStatus().equals(TransactionStatus.COMMITTED)) {
            try {
                transaction.rollback();
            } catch (Exception ignored) {
                // Ignore rollback failures.
            }
        }
    }

    /**
     * Callback interface for operations in Hibernate.
     *
     * @param <T> the return type
     */
    @FunctionalInterface
    public interface HibernateCallback<T>
    {
        /**
         * @param session an open Hibernate session
         * @return any result
         * @throws Exception on failure
         */
        T doInHibernate(Session session) throws Exception;
    }
}
