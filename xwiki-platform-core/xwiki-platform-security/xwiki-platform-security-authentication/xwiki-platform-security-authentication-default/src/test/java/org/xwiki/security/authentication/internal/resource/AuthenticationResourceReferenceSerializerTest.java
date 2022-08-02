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

import org.apache.commons.collections.map.HashedMap;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.security.authentication.AuthenticationAction;
import org.xwiki.security.authentication.AuthenticationResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.url.ExtendedURL;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link AuthenticationResourceReferenceSerializer}.
 *
 * @version $Id$
 */
@ComponentTest
class AuthenticationResourceReferenceSerializerTest
{
    @InjectMockComponents
    private AuthenticationResourceReferenceSerializer serializer;

    @Test
    void serialize() throws UnsupportedResourceReferenceException, SerializeResourceReferenceException
    {
        WikiReference currentWiki = new WikiReference("current");
        AuthenticationResourceReference resourceReference = new AuthenticationResourceReference(
            currentWiki,
            AuthenticationAction.RETRIEVE_USERNAME);
        resourceReference.addParameter("key1", "value1");
        resourceReference.addParameter("key2", Arrays.asList("value2_a", "value2_b"));

        ExtendedURL serialized = this.serializer.serialize(resourceReference);

        Map<String, List<String>> parameters = new HashedMap();
        parameters.put("key1", Collections.singletonList("value1"));
        parameters.put("key2", Arrays.asList("value2_a", "value2_b"));
        ExtendedURL expectedURL = new ExtendedURL(
            Arrays.asList("authenticate", "wiki", "current", "retrieveusername"), parameters);

        assertEquals(expectedURL, serialized);
    }
}
