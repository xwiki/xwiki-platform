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
package org.xwiki.query.internal;

import static org.junit.Assert.fail;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutorManager;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Tests for {@link SecureQueryExecutorManager}
 *
 * @version $Id$
 */
public class SecureQueryExecutorManagerTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private SecureQueryExecutorManager qem;

    private DocumentAccessBridge dab;

    @Override
    public void configure() throws Exception
    {
        final QueryExecutorManager nestedQueryExecutorManager =
            getComponentManager().getInstance(QueryExecutorManager.class);
        getMockery().checking(new Expectations() {{
            allowing(nestedQueryExecutorManager).execute(with(any(Query.class)));
        }});

        this.dab = getComponentManager().getInstance(DocumentAccessBridge.class);
    }

    private void setProgrammingRights(final boolean hasProgrammingRights)
    {
        getMockery().checking(new Expectations() {{
            allowing(dab).hasProgrammingRights();
                will(returnValue(hasProgrammingRights));
        }});
    }
    
    private Query createQuery(String stmt, String lang)
    {
        return new DefaultQuery(stmt, lang, this.qem);
    }

    private Query createNamedQuery(String name)
    {
        return new DefaultQuery(name, this.qem);
    }

    @Test
    public void createWhereXWQLQueryWithProgrammingRights() throws QueryException
    {
        setProgrammingRights(true);
        createQuery("where doc.space='Main'", "xwql").execute();
    }

    @Test
    public void createFromXWQLQueryWithProgrammingRights() throws QueryException
    {
        setProgrammingRights(true);
        createQuery("from doc.objects(XWiki.XWikiUsers) as u", "xwql").execute();
    }

    @Test
    public void createCompleteXWQLQueryWithProgrammingRights() throws QueryException
    {
        setProgrammingRights(true);
        createQuery("select u from Document as doc, doc.objects(XWiki.XWikiUsers) as u", "xwql").execute();

    }

    @Test
    public void createWhereHQLQueryWithProgrammingRights() throws QueryException
    {
        setProgrammingRights(true);
        createQuery("where doc.space='Main'", "hql").execute();
    }

    @Test
    public void createFromHQLQueryWithProgrammingRights() throws QueryException
    {
        setProgrammingRights(true);
        createQuery(", BaseObject as obj", "hql").execute();
    }

    @Test
    public void createCompleteHQLQueryWithProgrammingRights() throws QueryException
    {
        setProgrammingRights(true);
        createQuery("select u from XWikiDocument as doc", "hql").execute();

    }

    @Test
    public void createNamedQueryWithProgrammingRights() throws QueryException
    {
        setProgrammingRights(true);
        createNamedQuery("somename").execute();
    }

    @Test
    public void setWikiInQueryWithProgrammingRights() throws QueryException
    {
        setProgrammingRights(true);
        createQuery("", "xwql").setWiki("somewiki").execute();
    }
    
    @Test
    public void createWhereXWQLQueryWithoutProgrammingRights() throws QueryException
    {
        setProgrammingRights(false);
        createQuery("where doc.space='Main'", "xwql").execute();
    }

    @Test
    public void createFromXWQLQueryWithoutProgrammingRights() throws QueryException
    {
        setProgrammingRights(false);
        createQuery("from doc.objects(XWiki.XWikiUsers) as u", "xwql").execute();
    }

    @Test
    public void createCompleteXWQLQueryWithoutProgrammingRights() throws QueryException
    {
        setProgrammingRights(false);
        try {
            createQuery("select u from Document as doc, doc.objects(XWiki.XWikiUsers) as u", "xwql").execute();
            fail("full form statements shouldn't be allowed since the user doesn't have programming rights");
        } catch (QueryException expected) {
        }
    }

    @Test
    public void createWhereHQLQueryWithoutProgrammingRights() throws QueryException
    {
        setProgrammingRights(false);
        createQuery("where doc.space='Main'", "hql").execute();
    }

    @Test
    public void createFromHQLQueryWithoutProgrammingRights() throws QueryException
    {
        setProgrammingRights(false);
        createQuery(", BaseObject as obj", "hql").execute();
    }

    @Test
    public void createCompleteHQLQueryWithoutProgrammingRights() throws QueryException
    {
        setProgrammingRights(false);
        try {
            createQuery("select u from XWikiDocument as doc", "hql").execute();
            createQuery(" select u from XWikiDocument as doc", "hql").execute();
            fail("full form statements shouldn't be allowed since the user doesn't have programming rights");
        } catch (QueryException expected) {
        }
    }

    @Test
    public void createUpdateHQLQueryWithoutProgrammingRights() throws QueryException
    {
        setProgrammingRights(false);
        try {
            createQuery("update u from XWikiDocument as doc", "hql").execute();
            createQuery(" update u from XWikiDocument as doc", "hql").execute();
            fail("full form statements shouldn't be allowed since the user doesn't have programming rights");
        } catch (QueryException expected) {
        }
    }

    @Test
    public void createDeleteHQLQueryWithoutProgrammingRights() throws QueryException
    {
        setProgrammingRights(false);
        try {
            createQuery("delete from XWikiDocument as doc", "hql").execute();
            createQuery(" delete from XWikiDocument as doc", "hql").execute();
            fail("full form statements shouldn't be allowed since the user doesn't have programming rights");
        } catch (QueryException expected) {
        }
    }

    @Test
    public void createNamedQueryWithoutProgrammingRights() throws QueryException
    {
        setProgrammingRights(false);
        try {
            createNamedQuery("somename").execute();
            fail("named queries shouldn't be allowed since the user doesn't have programming rights");
        } catch (QueryException expected) {
        }
    }

    @Test
    public void setWikiInQueryWithoutProgrammingRights() throws QueryException
    {
        setProgrammingRights(false);
        try {
            createQuery("", "xwql").setWiki("somewiki").execute();
            fail("Query#setWiki shouldn't be allowed since the user doesn't have programming rights");
        } catch (QueryException expected) {
        }
    }
}
