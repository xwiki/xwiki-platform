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
package org.xwiki.internal.migration;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.security.internal.XWikiLegacyPasswordEncoder;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiCacheStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link R180800000XWIKI24357DataMigration}.
 *
 * @version $Id$
 */
@ComponentTest
class R180800000XWIKI24357DataMigrationTest
{
    private static final String LEGACY_PASSWORD = "hash:SHA-512:abcd:0123456789abcdef";

    @InjectMockComponents(role = HibernateDataMigration.class)
    private R180800000XWIKI24357DataMigration dataMigration;

    @MockComponent
    private Execution execution;

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private XWikiContext context;

    private XWiki wiki;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(this.context);
        this.wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.wiki);
    }

    @Test
    void shouldExecute()
    {
        assertTrue(this.dataMigration.shouldExecute(new XWikiDBVersion(171013000)));
        assertTrue(this.dataMigration.shouldExecute(new XWikiDBVersion(171014000)));
        assertFalse(this.dataMigration.shouldExecute(new XWikiDBVersion(171014001)));
        assertFalse(this.dataMigration.shouldExecute(new XWikiDBVersion(171015000)));
        assertTrue(this.dataMigration.shouldExecute(new XWikiDBVersion(180000000)));
        assertTrue(this.dataMigration.shouldExecute(new XWikiDBVersion(180404000)));
        assertFalse(this.dataMigration.shouldExecute(new XWikiDBVersion(180405000)));
        assertFalse(this.dataMigration.shouldExecute(new XWikiDBVersion(180407000)));
        assertTrue(this.dataMigration.shouldExecute(new XWikiDBVersion(180500000)));
        assertTrue(this.dataMigration.shouldExecute(new XWikiDBVersion(180700000)));
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void migrate() throws Exception
    {
        XWikiCacheStore store = mock(XWikiCacheStore.class);
        when(this.wiki.getStore()).thenReturn(store);
        QueryManager queryManager = mock(QueryManager.class);
        when(store.getQueryManager()).thenReturn(queryManager);
        XWikiHibernateStore hibernateStore = mock(XWikiHibernateStore.class);
        when(this.wiki.getHibernateStore()).thenReturn(hibernateStore);

        Query countQuery = mock(Query.class);
        when(queryManager.createQuery("select count(*) from PasswordProperty where length(value) > 0", Query.HQL))
            .thenReturn(countQuery);
        when(countQuery.execute()).thenReturn(List.of(2L));

        Query batchQuery = mock(Query.class);
        when(queryManager.createQuery("select id.id, id.name, value from PasswordProperty where length(value) > 0 "
            + "order by id", Query.HQL)).thenReturn(batchQuery);
        when(batchQuery.setLimit(100)).thenReturn(batchQuery);
        when(batchQuery.setOffset(anyInt())).thenReturn(batchQuery);
        List<Object> passwords = new ArrayList<>();
        passwords.add(new Object[] { 1L, "password", LEGACY_PASSWORD });
        // Already using a non-legacy format: not re-encoded.
        passwords.add(new Object[] { 2L, "password", "{argon2}$argon2id$v=19$m=16384,t=2,p=1$abc$def" });
        when(batchQuery.execute()).thenReturn(passwords, List.of());

        when(hibernateStore.executeWrite(any(XWikiContext.class), any(HibernateCallback.class))).thenReturn(1);

        this.dataMigration.migrate();

        // Only the legacy password is re-encoded.
        ArgumentCaptor<HibernateCallback<Integer>> callbackCaptor = ArgumentCaptor.forClass(HibernateCallback.class);
        verify(hibernateStore).executeWrite(any(XWikiContext.class), callbackCaptor.capture());
        Session session = mock(Session.class);
        org.hibernate.query.Query updateQuery = mock(org.hibernate.query.Query.class);
        when(session.createQuery(anyString())).thenReturn(updateQuery);
        when(updateQuery.setParameter(anyString(), any())).thenReturn(updateQuery);
        callbackCaptor.getValue().doInHibernate(session);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(updateQuery).setParameter(eq("value"), valueCaptor.capture());
        verify(updateQuery).setParameter("id", 1L);
        verify(updateQuery).setParameter("name", "password");

        // The algorithm and the salt are kept to be able to compute the legacy hash, which is hashed with argon2.
        String prefix = "{" + XWikiLegacyPasswordEncoder.ALGORITHM_ID + "}SHA-512:abcd:";
        String newValue = (String) valueCaptor.getValue();
        assertTrue(newValue.startsWith(prefix), newValue);
        assertTrue(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
            .matches(LEGACY_PASSWORD, newValue.substring(prefix.length())));

        verify(hibernateStore).beginTransaction(this.context);
        verify(hibernateStore).endTransaction(this.context, true);
        // The cached documents must not be saved back with their former password values.
        verify(store).flushCache();

        assertEquals(3, this.logCapture.size());
        assertEquals("Found [2] passwords to check for possible re-hashing.", this.logCapture.getMessage(0));
        assertEquals("Processing a batch of [2] passwords.", this.logCapture.getMessage(1));
        assertEquals("[1] passwords updated.", this.logCapture.getMessage(2));
    }
}
