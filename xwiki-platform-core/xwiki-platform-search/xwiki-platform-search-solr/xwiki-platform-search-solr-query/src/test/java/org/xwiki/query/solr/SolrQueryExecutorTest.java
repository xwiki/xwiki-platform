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
package org.xwiki.query.solr;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQueryExecutorManager;
import org.xwiki.query.internal.DefaultQueryManager;
import org.xwiki.query.solr.internal.SolrQueryExecutor;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Basic test for the {@link SolrQueryExecutor}.
 * 
 * @version $Id$
 */
@ComponentList({DefaultQueryManager.class, DefaultQueryExecutorManager.class})
public class SolrQueryExecutorTest
{
    @Rule
    public final MockitoComponentMockingRule<QueryExecutor> componentManager =
        new MockitoComponentMockingRule<QueryExecutor>(SolrQueryExecutor.class);

    @Test
    public void testExecutorRegistration() throws Exception
    {
        QueryManager queryManager = this.componentManager.getInstance(QueryManager.class);

        Assert.assertTrue(queryManager.getLanguages().contains(SolrQueryExecutor.SOLR));
    }
}
