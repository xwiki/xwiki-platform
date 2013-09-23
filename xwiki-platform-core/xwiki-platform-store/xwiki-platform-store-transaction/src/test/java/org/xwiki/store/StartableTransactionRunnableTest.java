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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for StartableTransactionRunnable
 *
 * @version $Id$
 * @since 3.0M2
 */
public class StartableTransactionRunnableTest
{
    private final StartableTransactionRunnable testRunnable = new StartableTransactionRunnable();

    private boolean hasRun;

    @Test(expected = IllegalStateException.class)
    public void alreadyRunTest() throws Exception
    {
        this.testRunnable.start();
        this.testRunnable.start();
        Assert.fail("exception was not thrown");
    }

    @Test(expected = TransactionException.class)
    public void rollbackAfterExceptionTest() throws Exception
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

        try {
            this.testRunnable.start();
        } catch (TransactionException e) {
            Assert.assertEquals("Wrong number of exceptions reported", 1, e.exceptionCount());
            Assert.assertTrue("Rollback did not run after exception", hasRun());
            throw e;
        }
        Assert.fail("exception was not thrown");
    }

    @Test(expected = TransactionException.class)
    public void exceptionInRollbackTest() throws Exception
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

        try {
            this.testRunnable.start();
        } catch (TransactionException e) {
            Assert.assertEquals("Wrong number of exceptions reported", 2, e.exceptionCount());
            Assert.assertTrue("Rollback failed and yet the exception did not warn of possible corruption.",
                e.isNonRecoverable());
            Assert.assertTrue("Complete did not run after exception", hasRun());
            throw e;
        }
        Assert.fail("exception was not thrown");
    }

    /**
     * Make sure an exception or error in onComplete is caught and reported.
     */
    @Test(expected = TransactionException.class)
    public void exceptionInCompleteTest() throws Exception
    {
        new TransactionRunnable()
        {
            protected void onComplete() throws Exception
            {
                throw new Exception();
            }
        } .runIn(this.testRunnable);

        this.testRunnable.start();
        Assert.fail("exception was not thrown");
    }

    public boolean hasRun()
    {
        return this.hasRun;
    }

    public void itRan()
    {
        this.hasRun = true;
    }
}
