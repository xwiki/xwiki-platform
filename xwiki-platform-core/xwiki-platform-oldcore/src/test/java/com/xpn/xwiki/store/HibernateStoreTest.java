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
package com.xpn.xwiki.store;

import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.internal.store.hibernate.HibernateStore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HibernateStoreTest
{
    @Rule
    public MockitoComponentMockingRule<HibernateStore> mocker =
        new MockitoComponentMockingRule<HibernateStore>(HibernateStore.class);

    private Transaction transaction = mock(Transaction.class);

    @Before
    public void before() throws ComponentLookupException
    {
        ExecutionContext executionContext = mock(ExecutionContext.class);

        Execution execution = this.mocker.getInstance(Execution.class);
        when(execution.getContext()).thenReturn(executionContext);

        when(executionContext.getProperty("hibtransaction")).thenReturn(transaction);
    }

    @Test
    public void testEndTransactionWhenSQLBatchUpdateExceptionThrown() throws Exception
    {
        SQLException sqlException2 = new SQLException("sqlexception2");
        sqlException2.setNextException(new SQLException("nextexception2"));

        SQLException sqlException1 = new SQLException("sqlexception1");
        sqlException1.initCause(sqlException2);
        sqlException1.setNextException(new SQLException("nextexception1"));

        doThrow(new HibernateException("exception1", sqlException1)).when(transaction).commit();

        try {
            mocker.getComponentUnderTest().endTransaction(true);
            fail("Should have thrown an exception here");
        } catch (HibernateException e) {
            assertEquals("Failed to commit or rollback transaction. Root cause [\n"
                + "SQL next exception = [java.sql.SQLException: nextexception1]\n"
                + "SQL next exception = [java.sql.SQLException: nextexception2]]", e.getMessage());
        }
    }
}
