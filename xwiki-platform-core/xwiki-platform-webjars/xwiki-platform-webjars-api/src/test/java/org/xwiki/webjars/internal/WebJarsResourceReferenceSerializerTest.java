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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WebjarsResourceReferenceSerializer}.
 *
 * @version $Id$
 * @since 7.1M1
 */
public class WebJarsResourceReferenceSerializerTest
{
    @Rule
    public MockitoComponentMockingRule<WebjarsResourceReferenceSerializer> mocker =
        new MockitoComponentMockingRule<>(WebjarsResourceReferenceSerializer.class);

    @Test
    public void serialize() throws Exception
    {
        URLNormalizer<ExtendedURL> normalizer = this.mocker.getInstance(
            new DefaultParameterizedType(null, URLNormalizer.class, ExtendedURL.class), "contextpath");
        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put("key1", Arrays.asList("value1"));
        parameters.put("key2", Arrays.asList("value2", "value3"));
        ExtendedURL partialURL = new ExtendedURL(Arrays.asList("webjars", "namespace", "one", "two"), parameters);
        ExtendedURL expectedURL = new ExtendedURL(
            Arrays.asList("xwiki", "webjars", "namespace", "one", "two"), parameters);
        when(normalizer.normalize(partialURL)).thenReturn(expectedURL);

        WebJarsResourceReference reference = new WebJarsResourceReference("namespace", Arrays.asList("one", "two"));
        reference.addParameter("key1", "value1");
        reference.addParameter("key2", new String[]{ "value2", "value3" });

        assertEquals(expectedURL, this.mocker.getComponentUnderTest().serialize(reference));
    }
}
