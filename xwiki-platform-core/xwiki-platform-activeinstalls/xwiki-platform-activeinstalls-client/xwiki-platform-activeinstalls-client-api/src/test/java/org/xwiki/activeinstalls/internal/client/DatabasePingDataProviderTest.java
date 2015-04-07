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
package org.xwiki.activeinstalls.internal.client;

import java.sql.DatabaseMetaData;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.activeinstalls.internal.client.data.DatabasePingDataProvider;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;

import net.sf.json.JSONObject;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.activeinstalls.internal.client.data.DatabasePingDataProvider}.
 *
 * @version $Id$
 * @since 6.1M1
 */
public class DatabasePingDataProviderTest
{
    @Rule
    public MockitoComponentMockingRule<DatabasePingDataProvider> mocker =
        new MockitoComponentMockingRule<>(DatabasePingDataProvider.class);

    @Test
    public void provideMapping() throws Exception
    {
        assertEquals("{\"dbName\":{\"index\":\"not_analyzed\",\"type\":\"string\"},"
                + "\"dbVersion\":{\"index\":\"not_analyzed\",\"type\":\"string\"}}",
            JSONObject.fromObject(this.mocker.getComponentUnderTest().provideMapping()).toString()
        );
    }

    @Test
    public void provideData() throws Exception
    {
        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        XWikiContext xwikiContext = mock(XWikiContext.class);
        when(executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY)).thenReturn(xwikiContext);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xwikiContext.getWiki()).thenReturn(xwiki);
        XWikiCacheStoreInterface cacheStore = mock(XWikiCacheStoreInterface.class);
        when(xwiki.getStore()).thenReturn(cacheStore);
        XWikiHibernateStore store = mock(XWikiHibernateStore.class);
        when(cacheStore.getStore()).thenReturn(store);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(store.getDatabaseMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("HSQL Database Engine");
        when(databaseMetaData.getDatabaseProductVersion()).thenReturn("2.2.9");

        assertEquals("{\"dbName\":\"HSQL Database Engine\",\"dbVersion\":\"2.2.9\"}",
            JSONObject.fromObject(this.mocker.getComponentUnderTest().provideData()).toString());
    }
}
