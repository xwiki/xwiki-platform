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

import static org.hamcrest.core.IsEqual.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.object.HasToString;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.SimpleExpression;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

/**
 * Unit tests for {@link XWikiHibernateRecycleBinStore}.
 * 
 * @version $Id$
 */
public class XWikiHibernateRecycleBinStoreTest
{
    /**
     * A special component manager that mocks automatically all dependencies of the component under test.
     */
    @Rule
    public MockitoComponentMockingRule<XWikiRecycleBinStoreInterface> mocker =
        new MockitoComponentMockingRule<XWikiRecycleBinStoreInterface>(XWikiHibernateRecycleBinStore.class);

    /**
     * The mock XWiki context.
     */
    private XWikiContext context = mock(XWikiContext.class);

    /**
     * The Hibernate session.
     */
    private Session session = mock(Session.class);

    @Before
    public void setUp() throws Exception
    {
        // For XWikiHibernateBaseStore#initialize()

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(context);

        Execution execution = mocker.getInstance(Execution.class);
        when(execution.getContext()).thenReturn(executionContext);

        XWiki wiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(wiki);

        // For XWikiHibernateBaseStore#initHibernate()

        HibernateSessionFactory sessionFactory = mocker.getInstance(HibernateSessionFactory.class);
        when(sessionFactory.getConfiguration()).thenReturn(mock(Configuration.class));

        // For XWikiHibernateBaseStore#beginTransaction()

        Transaction transaction = mock(Transaction.class);
        when(session.beginTransaction()).thenReturn(transaction);

        when(context.get("hibsession")).thenReturn(session);
        when(context.get("hibtransaction")).thenReturn(transaction);
        SessionFactory wrappedSessionFactory = mock(SessionFactory.class);
        when(wrappedSessionFactory.openSession()).thenReturn(session);
        when(sessionFactory.getSessionFactory()).thenReturn(wrappedSessionFactory);
    }

    @Test
    public void getAllDeletedDocuments() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getFullName()).thenReturn("Space.Page");
        when(document.getLanguage()).thenReturn("ro");

        List<XWikiDeletedDocument> deletedVersions =
            Arrays.asList(mock(XWikiDeletedDocument.class, "v1"), mock(XWikiDeletedDocument.class, "v2"));

        Criteria criteria = mock(Criteria.class);
        when(criteria.list()).thenReturn(deletedVersions);
        when(session.createCriteria(XWikiDeletedDocument.class)).thenReturn(criteria);

        assertArrayEquals(deletedVersions.toArray(new XWikiDeletedDocument[2]), mocker.getComponentUnderTest()
            .getAllDeletedDocuments(document, context, true));

        // Too bad the restrictions don't implement equals..
        verify(criteria).add(argThat(new HasToString<SimpleExpression>(equalTo("fullName=Space.Page"))));
        verify(criteria).add(argThat(new HasToString<SimpleExpression>(equalTo("language=ro"))));
        verify(criteria).addOrder(argThat(new HasToString<Order>(equalTo("date desc"))));
    }

    @Test
    public void getAllDeletedDocumentsWhenLanguageIsEmpty() throws Exception
    {
        Criteria criteria = mock(Criteria.class);
        when(session.createCriteria(XWikiDeletedDocument.class)).thenReturn(criteria);

        mocker.getComponentUnderTest().getAllDeletedDocuments(mock(XWikiDocument.class), context, true);

        // Too bad the restrictions don't implement equals..
        verify(criteria).add(argThat(new HasToString<SimpleExpression>(equalTo("fullName=null"))));
        verify(criteria).add(argThat(new HasToString<SimpleExpression>(equalTo("language= or language is null"))));
    }
}
