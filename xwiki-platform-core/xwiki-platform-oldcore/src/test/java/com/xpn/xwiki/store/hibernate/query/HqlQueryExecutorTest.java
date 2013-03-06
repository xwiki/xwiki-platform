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

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import org.hibernate.cfg.Configuration;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.query.QueryExecutor;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

/**
 * Tests for {@link HqlQueryExecutor}
 *
 * @version $Id$
 */
@MockingRequirement(HqlQueryExecutor.class)
public class HqlQueryExecutorTest extends AbstractMockingComponentTestCase<HqlQueryExecutor>
{
    private HqlQueryExecutor executor;

    HibernateSessionFactory sessionFactory;

    @Before
    public void configure() throws Exception
    {
        sessionFactory = getComponentManager().getInstance(HibernateSessionFactory.class);

        getMockery().checking(new Expectations() {{
            allowing(sessionFactory).getConfiguration();
            will(returnValue(new Configuration()));
        }});

        this.executor = getComponentManager().getInstance(QueryExecutor.class, "hql");
    }

    @Test
    public void completeShortStatementStartingWithWhere()
    {
        Assert.assertEquals("select doc.fullName from XWikiDocument doc where doc.author='XWiki.Admin'",
            executor.completeShortFormStatement("where doc.author='XWiki.Admin'"));
    }


    @Test
    public void completeShortStatementStartingWithFrom()
    {
        Assert.assertEquals("select doc.fullName from XWikiDocument doc , BaseObject obj where doc.fullName=obj.name "
            + "and obj.className='XWiki.MyClass'", executor.completeShortFormStatement(", BaseObject obj where "
            + "doc.fullName=obj.name and obj.className='XWiki.MyClass'"));
    }

    @Test
    public void completeShortStatementStartingWithOrderBy()
    {
        Assert.assertEquals("select doc.fullName from XWikiDocument doc order by doc.date desc",
            executor.completeShortFormStatement("order by doc.date desc"));
    }

    @Test
    public void completeShortStatementPassingAnAlreadyCompleteQuery()
    {
        Assert.assertEquals("select doc.fullName from XWikiDocument doc order by doc.date desc",
            executor.completeShortFormStatement("select doc.fullName from XWikiDocument doc order by doc.date desc"));
    }

    @Test
    public void completeShortStatementPassingAQueryOnSomethingElseThanADocument()
    {
        Assert.assertEquals("select lock.docId from XWikiLock as lock ",
            executor.completeShortFormStatement("select lock.docId from XWikiLock as lock "));
    }
}
