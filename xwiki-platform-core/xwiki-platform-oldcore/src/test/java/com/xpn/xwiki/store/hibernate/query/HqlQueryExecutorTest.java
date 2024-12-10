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
package com.xpn.xwiki.store.hibernate.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.WrappingQuery;
import org.xwiki.query.hql.internal.DefaultHQLQueryValidator;
import org.xwiki.query.hql.internal.HQLQueryValidator;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HqlQueryExecutor}
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(DefaultHQLQueryValidator.class)
class HqlQueryExecutorTest
{
    @InjectMockComponents
    private HqlQueryExecutor executor;

    @InjectMockComponents
    private DefaultHQLQueryValidator defaultQueryValidator;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private XWikiHibernateStore store;

    @MockComponent
    private HibernateStore hibernateStore;

    @MockComponent
    private Execution execution;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentMannager;

    private boolean hasProgrammingRight;

    @AfterComponent
    public void afterComponent()
    {
        when(this.hibernateStore.getConfiguration()).thenReturn(new Configuration());
        when(this.hibernateStore.getConfigurationMetadata()).thenReturn(mock(Metadata.class));
    }

    @BeforeEach
    public void beforeEach() throws Exception
    {
        when(this.authorization.hasAccess(Right.PROGRAM)).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                return hasProgrammingRight;
            }
        });

        this.hasProgrammingRight = true;

        when(this.contextComponentMannager.getInstanceList(HQLQueryValidator.class))
            .thenReturn(List.of(this.defaultQueryValidator));

        // Actual Hibernate query

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);
        XWikiContext xwikiContext = mock(XWikiContext.class);
        when(executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY)).thenReturn(xwikiContext);
        when(xwikiContext.getWikiId()).thenReturn("currentwikid");

        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xwikiContext.getWiki()).thenReturn(xwiki);
        when(xwiki.getHibernateStore()).thenReturn(this.store);
    }

    private void execute(String statement, Boolean withProgrammingRights) throws QueryException
    {
        this.hasProgrammingRight = withProgrammingRights != null ? withProgrammingRights : true;

        DefaultQuery query = new DefaultQuery(statement, Query.HQL, this.executor);
        if (withProgrammingRights != null) {
            query.checkCurrentAuthor(true);
        }

        this.executor.execute(query);
    }

    private void executeNamed(String name, Boolean withProgrammingRights) throws QueryException
    {
        this.hasProgrammingRight = withProgrammingRights;

        DefaultQuery query = new DefaultQuery(name, this.executor);
        if (withProgrammingRights != null) {
            query.checkCurrentAuthor(true);
        }

        this.executor.execute(query);
    }

    // Tests

    @Test
    void completeShortStatementWhenEmpty()
    {
        assertEquals("select doc.fullName from XWikiDocument doc ", this.executor.completeShortFormStatement(""));
    }

    @Test
    void completeShortStatementStartingWithWhere()
    {
        assertEquals("select doc.fullName from XWikiDocument doc where doc.author='XWiki.Admin'",
            this.executor.completeShortFormStatement("where doc.author='XWiki.Admin'"));
    }

    @Test
    void completeShortStatementStartingWithFrom()
    {
        assertEquals(
            "select doc.fullName from XWikiDocument doc , BaseObject obj where doc.fullName=obj.name "
                + "and obj.className='XWiki.MyClass'",
            this.executor.completeShortFormStatement(
                ", BaseObject obj where " + "doc.fullName=obj.name and obj.className='XWiki.MyClass'"));
    }

    @Test
    void completeShortStatementStartingWithOrderBy()
    {
        assertEquals("select doc.fullName from XWikiDocument doc order by doc.date desc",
            this.executor.completeShortFormStatement("order by doc.date desc"));
    }

    @Test
    void completeShortStatementPassingAnAlreadyCompleteQuery()
    {
        assertEquals("select doc.fullName from XWikiDocument doc order by doc.date desc", this.executor
            .completeShortFormStatement("select doc.fullName from XWikiDocument doc order by doc.date desc"));
    }

    @Test
    void completeShortStatementPassingAQueryOnSomethingElseThanADocument()
    {
        assertEquals("select lock.docId from XWikiLock as lock ",
            this.executor.completeShortFormStatement("select lock.docId from XWikiLock as lock "));
    }

    @Test
    void setNamedParameter()
    {
        org.hibernate.query.Query query = mock(org.hibernate.query.Query.class);
        String name = "abc";
        Date value = new Date();
        this.executor.setNamedParameter(query, name, value);

        verify(query).setParameter(name, value);
    }

    @Test
    void setNamedParameterList()
    {
        org.hibernate.query.Query query = mock(org.hibernate.query.Query.class);
        String name = "foo";
        List<String> value = Arrays.asList("one", "two", "three");
        this.executor.setNamedParameter(query, name, value);

        verify(query).setParameterList(name, value);
    }

    @Test
    void setNamedParameterArray()
    {
        org.hibernate.query.Query query = mock(org.hibernate.query.Query.class);
        String name = "bar";
        Integer[] value = new Integer[] {1, 2, 3};
        this.executor.setNamedParameter(query, name, value);

        verify(query).setParameterList(name, value);
    }

    @Test
    void populateParameters()
    {
        org.hibernate.query.Query hquery = mock(org.hibernate.query.Query.class);
        Query query = mock(Query.class);

        int offset = 13;
        when(query.getOffset()).thenReturn(offset);

        int limit = 7;
        when(query.getLimit()).thenReturn(limit);

        Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("alice", 10);
        List<String> listValue = Collections.singletonList("yellow");
        namedParameters.put("bob", listValue);
        when(query.getNamedParameters()).thenReturn(namedParameters);

        this.executor.populateParameters(hquery, query);

        verify(hquery).setFirstResult(offset);
        verify(hquery).setMaxResults(limit);
        verify(hquery).setParameter("alice", 10);
        verify(hquery).setParameterList("bob", listValue);
    }

    @Test
    void executeWhenStoreException() throws Exception
    {
        XWikiException exception = mock(XWikiException.class);
        when(exception.getMessage()).thenReturn("nestedmessage");

        when(this.store.executeRead(any(XWikiContext.class), any(XWikiHibernateBaseStore.HibernateCallback.class)))
            .thenThrow(exception);

        try {
            execute("statement", null);
            fail("Should have thrown an exception here");
        } catch (QueryException expected) {
            assertEquals("Exception while executing query. Query statement = [statement]", expected.getMessage());
            // Verify nested exception!
            assertEquals("nestedmessage", expected.getCause().getMessage());
        }
    }

    @Test
    void createNamedNativeHibernateQuery() throws Exception
    {
        DefaultQuery query = new DefaultQuery("queryName", this.executor);

        Session session = mock(Session.class);
        NativeQuery sqlQuery = mock(NativeQuery.class);
        when(session.getNamedQuery(query.getStatement())).thenReturn(sqlQuery);
        when(sqlQuery.getQueryString()).thenReturn("foo");

        // Add a Query Filter to verify it's called and can change the statement
        QueryFilter filter = mock(QueryFilter.class);
        query.addFilter(filter);
        when(filter.filterStatement("foo", "sql")).thenReturn("bar");
        when(filter.filterQuery(any(Query.class))).then(returnsFirstArg());

        NativeQuery finalQuery = mock(NativeQuery.class);
        when(session.createSQLQuery("bar")).thenReturn(finalQuery);

        NamedSQLQueryDefinition definition = mock(NamedSQLQueryDefinition.class);
        when(definition.getResultSetRef()).thenReturn("someResultSet");
        when(definition.getName()).thenReturn(query.getStatement());

        when(this.hibernateStore.getConfigurationMetadata().getNamedNativeQueryDefinition(query.getStatement()))
            .thenReturn(definition);

        assertSame(finalQuery, this.executor.createHibernateQuery(session, query));

        verify(finalQuery).setResultSetMapping(definition.getResultSetRef());
    }

    @Test
    void createHibernateQueryWhenFilter() throws Exception
    {
        Session session = mock(Session.class);

        DefaultQuery query = new DefaultQuery("where doc.space='Main'", Query.HQL, this.executor);

        // Add a Query Filter to verify it's called and can change the statement.
        // We also verify that QueryFilter#filterStatement() is called before QueryFilter#filterQuery()
        QueryFilter filter = mock(QueryFilter.class);
        query.addFilter(filter);
        when(filter.filterStatement("select doc.fullName from XWikiDocument doc where doc.space='Main'", Query.HQL))
            .thenReturn("select doc.fullName from XWikiDocument doc where doc.space='Main2'");
        when(filter.filterQuery(any(Query.class))).thenReturn(new WrappingQuery(query)
        {
            @Override
            public String getStatement()
            {
                return "select doc.fullName from XWikiDocument doc where doc.space='Main3'";
            }
        });

        this.executor.createHibernateQuery(session, query);

        // The test is here!
        verify(session).createQuery("select doc.fullName from XWikiDocument doc where doc.space='Main3'");
    }

    @Test
    void createHibernateQueryAutomaticallyAddEscapeLikeParametersFilterWhenQueryParameter() throws Exception
    {
        Session session = mock(Session.class);
        DefaultQuery query = new DefaultQuery("where space like :space", Query.HQL, this.executor);
        query.bindValue("space").literal("test");

        QueryFilter filter = mock(QueryFilter.class);
        when(filter.filterStatement(anyString(), anyString())).then(returnsFirstArg());
        when(filter.filterQuery(any(Query.class))).then(returnsFirstArg());

        when(this.contextComponentMannager.getInstance(QueryFilter.class, "escapeLikeParameters")).thenReturn(filter);

        when(session.createQuery(anyString())).thenReturn(mock(org.hibernate.query.Query.class));

        this.executor.createHibernateQuery(session, query);

        // The test is here! We verify that the filter has been called even though we didn't explicitly add it to the
        // query, i.e. that it's been added automatically
        verify(filter).filterQuery(any(Query.class));
    }

    @Test
    void executeShortWhereHQLQueryWithProgrammingRights() throws QueryException
    {
        execute("where doc.space='Main'", true);
    }

    @Test
    void executeShortFromHQLQueryWithProgrammingRights() throws QueryException
    {
        execute(", BaseObject as obj", true);
    }

    @Test
    void executeCompleteHQLQueryWithProgrammingRights() throws QueryException
    {
        execute("select u from XWikiDocument as doc", true);

    }

    @Test
    void executeNamedQueryWithProgrammingRights() throws QueryException
    {
        executeNamed("somename", true);
    }

    @Test
    void executeShortWhereHQLQueryWithoutProgrammingRights() throws QueryException
    {
        execute("where doc.space='Main'", false);
    }

    @Test
    void executeShortFromHQLQueryWithoutProgrammingRights() throws QueryException
    {
        execute(", BaseObject as obj", false);
    }

    // Not allowed

    @Test
    void executeWhenNotAllowedSelect() throws Exception
    {
        try {
            execute("select notallowed.name from NotAllowedTable notallowed", false);
            fail("Should have thrown an exception here");
        } catch (QueryException expected) {
            assertEquals(
                "The query requires programming right."
                    + " Query statement = [select notallowed.name from NotAllowedTable notallowed]",
                expected.getCause().getMessage());
        }
    }

    @Test
    void executeDeleteWithoutProgrammingRight() throws Exception
    {
        try {
            execute("delete from XWikiDocument as doc", false);
            fail("Should have thrown an exception here");
        } catch (QueryException expected) {
            assertEquals("The query requires programming right. Query statement = [delete from XWikiDocument as doc]",
                expected.getCause().getMessage());
        }
    }

    @Test
    void executeNamedQueryWithoutProgrammingRight() throws Exception
    {
        try {
            executeNamed("somename", false);
            fail("Should have thrown an exception here");
        } catch (QueryException expected) {
            assertEquals("Named queries requires programming right. Named query = [somename]",
                expected.getCause().getMessage());
        }
    }

    @Test
    void executeUpdateWithoutProgrammingRight() throws Exception
    {
        try {
            execute("update XWikiDocument set name='name'", false);
            fail("Should have thrown an exception here");
        } catch (QueryException expected) {
            assertEquals(
                "The query requires programming right. Query statement = [update XWikiDocument set name='name']",
                expected.getCause().getMessage());
        }
    }
}
