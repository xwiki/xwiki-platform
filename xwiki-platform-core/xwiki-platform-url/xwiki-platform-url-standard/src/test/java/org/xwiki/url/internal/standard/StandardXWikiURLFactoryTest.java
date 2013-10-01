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
package org.xwiki.url.internal.standard;

import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.EntityResource;
import org.xwiki.resource.ResourceFactory;
import org.xwiki.resource.UnsupportedResourceException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.internal.ExtendedURL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StandardResourceFactory}.
 *
 * @version $Id$
 * @since 5.2M1
 */
public class StandardXWikiURLFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<StandardResourceFactory> mocker =
        new MockitoComponentMockingRule(StandardResourceFactory.class);

    @Test
    public void createResourceWhenBinPathSegment() throws Exception
    {
        URL url = new URL("http://localhost:8080/xwiki/bin/view/Space/Page");
        Map<String, Object> parameters = Collections.singletonMap("ignorePrefix", (Object) "xwiki");

        this.mocker.getComponentUnderTest().createResource(url, parameters);

        // Verify the Entity URL Factory is called and with the proper parameters
        ParameterizedType TYPE_ENTITY_EXTENDED_URL = new DefaultParameterizedType(null, ResourceFactory.class,
            ExtendedURL.class, EntityResource.class);
        ResourceFactory<ExtendedURL, EntityResource> factory =
            this.mocker.getInstance(TYPE_ENTITY_EXTENDED_URL, "standard");
        verify(factory).createResource(any(ExtendedURL.class), anyMap());
    }
}
