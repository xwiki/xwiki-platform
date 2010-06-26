package org.xwiki.query.internal;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutorManager;

/**
 * Tests for {@link SecureQueryExecutorManager}
 *
 * @version $Id$
 */
@RunWith(JMock.class)
public class SecureQueryExecutorManagerTest
{
    Mockery context = new JUnit4Mockery();

    DocumentAccessBridge dab = context.mock(DocumentAccessBridge.class);

    SecureQueryExecutorManager qem = new SecureQueryExecutorManager()
    {
        @Override
        protected DocumentAccessBridge getBridge()
        {
            return dab;
        }

        @Override
        protected QueryExecutorManager getNestedQueryExecutorManager()
        {
            return new QueryExecutorManager()
            {
                public Set<String> getLanguages()
                {
                    return Collections.emptySet();
                }

                public <T> List<T> execute(Query query) throws QueryException
                {
                    return Collections.emptyList();
                }
            };
        }
    };

    private Query createQuery(String stmt, String lang)
    {
        return new DefaultQuery(stmt, lang, qem);
    }

    private Query createNamedQuery(String name)
    {
        return new DefaultQuery(name, qem);
    }

    @Test
    public void testWithProgrammingRight() throws QueryException
    {
        context.checking(new Expectations()
        {{
                allowing(dab).hasProgrammingRights();
                will(returnValue(true));
            }});
        // all queries allowed
        createQuery("where doc.space='Main'", "xwql").execute();
        createQuery("from doc.objects(XWiki.XWikiUsers) as u", "xwql").execute();
        createQuery("select u from Document as doc, doc.objects(XWiki.XWikiUsers) as u", "xwql").execute();
        createQuery("some hql query", "hql").execute();
        createQuery("where doc.space='Main'", "xwql").setWiki("somewiki").execute();
        createNamedQuery("somename").execute();
    }

    @Test
    public void testWithoutProgrammingRight() throws QueryException
    {
        context.checking(new Expectations()
        {{
                allowing(dab).hasProgrammingRights();
                will(returnValue(false));
            }});
        createQuery("where doc.space='WebHome'", "xwql").execute(); // OK
        createQuery("from doc.objects(XWiki.XWikiUsers) as u", "xwql").execute(); // OK
        try {
            createQuery("select u from Document as doc, doc.objects(XWiki.XWikiUsers) as u", "xwql").execute();
            fail("full form xwql should not allowed");
        } catch (QueryException e) {
        }
        try {
            createQuery("some hql query", "hql").execute();
            fail("hql should not allowed");
        } catch (QueryException e) {
        }
        try {
            createQuery("where doc.space='Main'", "xwql").setWiki("somewiki").execute();
            fail("Query#setWiki should not allowed");
        } catch (QueryException e) {
        }
        try {
            createNamedQuery("somename").execute();
            fail("named queries should not allowed");
        } catch (QueryException e) {
        }
        ;
    }
}
