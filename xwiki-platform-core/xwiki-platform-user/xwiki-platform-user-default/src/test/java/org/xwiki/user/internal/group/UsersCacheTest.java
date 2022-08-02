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
package org.xwiki.user.internal.group;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.internal.MapCache;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link UsersCache}.
 * 
 * @version $Id$
 */
@ComponentTest
@ReferenceComponentList
public class UsersCacheTest
{
    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private QueryManager queryManager;

    @InjectMockComponents
    private UsersCache cache;

    private List<String> USERS_STRING_ACTIVE = Arrays.asList("XWiki.User1", "XWiki.User3");

    private List<DocumentReference> USERS_REF_ACTIVE =
        Arrays.asList(new DocumentReference("wiki", "XWiki", "User1"), new DocumentReference("wiki", "XWiki", "User3"));

    private List<String> USERS_STRING_ALL = Arrays.asList("XWiki.User1", "XWiki.User2", "XWiki.User3");

    private List<DocumentReference> USERS_REF_ALL = Arrays.asList(new DocumentReference("wiki", "XWiki", "User1"),
        new DocumentReference("wiki", "XWiki", "User2"), new DocumentReference("wiki", "XWiki", "User3"));

    @BeforeComponent
    public void beforeComponent() throws CacheException
    {
        when(this.cacheManager.<List<DocumentReference>>createNewCache(any())).thenReturn(new MapCache<>());
    }

    private void mockQuery(String statement, List<String> users) throws QueryException
    {
        Query query = mock(Query.class);
        when(query.<String>execute()).thenReturn(users);

        when(this.queryManager.createQuery(statement, Query.XWQL)).thenReturn(query);
    }

    private void mockUsers(boolean activeOnly, List<String> users) throws QueryException
    {
        StringBuilder statement =
            new StringBuilder("select distinct doc.fullName from Document doc, doc.object(XWiki.XWikiUsers) as user");

        if (activeOnly) {
            statement.append(" where user.active = 1");
        }

        mockQuery(statement.toString(), users);
    }

    public void assertGetCache(List<DocumentReference> expected, boolean activeOnly)
    {
        List<DocumentReference> actual = this.cache.getUsers(new WikiReference("wiki"), activeOnly);

        assertEquals(expected, actual);

        // Make sure the result is cached
        assertSame(actual, this.cache.getUsers(new WikiReference("wiki"), activeOnly));
    }

    // Tests

    @Test
    public void getUsers() throws QueryException
    {
        mockUsers(false, USERS_STRING_ALL);
        mockUsers(true, USERS_STRING_ACTIVE);

        assertGetCache(USERS_REF_ACTIVE, true);
        assertGetCache(USERS_REF_ALL, false);
    }

    @Test
    public void getUsersNoUser() throws QueryException
    {
        mockUsers(true, Collections.emptyList());
        mockUsers(false, Collections.emptyList());

        assertGetCache(Collections.emptyList(), true);
        assertGetCache(Collections.emptyList(), false);
    }
}
