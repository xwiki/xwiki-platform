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

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.test.LogRule;

import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link XWikiHibernateStore} class.
 * 
 * @version $Id$
 */
public class XWikiHibernateStoreTest extends AbstractBridgedComponentTestCase
{
    @Rule
    public LogRule logRule = new LogRule() {{
        record(LogLevel.WARN);
        recordLoggingForType(XWikiHibernateStore.class);
     }};

    @Test
    public void testGetColumnsForSelectStatement()
    {
        XWikiHibernateStore store = new XWikiHibernateStore("whatever");
        Assert.assertEquals(", doc.date", store.getColumnsForSelectStatement("where 1=1 order by doc.date desc"));
        Assert.assertEquals(", doc.date", store.getColumnsForSelectStatement("where 1=1 order by doc.date asc"));
        Assert.assertEquals(", doc.date", store.getColumnsForSelectStatement("where 1=1 order by doc.date"));
        Assert.assertEquals(", description", store.getColumnsForSelectStatement("where 1=1 order by description desc"));
        Assert.assertEquals(", ascendent", store.getColumnsForSelectStatement("where 1=1 order by ascendent asc"));
        Assert.assertEquals(", doc.date, doc.name",
            store.getColumnsForSelectStatement("where 1=1 order by doc.date, doc.name"));
        Assert.assertEquals(", doc.date, doc.name",
            store.getColumnsForSelectStatement("where 1=1 order by doc.date ASC, doc.name DESC"));
        Assert.assertEquals("", store.getColumnsForSelectStatement(", BaseObject as obj where obj.name=doc.fullName"));
    }

    @Test
    public void testCreateSQLQuery()
    {
        XWikiHibernateStore store = new XWikiHibernateStore("whatever");
        Assert.assertEquals(
            "select distinct doc.space, doc.name from XWikiDocument as doc",
            store.createSQLQuery("select distinct doc.space, doc.name", ""));
        Assert.assertEquals("select distinct doc.space, doc.name, doc.date from XWikiDocument as doc "
            + "where 1=1 order by doc.date desc", store.createSQLQuery(
            "select distinct doc.space, doc.name", "where 1=1 order by doc.date desc"));
    }

    @Test
    public void testEndTransactionWhenSQLBatchUpdateExceptionThrown() throws Exception
    {
        XWikiHibernateStore store = new XWikiHibernateStore("whatever");

        SQLException sqlException2 = new SQLException("sqlexception2");
        sqlException2.setNextException(new SQLException("nextexception2"));

        final SQLException sqlException1 = new SQLException("sqlexception1");
        sqlException1.initCause(sqlException2);
        sqlException1.setNextException(new SQLException("nextexception1"));

        Transaction transaction = mock(Transaction.class);
        doThrow(new HibernateException("exception1", sqlException1)).when(transaction).commit();

        store.setTransaction(transaction, getContext());

        try {
            store.endTransaction(getContext(), true);
            Assert.fail("Should have thrown an exception here");
        } catch (HibernateException e) {
            Assert.assertEquals("Failed to commit or rollback transaction. Root cause [\n"
                + "SQL next exception = [java.sql.SQLException: nextexception1]\n"
                + "SQL next exception = [java.sql.SQLException: nextexception2]]", e.getMessage());
        }
    }

    @Test
    public void executeDeleteWikiStatementForPostgreSQLWhenInSchemaMode() throws Exception
    {
        XWikiHibernateStore store = new XWikiHibernateStore();

        // Used to be able to say that we are in schema mode
        HibernateSessionFactory sessionFactory = mock(HibernateSessionFactory.class);
        ReflectionUtils.setFieldValue(store, "sessionFactory", sessionFactory);
        Configuration configuration = mock(Configuration.class);
        when(sessionFactory.getConfiguration()).thenReturn(configuration);
        when(configuration.getProperty("xwiki.virtual_mode")).thenReturn("schema");

        Statement statement = mock(Statement.class);
        DatabaseProduct databaseProduct = DatabaseProduct.POSTGRESQL;

        store.executeDeleteWikiStatement(statement, databaseProduct, "schema");

        verify(statement).execute("DROP SCHEMA schema CASCADE");

    }

    @Test
    public void executeDeleteWikiStatementForPostgreSQLWhenInDatabaseMode() throws Exception
    {
        XWikiHibernateStore store = new XWikiHibernateStore();

        // Used to be able to say that we are in schema mode
        HibernateSessionFactory sessionFactory = mock(HibernateSessionFactory.class);
        ReflectionUtils.setFieldValue(store, "sessionFactory", sessionFactory);
        Configuration configuration = mock(Configuration.class);
        when(sessionFactory.getConfiguration()).thenReturn(configuration);
        when(configuration.getProperty("xwiki.virtual_mode")).thenReturn("database");

        Statement statement = mock(Statement.class);
        DatabaseProduct databaseProduct = DatabaseProduct.POSTGRESQL;

        store.executeDeleteWikiStatement(statement, databaseProduct, "schema");

        Assert.assertEquals(1, this.logRule.size());
        Assert.assertEquals("Subwiki deletion not yet supported in Database mode for PostgreSQL",
            this.logRule.getMessage(0));
        verify(statement, never()).execute(any(String.class));
    }
}
