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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for TransactionRunnable.
 *
 * @version $Id$
 * @since 3.0M2
 */
class TransactionRunnableTest
{
    private final TransactionRunnable testCase = new TransactionRunnable();

    private boolean hasRun;

    /**
     * Make sure the user cannot do anything foolish.
     */
    @Test
    void infiniloopTest()
    {
        TransactionRunnable transactionRunnable = new TransactionRunnable();
        assertThrows(IllegalArgumentException.class, () -> this.testCase.runIn(transactionRunnable.runIn(this.testCase)));
    }

    /**
     * Make sure the user cannot do anything foolish.
     */
    @Test
    void runInMultiTest()
    {
        this.testCase.runIn(new TransactionRunnable());
        TransactionRunnable parentRunnable = new TransactionRunnable();
        assertThrows(IllegalStateException.class, () -> this.testCase.runIn(parentRunnable));
    }

    /**
     * preRun: Parent before child, exceptions cause everything to stop.
     */
    @Test
    void preRunChildTest()
    {
        new TransactionRunnable()
        {
            protected void onPreRun()
            {
                fail("Run in wrong order.");
            }
        }.runIn(new TransactionRunnable()
        {
            protected void onPreRun() throws Exception
            {
                throw new CustomException();
            }
        } .runIn(this.testCase));

        assertThrows(TransactionException.class, () -> this.testCase.preRun());
    }

    /**
     * preRun: Siblings in same order as registered, exceptions cause everything to stop.
     */
    @Test
    void preRunSiblingTest()
    {
        new TransactionRunnable()
        {
            protected void onPreRun() throws Exception
            {
                throw new CustomException();
            }
        }.runIn(this.testCase);

        new TransactionRunnable()
        {
            protected void onPreRun()
            {
                fail("Run in wrong order.");
            }
        }.runIn(this.testCase);

        assertThrows(TransactionException.class, () -> this.testCase.preRun());
    }

    /**
     * Run: Parent before child, exceptions cause everything to stop.
     */
    @Test
    void runChildTest()
    {
        new TransactionRunnable()
        {
            protected void onRun()
            {
                fail("Run in wrong order.");
            }
        }.runIn(new TransactionRunnable()
        {
            protected void onRun() throws Exception
            {
                throw new CustomException();
            }
        }.runIn(this.testCase));

        assertThrows(TransactionException.class, () -> this.testCase.run());
    }

    /**
     * Run: Siblings in same order as registered, exceptions cause everything to stop.
     */
    @Test
    void runSiblingTest()
    {
        new TransactionRunnable()
        {
            protected void onRun() throws Exception
            {
                throw new CustomException();
            }
        }.runIn(this.testCase);

        new TransactionRunnable()
        {
            protected void onRun()
            {
                fail("Run in wrong order.");
            }
        }.runIn(this.testCase);

        assertThrows(TransactionException.class, () -> this.testCase.run());
    }

    /**
     * Commit: Child before parent, exceptions cause everything to stop.
     */
    @Test
    void commitChildTest()
    {
        new TransactionRunnable()
        {
            // Child first
            protected void onCommit() throws Exception
            {
                throw new CustomException();
            }
        }.runIn(new TransactionRunnable()
        {
            // Then parent
            protected void onCommit()
            {
                fail("Run in wrong order.");
            }
        }.runIn(this.testCase));

        assertThrows(TransactionException.class, () -> this.testCase.commit());
    }

    /**
     * Commit: Siblings in reverse order as registered, exceptions cause everything to stop.
     */
    @Test
    void commitSiblingTest()
    {
        new TransactionRunnable()
        {
            protected void onCommit()
            {
                fail("Run in wrong order.");
            }
        }.runIn(this.testCase);

        new TransactionRunnable()
        {
            protected void onCommit() throws Exception
            {
                throw new CustomException();
            }
        }.runIn(this.testCase);

        assertThrows(TransactionException.class, () -> this.testCase.commit());
    }

    /**
     * Rollback: Child before parent, exceptions are collected and thrown at end.
     * Exception must indicate possibility of corruption.
     */
    @Test
    void rollbackChildTest()
    {
        new TransactionRunnable()
        {
            protected void onRollback() throws Exception
            {
                assertFalse(hasRun(), "Child rolled back after parent.");
                throw new CustomException();
            }
        }.runIn(new TransactionRunnable()
        {
            protected void onRollback() throws Exception
            {
                itRan();
            }
        }.runIn(this.testCase));


        var e = assertThrows(TransactionException.class, () -> this.testCase.rollback());
        assertTrue(e.isNonRecoverable(), "onRollback failed and exception did not indicate the possibility of "
            + "storage corruption.");
        assertTrue(hasRun(), "onRollback did not run for a child of a runnable which threw an exception.");
    }

