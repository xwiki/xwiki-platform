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
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.activeinstalls.internal.client.data.DatabasePingDataProvider;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;

import static org.junit.Assert.*;
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
        Map<String, Object> mapping = this.mocker.getComponentUnderTest().provideMapping();
        assertEquals(2, mapping.size());

        Map<String, Object> propertiesMapping = (Map<String, Object>) mapping.get("dbName");
        assertEquals(2, propertiesMapping.size());
        assertEquals("not_analyzed", propertiesMapping.get("index"));
        assertEquals("string", propertiesMapping.get("type"));

        propertiesMapping = (Map<String, Object>) mapping.get("dbName");
        assertEquals(2, propertiesMapping.size());
        assertEquals("not_analyzed", propertiesMapping.get("index"));
        assertEquals("string", propertiesMapping.get("type"));
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

        Map<String, Object> data = this.mocker.getComponentUnderTest().provideData();
        assertEquals(2, data.size());
        assertEquals("HSQL Database Engine", data.get("dbName"));
        assertEquals("2.2.9", data.get("dbVersion"));
    }
}
