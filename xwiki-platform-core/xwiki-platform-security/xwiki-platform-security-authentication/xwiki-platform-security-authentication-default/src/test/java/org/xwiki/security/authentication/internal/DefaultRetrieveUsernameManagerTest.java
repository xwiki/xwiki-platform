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
package org.xwiki.security.authentication.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.Test;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authentication.RetrieveUsernameException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultRetrieveUsernameManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultRetrieveUsernameManagerTest
{
    private static final String HQL_QUERY = ", BaseObject obj, StringProperty prop where obj.name = doc.fullName and "
        + "obj.className = 'XWiki.XWikiUsers' and prop.id.id = obj.id and prop.id.name = 'email' "
        + "and LOWER(prop.value) = :email";

    @InjectMockComponents
    private DefaultRetrieveUsernameManager retrieveUsernameManager;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private UserReferenceResolver<String> userReferenceResolver;

    @MockComponent
    private AuthenticationMailSender authenticationMailSender;

    @Test
    void findUsers() throws QueryException, RetrieveUsernameException
    {
        String email = "foo@bar.com";

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(HQL_QUERY, Query.HQL)).thenReturn(query);
        when(query.bindValue("email", email)).thenReturn(query);

        when(query.execute()).thenReturn(Arrays.asList("User1", "User2"));
        UserReference userReference1 = mock(UserReference.class, "user1");
        UserReference userReference2 = mock(UserReference.class, "user2");
        UserReference userReference3 = mock(UserReference.class, "user3");

        when(this.userReferenceResolver.resolve("User1")).thenReturn(userReference1);
        when(this.userReferenceResolver.resolve("User2")).thenReturn(userReference2);
        when(this.userReferenceResolver.resolve("User3")).thenReturn(userReference3);

        Set<UserReference> expectedSet = new HashSet<>(Arrays.asList(userReference1, userReference2));
        assertEquals(expectedSet, this.retrieveUsernameManager.findUsers(email));
        verify(this.queryManager).createQuery(HQL_QUERY, Query.HQL);

        when(query.execute()).thenReturn(Collections.emptyList());
        Query globalQuery = mock(Query.class, "globalQuery");
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");
        when(query.setWiki("mainWiki")).thenReturn(globalQuery);

        when(globalQuery.execute()).thenReturn(Collections.singletonList("User3"));
        assertEquals(Collections.singleton(userReference3), this.retrieveUsernameManager.findUsers(email));

        // 1 time for previous check
        // 1 time for checking on current wiki
        // 1 time for checking on main wiki
        verify(this.queryManager, times(3)).createQuery(HQL_QUERY, Query.HQL);
    }

    @Test
    void sendRetrieveUsernameEmail() throws RetrieveUsernameException, AddressException
    {
        RetrieveUsernameException exception = assertThrows(RetrieveUsernameException.class, () ->
            this.retrieveUsernameManager.sendRetrieveUsernameEmail("",
                Collections.singleton(mock(UserReference.class))));

        assertTrue(exception.getCause() instanceof AddressException);

        exception = assertThrows(RetrieveUsernameException.class, () ->
            this.retrieveUsernameManager.sendRetrieveUsernameEmail("foo@bar.com", Collections.emptySet()));

        assertNull(exception.getCause());
        assertEquals("The list of user is empty.", exception.getMessage());

        Set<UserReference> userReferences = new HashSet<>(Arrays.asList(
            mock(UserReference.class),
            mock(UserReference.class)
        ));
        this.retrieveUsernameManager.sendRetrieveUsernameEmail("foo@bar.com", userReferences);
        verify(this.authenticationMailSender)
            .sendRetrieveUsernameEmail(new InternetAddress("foo@bar.com"), userReferences);
    }
}