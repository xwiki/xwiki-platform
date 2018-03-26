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

import javax.inject.Provider;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.classic.Session;
import org.junit.Before;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Base class for Hibernate store unit tests.
 * 
 * @param <T> the store type
 * @version $Id$
 */
public abstract class AbstractXWikiHibernateStoreTest<T>
{
    /**
     * The mock XWiki context.
     */
    protected XWikiContext xcontext = mock(XWikiContext.class);

    /**
     * The Hibernate session.
     */
    protected Session session = mock(Session.class);

    /**
     * The Hibernate transaction.
     */
    protected Transaction transaction = mock(Transaction.class);

    protected HibernateStore hibernateStore;

    @Before
    public void setUp() throws Exception
    {
        // For XWikiHibernateBaseStore#initialize()

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xcontext);

        Execution execution = getMocker().getInstance(Execution.class);
        when(execution.getContext()).thenReturn(executionContext);

        Provider<XWikiContext> xcontextProvider = getMocker().registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);
        xcontextProvider = getMocker().registerMockComponent(XWikiContext.TYPE_PROVIDER, "readonly");
        when(xcontextProvider.get()).thenReturn(this.xcontext);

        XWiki wiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(wiki);

        // For XWikiHibernateBaseStore#initHibernate()

        HibernateSessionFactory sessionFactory = getMocker().getInstance(HibernateSessionFactory.class);
        when(sessionFactory.getConfiguration()).thenReturn(mock(Configuration.class));

        // For XWikiHibernateBaseStore#beginTransaction()

        SessionFactory wrappedSessionFactory = mock(SessionFactory.class);
        when(sessionFactory.getSessionFactory()).thenReturn(wrappedSessionFactory);
        when(wrappedSessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);

        // HibernateStore
        this.hibernateStore = getMocker().registerMockComponent(HibernateStore.class);

        // Return null on first get to force the session/transaction creation.
        when(this.hibernateStore.getCurrentSession()).thenReturn(session);
        when(this.hibernateStore.getCurrentTransaction()).thenReturn(transaction);

        // Default is schema mode
        when(this.hibernateStore.isInSchemaMode()).thenReturn(true);
    }

    /**
     * @return the component manager
     */
    protected abstract MockitoComponentMockingRule<T> getMocker();
}
