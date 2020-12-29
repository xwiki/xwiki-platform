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
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.apache.commons.collections.map.HashedMap;
import org.junit.jupiter.api.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.security.authentication.api.AuthenticationResourceReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AuthenticationResourceReferenceSerializer}.
 *
 * @version $Id$
 */
@ComponentTest
public class AuthenticationResourceReferenceSerializerTest
{
    @InjectMockComponents
    private AuthenticationResourceReferenceSerializer serializer;

    private XWikiContext xWikiContext;

    @BeforeComponent
    void setup(MockitoComponentManager componentManager) throws Exception
    {
        Provider<XWikiContext> contextProvider = componentManager
            .registerMockComponent(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        this.xWikiContext = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(this.xWikiContext);
        when(this.xWikiContext.getWikiId()).thenReturn("foobar");
    }

    @Test
    void serialize() throws UnsupportedResourceReferenceException, SerializeResourceReferenceException
    {
        AuthenticationResourceReference resourceReference = new AuthenticationResourceReference(
            AuthenticationResourceReference.AuthenticationAction.FORGOT_USERNAME);
        resourceReference.addParameter("key1", "value1");
        resourceReference.addParameter("key2", Arrays.asList("value2_a", "value2_b"));

        ExtendedURL serialized = this.serializer.serialize(resourceReference);

        Map<String, List<String>> parameters = new HashedMap();
        parameters.put("key1", Collections.singletonList("value1"));
        parameters.put("key2", Arrays.asList("value2_a", "value2_b"));
        ExtendedURL expectedURL = new ExtendedURL(Arrays.asList("foobar", "authenticate", "forgot"), parameters);

        assertEquals(expectedURL, serialized);
    }
}
