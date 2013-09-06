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

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

import javax.inject.Provider;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.query.internal.DefaultQueryExecutorManager;
import org.xwiki.query.internal.DefaultQueryManager;
import org.xwiki.query.solr.internal.SolrQueryExecutor;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Basic test for the {@link SolrQueryExecutor}.
 * 
 * @version $Id$
 */
@ComponentList({DefaultQueryManager.class, DefaultQueryExecutorManager.class})
public class SolrQueryExecutorTest
{

    private static final String   ITERABLE_PARAM_NAME = "multiParam";
    private static final String[] ITERABLE_PARAM_EXPECTED = {"value1", "value2"};
    private static final Iterable ITERABLE_PARAM_VALUE = Arrays.asList(ITERABLE_PARAM_EXPECTED);

    private static final String   INT_ARR_PARAM_NAME = "intArrayParam";
    private static final String[] INT_ARR_PARAM_EXPECTED = {"-42", "4711"};
    private static final int[]    INT_ARR_PARAM_VALUE = {-42, 4711};

    private static final String   STR_ARR_PARAM_NAME = "stringArrayParam";
    private static final String[] STR_ARR_PARAM_EXPECTED = {"valueA", "valueB"};
    private static final String[] STR_ARR_PARAM_VALUE = STR_ARR_PARAM_EXPECTED;

    private static final String SINGLE_PARAM_NAME = "singleParam";
    private static final Object SINGLE_PARAM_VALUE = new Object();
    private static final Object SINGLE_PARAM_EXPECTED = SINGLE_PARAM_VALUE.toString();

    @Rule
    public final MockitoComponentMockingRule<QueryExecutor> componentManager =
        new MockitoComponentMockingRule<QueryExecutor>(SolrQueryExecutor.class);

    @Test
    public void testExecutorRegistration() throws Exception
    {
        QueryManager queryManager = this.componentManager.getInstance(QueryManager.class);

        Assert.assertTrue(queryManager.getLanguages().contains(SolrQueryExecutor.SOLR));
    }

    @Test
    public void testMultiValuedQueryArgs() throws Exception
    {
        SolrInstance solr = mock(SolrInstance.class);
        when(solr.query(any(SolrQuery.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                SolrQuery solrQuery = (SolrQuery) invocation.getArguments()[0];

                Assert.assertArrayEquals(ITERABLE_PARAM_EXPECTED, solrQuery.getParams(ITERABLE_PARAM_NAME));
                Assert.assertArrayEquals(INT_ARR_PARAM_EXPECTED,  solrQuery.getParams(INT_ARR_PARAM_NAME));
                Assert.assertArrayEquals(STR_ARR_PARAM_EXPECTED,  solrQuery.getParams(STR_ARR_PARAM_NAME));
                Assert.assertEquals(SINGLE_PARAM_EXPECTED, solrQuery.get(SINGLE_PARAM_NAME));

                QueryResponse r = mock(QueryResponse.class);
                when(r.getResults()).thenReturn(new SolrDocumentList());
                return r;
            }
        });

        ParameterizedType solrProviderType = new DefaultParameterizedType(null, Provider.class, SolrInstance.class);
        Provider<SolrInstance> provider = componentManager.getInstance(solrProviderType);
        when(provider.get()).thenReturn(solr);

        DefaultQuery query = new DefaultQuery("TestQuery", null);
        query.bindValue(ITERABLE_PARAM_NAME, ITERABLE_PARAM_VALUE);
        query.bindValue(INT_ARR_PARAM_NAME,  INT_ARR_PARAM_VALUE);
        query.bindValue(STR_ARR_PARAM_NAME,  STR_ARR_PARAM_VALUE);
        query.bindValue(SINGLE_PARAM_NAME,   SINGLE_PARAM_VALUE);

        componentManager.getComponentUnderTest().execute(query);
    }
}
