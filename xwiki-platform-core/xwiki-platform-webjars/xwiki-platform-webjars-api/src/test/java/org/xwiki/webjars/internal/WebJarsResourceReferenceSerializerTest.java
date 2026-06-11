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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WebjarsResourceReferenceSerializer}.
 *
 * @version $Id$
 * @since 7.1M1
 */
@ComponentTest
class WebJarsResourceReferenceSerializerTest
{
    @InjectMockComponents
    private WebjarsResourceReferenceSerializer serializer;

    @MockComponent
    @Named("contextpath")
    private URLNormalizer<ExtendedURL> normalizer;

    @Test
    void serialize() throws Exception
    {
        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put("key1", List.of("value1"));
        parameters.put("key2", List.of("value2", "value3"));
        ExtendedURL partialURL = new ExtendedURL(List.of("webjars", "namespace", "one", "two"), parameters);
        ExtendedURL expectedURL = new ExtendedURL(
            List.of("xwiki", "webjars", "namespace", "one", "two"), parameters);
        when(this.normalizer.normalize(partialURL)).thenReturn(expectedURL);

        WebJarsResourceReference reference = new WebJarsResourceReference("namespace", List.of("one", "two"));
        reference.addParameter("key1", "value1");
        reference.addParameter("key2", new String[]{ "value2", "value3" });

        assertEquals(expectedURL, this.serializer.serialize(reference));
    }
}
