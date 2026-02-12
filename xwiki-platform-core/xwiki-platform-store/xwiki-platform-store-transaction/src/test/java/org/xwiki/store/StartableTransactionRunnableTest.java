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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for StartableTransactionRunnable
 *
 * @version $Id$
 * @since 3.0M2
 */
class StartableTransactionRunnableTest
{
    private final StartableTransactionRunnable testRunnable = new StartableTransactionRunnable();

    private boolean hasRun;

    @Test
    void alreadyRunTest() throws Exception
    {
        this.testRunnable.start();
        assertThrows(IllegalStateException.class, () -> this.testRunnable.start()) ;
    }

    @Test
    void rollbackAfterExceptionTest()
    {
        new TransactionRunnable()
        {
            protected void onRun() throws Exception
            {
                throw new Exception();
            }

            protected void onRollback()
            {
                itRan();
            }
        } .runIn(this.testRunnable);

        var e = assertThrows(TransactionException.class, () -> this.testRunnable.start()) ;
        assertEquals(1, e.exceptionCount(), "Wrong number of exceptions reported");
        assertTrue(hasRun(), "Rollback did not run after exception");
    }

    @Test
    void exceptionInRollbackTest()
    {
        new TransactionRunnable()
        {
            protected void onRun() throws Exception
            {
                throw new Exception();
            }

            protected void onRollback() throws Exception
            {
                throw new Exception();
            }

            protected void onComplete()
            {
                itRan();
            }
        } .runIn(this.testRunnable);

        var e = assertThrows(TransactionException.class, () -> this.testRunnable.start()) ;
        assertEquals(2, e.exceptionCount(), "Wrong number of exceptions reported");
        assertTrue(e.isNonRecoverable(), "Rollback failed and yet the exception did not warn of possible corruption.");
        assertTrue(hasRun(), "Complete did not run after exception");
    }

    /**
     * Make sure an exception or error in onComplete is caught and reported.
     */
    @Test
    void exceptionInCompleteTest()
    {
        new TransactionRunnable()
        {
            protected void onComplete() throws Exception
            {
                throw new Exception();
            }
        } .runIn(this.testRunnable);

        assertThrows(TransactionException.class, () -> this.testRunnable.start());

    }

    private boolean hasRun()
    {
        return this.hasRun;
    }

    private void itRan()
    {
        this.hasRun = true;
    }
}