    /**
     * Rollback: Siblings in reverse order as registered, exceptions are collected and thrown at end.
     * Exception must indicate possibility of corruption.
     */
    @Test
    void rollbackSiblingTest()
    {
        new TransactionRunnable()
        {
            protected void onRollback()
            {
                itRan();
            }
        }.runIn(this.testCase);

        new TransactionRunnable()
        {
            protected void onRollback() throws Exception
            {
                assertFalse(hasRun(), "Siblings rolled back in same order as they were registered.");
                throw new CustomException();
            }
        }.runIn(this.testCase);


        var e = assertThrows(TransactionException.class, () -> this.testCase.rollback());
        assertTrue(e.isNonRecoverable(), "onRollback failed and exception did not indicate the possibility of storage"
            + " corruption.");
        assertTrue(hasRun(), "onRollback did not run for the sibling of a runnable which threw an exception.");
    }

    /**
     * Complete: Child before parent, exceptions are collected and thrown at end.
     * Exception must not indicate possibility of corruption.
     */
    @Test
    void completeChildTest()
    {
        new TransactionRunnable()
        {
            protected void onComplete() throws Exception
            {
                itRan();
                throw new CustomException();
            }
        }.runIn(new TransactionRunnable()
        {
            protected void onComplete()
            {
                assertTrue(hasRun(), "Child run after parent.");
            }
        }.runIn(this.testCase));


        var e = assertThrows(TransactionException.class, () -> this.testCase.complete());
        assertFalse(e.isNonRecoverable(), "onComplete failed and exception erroniously indicated the possibility of storage corruption.");
        assertTrue(hasRun(), "onComplete did not run for a child of a runnable which threw an exception.");
    }

    /**
     * Complete: Siblings in opposite as registered, exceptions are collected and thrown at end.
     * Exception must not indicate possibility of corruption.
     */
    @Test
    void completeSiblingTest()
    {
        new TransactionRunnable()
        {
            protected void onComplete() throws Exception
            {
                assertTrue(hasRun(), "onComplete for siblings run in same order as registered.");
                throw new CustomException();
            }
        }.runIn(this.testCase);

        new TransactionRunnable()
        {
            protected void onComplete()
            {
                itRan();
            }
        }.runIn(this.testCase);

        var e = assertThrows(TransactionException.class, () -> this.testCase.complete());
        assertFalse(e.isNonRecoverable(), "onComplete failed and exception erroniously indicated the possibility of storage corruption.");
        assertTrue(hasRun(), "onComplete did not run for the sibling of a runnable which threw an exception.");
    }

    /**
     * onComplete should run for any TR which has had onPreRun called on it but not for ones which didn't.
     */
    @Test
    void noCompleteUnlessPreRunTest()
    {
        new TransactionRunnable()
        {
            protected void onPreRun() throws Exception
            {
                throw new CustomException();
            }

            protected void onComplete()
            {
                itRan();
            }
        }.runIn(this.testCase);

        new TransactionRunnable()
        {
            protected void onComplete()
            {
                fail("onComplete ran for a TransactionRunnable which did not have onPreRun called.");
            }
        }.runIn(this.testCase);

        assertThrows(TransactionException.class, () -> this.testCase.preRun());
        assertTrue(hasRun(), "onComplete did not run for runnable which was preRun.");
    }

    /**
     * onComplete should run for any TR which has had onPreRun called on it but not for ones which didn't.
     */
    @Test
    void noRollbackUnlessRunTest()
    {
        new TransactionRunnable()
        {
            protected void onRun() throws Exception
            {
                throw new CustomException();
            }

            protected void onRollback()
            {
                itRan();
            }
        }.runIn(this.testCase);

        new TransactionRunnable()
        {
            protected void onRollback()
            {
                fail("onRollback ran for a TransactionRunnable which did not have onRun called.");
            }
        }.runIn(this.testCase);

        assertThrows(TransactionException.class, () -> this.testCase.run());
        assertTrue(hasRun(), "onRollback did not run for runnable which was run.");
    }

    private boolean hasRun()
    {
        return this.hasRun;
    }

    private void itRan()
    {
        this.hasRun = true;
    }

    private final class CustomException extends Exception
    {
        // Does nothing different.
    }
}
