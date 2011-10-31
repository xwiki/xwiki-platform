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
package org.xwiki.store;

/**
 * A TransactionRunnable which is safe to start.
 * If your TransactionRunnable can safely be started on it's own, and does not need to be run inside of
 * another runnable, then it should extend StartableTransactionRunnable.
 *
 * @param <T> see: {@link TransactionRunnable}
 * @version $Id$
 * @since 3.0M2
 */
public class StartableTransactionRunnable<T> extends ProvidingTransactionRunnable<T, T>
{
    /**
     * True after this runnable has been started and once true, this runnable may not be run again.
     */
    private boolean alreadyRun;

    /**
     * Start this TransactionRunnable and all that are chained to it.
     *
     * @throws TransactionException if something goes wrong while pre running, running, committing,
     * rolling back or completeing this or any of the chained runnables.
     * @throws IllegalStateException if the same runnable is started more than once.
     */
    public void start() throws TransactionException
    {
        if (this.alreadyRun) {
            throw new IllegalStateException("This TransactionRunnable has already been run and may not "
                + "be run again.");
        }
        this.alreadyRun = true;

        this.preRun();
        this.run();
        this.commit();
        this.complete();
    }
}
