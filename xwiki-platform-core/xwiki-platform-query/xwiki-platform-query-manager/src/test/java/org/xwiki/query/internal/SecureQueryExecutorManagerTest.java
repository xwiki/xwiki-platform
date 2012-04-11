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

    @Test
    public void testWithProgrammingRight() throws QueryException
    {
        getMockery().checking(new Expectations() {{
            allowing(dab).hasProgrammingRights();
                will(returnValue(true));
        }});

        // All queries allowed
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
        getMockery().checking(new Expectations() {{
            allowing(dab).hasProgrammingRights();
                will(returnValue(false));
        }});

        createQuery("where doc.space='WebHome'", "xwql").execute(); // OK
        createQuery("from doc.objects(XWiki.XWikiUsers) as u", "xwql").execute(); // OK

        try {
            createQuery("select u from Document as doc, doc.objects(XWiki.XWikiUsers) as u", "xwql").execute();
            fail("full form xwql shouldn't be allowed since the user doesn't have programming rights");
        } catch (QueryException expected) {
        }

        // Make sure leading spaces are ignored when looking for a full form query
        try {
            createQuery(" select u from Document as doc", "xwql").execute();
            fail("full form xwql shouldn't be allowed since the user doesn't have programming rights");
        } catch (QueryException expected) {
        }

        try {
            createQuery("some hql query", "hql").execute();
            fail("hql not allowed since the user doesn't have programming rights");
        } catch (QueryException expected) {
        }

        try {
            createQuery("where doc.space='Main'", "xwql").setWiki("somewiki").execute();
            fail("Query#setWiki shouldn't be allowed since the user doesn't have programming rights");
        } catch (QueryException expected) {
        }

        try {
            createNamedQuery("somename").execute();
            fail("named queries shouldn't be allowed since the user doesn't have programming rights");
        } catch (QueryException expected) {
        }
    }

    private Query createQuery(String stmt, String lang)
    {
        return new DefaultQuery(stmt, lang, this.qem);
    }

    private Query createNamedQuery(String name)
    {
        return new DefaultQuery(name, this.qem);
    }
}
