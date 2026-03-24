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

/**
 * Tests for ProvidingTransactionRunnable.
 *
 * @version $Id$
 * @since 3.0M2
 */
class ProvidingTransactionRunnableTest
{
    private static final String DB_CONNECTION = "Hello, I'm a database connection!";

    private static final String DATA_A1 = "hello world";

    @Test
    void dataSharingTest() throws Exception
    {
        DBStartableTransactionRunnable run = new DBStartableTransactionRunnable();
        ProvidingTransactionRunnable<DBTransaction, MyInterfaceWithDataA1> tr1 = new TR1();
        tr1.runIn(run);
        TransactionRunnable<DBTransaction> tr2 = new TR2();
        TransactionRunnable<MyInterfaceWithDataA1> tr2WithNewCapabilities = tr2.runIn(tr1.asProvider());

        new TR3().runIn(tr2WithNewCapabilities);
        // This should fail at compile time: new TR3().runIn(tr2);
        run.start();
    }

    private interface DBTransaction
    {
        String getConnection();
    }

    private static class MyDBTransaction implements DBTransaction
    {
        public String getConnection()
        {
            return DB_CONNECTION;
        }
    }

    private interface MyInterfaceWithDataA1 extends DBTransaction
    {
        String getDataA1();
    }

    private static class ImplementationWithDataA1 implements MyInterfaceWithDataA1
    {
        private final DBTransaction DBTrans;

        private final String dataA1;

        public ImplementationWithDataA1(final DBTransaction transact, final String dataA1)
        {
            this.DBTrans = transact;
            this.dataA1 = dataA1;
        }

        public String getDataA1()
        {
            return this.dataA1;
        }

        public String getConnection()
        {
            return this.DBTrans.getConnection();
        }
    }

    private static class DBStartableTransactionRunnable extends StartableTransactionRunnable<DBTransaction>
    {
        @Override
        protected DBTransaction getProvidedContext()
        {
            return new MyDBTransaction();
        }
    }

    private static class TR1 extends ProvidingTransactionRunnable<DBTransaction, MyInterfaceWithDataA1>
    {
        private String dataA1;

        @Override
        protected void onRun()
        {
            assertEquals(DB_CONNECTION, this.getContext().getConnection(), "DB Connection was not correct in TR1");
            this.dataA1 = DATA_A1;
        }

        @Override
        protected MyInterfaceWithDataA1 getProvidedContext()
        {
            return new ImplementationWithDataA1(this.getContext(), this.dataA1);
        }
    }

    private class TR2 extends TransactionRunnable<DBTransaction>
    {
        protected void onRun()
        {
            assertEquals(DB_CONNECTION, this.getContext().getConnection(), "DB Connection was not correct in TR2");
        }
    }

    private class TR3 extends TransactionRunnable<MyInterfaceWithDataA1>
    {
        protected void onRun()
        {
            assertEquals(DATA_A1, this.getContext().getDataA1(), "Data A1 was not correct in TR3");
            assertEquals(DB_CONNECTION, this.getContext().getConnection(), "DB Connection was not correct in TR3");
        }
    }
}
