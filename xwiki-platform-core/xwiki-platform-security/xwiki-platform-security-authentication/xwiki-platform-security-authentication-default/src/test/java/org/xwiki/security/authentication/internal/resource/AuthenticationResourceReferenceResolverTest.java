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
package org.xwiki.security.authentication.internal.resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.security.authentication.AuthenticationAction;
import org.xwiki.security.authentication.AuthenticationResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AuthenticationResourceReferenceResolver}.
 *
 * @version $Id$
 */
@ComponentTest
class AuthenticationResourceReferenceResolverTest
{
    @InjectMockComponents
    private AuthenticationResourceReferenceResolver resolver;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(this.context);
    }

    @Test
    void resolve() throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        WikiReference currentWiki = new WikiReference("current");
        when(this.context.getMainXWiki()).thenReturn("current");
        Map parameters = new HashMap<>();
        parameters.put("key1", Collections.singletonList("value1"));
        parameters.put("key2", Arrays.asList("value2_a", "value2_b"));

        ExtendedURL extendedURL = new ExtendedURL(Collections.singletonList("resetpassword"), parameters);
        AuthenticationResourceReference resourceReference =
            this.resolver.resolve(extendedURL, AuthenticationResourceReference.TYPE, parameters);

        AuthenticationResourceReference expectedReference = new AuthenticationResourceReference(
            currentWiki,
            AuthenticationAction.RESET_PASSWORD);
        expectedReference.addParameter("key1", Collections.singletonList("value1"));
        expectedReference.addParameter("key2", Arrays.asList("value2_a", "value2_b"));

        assertEquals(expectedReference, resourceReference);

        extendedURL = new ExtendedURL(Arrays.asList("wiki", "foo", "retrieveusername"), parameters);
        resourceReference =
            this.resolver.resolve(extendedURL, AuthenticationResourceReference.TYPE, parameters);

        expectedReference = new AuthenticationResourceReference(
            new WikiReference("foo"),
            AuthenticationAction.RETRIEVE_USERNAME);
        expectedReference.addParameter("key1", Collections.singletonList("value1"));
        expectedReference.addParameter("key2", Arrays.asList("value2_a", "value2_b"));

        assertEquals(expectedReference, resourceReference);
    }

    @Test
    void resolveBadAction()
    {
        when(this.context.getMainXWiki()).thenReturn("current");
        ExtendedURL extendedURL = new ExtendedURL(Collections.singletonList("foobar"), Collections.emptyMap());
        CreateResourceReferenceException createResourceReferenceException =
            assertThrows(CreateResourceReferenceException.class,
                () -> this.resolver.resolve(extendedURL, AuthenticationResourceReference.TYPE, Collections.emptyMap()));

        assertEquals("Cannot find an authentication action for name [foobar]",
            createResourceReferenceException.getMessage());

        ExtendedURL extendedURL2 = new ExtendedURL(Arrays.asList("wiki", "foo", "foobar"), Collections.emptyMap());
        createResourceReferenceException =
            assertThrows(CreateResourceReferenceException.class,
                () -> this.resolver.resolve(extendedURL2,
                    AuthenticationResourceReference.TYPE, Collections.emptyMap()));

        assertEquals("Cannot find an authentication action for name [foobar]",
            createResourceReferenceException.getMessage());
    }

    @Test
    void resolveBadURL()
    {
        when(this.context.getMainXWiki()).thenReturn("current");
        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("authenticate", "foobar"));
        CreateResourceReferenceException createResourceReferenceException =
            assertThrows(CreateResourceReferenceException.class,
                () -> this.resolver.resolve(extendedURL, AuthenticationResourceReference.TYPE, Collections.emptyMap()));

        assertEquals("Invalid Authentication URL format: [/authenticate/foobar]",
            createResourceReferenceException.getMessage());
    }
}
