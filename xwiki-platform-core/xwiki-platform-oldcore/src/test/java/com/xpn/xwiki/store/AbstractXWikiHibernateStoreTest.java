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

import static org.mockito.Mockito.*;

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
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

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
    protected XWikiContext context = mock(XWikiContext.class);

    /**
     * The Hibernate session.
     */
    protected Session session = mock(Session.class);

    /**
     * The Hibernate transaction.
     */
    protected Transaction transaction = mock(Transaction.class);

    @Before
    public void setUp() throws Exception
    {
        // For XWikiHibernateBaseStore#initialize()

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(context);

        Execution execution = getMocker().getInstance(Execution.class);
        when(execution.getContext()).thenReturn(executionContext);

        XWiki wiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(wiki);

        // For XWikiHibernateBaseStore#initHibernate()

        HibernateSessionFactory sessionFactory = getMocker().getInstance(HibernateSessionFactory.class);
        when(sessionFactory.getConfiguration()).thenReturn(mock(Configuration.class));

        // For XWikiHibernateBaseStore#beginTransaction()

        SessionFactory wrappedSessionFactory = mock(SessionFactory.class);
        when(sessionFactory.getSessionFactory()).thenReturn(wrappedSessionFactory);
        when(wrappedSessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);

        // Return null on first get to force the session/transaction creation.
        when(context.get("hibsession")).thenReturn(null, session);
        when(context.get("hibtransaction")).thenReturn(null, transaction);
    }

    /**
     * @return the component manager
     */
    protected abstract MockitoComponentMockingRule<T> getMocker();
}
