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
package org.xwiki.webjars.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link WebJarsResourceReferenceResolver}.
 *
 * @version $Id$
 * @since 7.1M1
 */
public class WebJarsResourceReferenceResolverTest
{
    @Rule
    public MockitoComponentMockingRule<WebJarsResourceReferenceResolver> mocker =
        new MockitoComponentMockingRule<>(WebJarsResourceReferenceResolver.class);

    @Test
    public void resolve() throws Exception
    {
        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put("key1", Arrays.asList("value1"));
        parameters.put("key2", Arrays.asList("value2", "value3"));
        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("namespace", "one", "two"), parameters);

        WebJarsResourceReference reference = this.mocker.getComponentUnderTest().resolve(extendedURL,
            WebJarsResourceReference.TYPE, Collections.<String, Object>emptyMap());

        assertEquals("namespace", reference.getNamespace());
        assertEquals("one/two", reference.getResourceName());
        assertEquals("value1", reference.getParameterValue("key1"));
        assertEquals(Arrays.asList("value2", "value3"), reference.getParameterValues("key2"));
    }
}
