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
package org.xwiki.activeinstalls.internal.client;

import java.util.Map;

import javax.servlet.ServletContext;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.activeinstalls.internal.client.data.ServletContainerPingDataProvider;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.activeinstalls.internal.client.data.ServletContainerPingDataProvider}.
 *
 * @version $Id$
 * @since 6.1M1
 */
public class ServletContainerPingDataProviderTest
{
    @Rule
    public MockitoComponentMockingRule<ServletContainerPingDataProvider> mocker =
        new MockitoComponentMockingRule<>(ServletContainerPingDataProvider.class);

    @Test
    public void provideMapping() throws Exception
    {
        Map<String, Object> mapping = this.mocker.getComponentUnderTest().provideMapping();
        assertEquals(2, mapping.size());

        Map<String, Object> propertiesMapping = (Map<String, Object>) mapping.get("servletContainerVersion");
        assertEquals(2, propertiesMapping.size());
        assertEquals("not_analyzed", propertiesMapping.get("index"));
        assertEquals("string", propertiesMapping.get("type"));

        propertiesMapping = (Map<String, Object>) mapping.get("servletContainerName");
        assertEquals(2, propertiesMapping.size());
        assertEquals("not_analyzed", propertiesMapping.get("index"));
        assertEquals("string", propertiesMapping.get("type"));
    }

    @Test
    public void provideData() throws Exception
    {
        ServletEnvironment servletEnvironment = mock(ServletEnvironment.class);
        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(), "environment", servletEnvironment);

        ServletContext servletContext = mock(ServletContext.class);
        when(servletEnvironment.getServletContext()).thenReturn(servletContext);
        when(servletContext.getServerInfo()).thenReturn("Apache Tomcat/7.0.4 (optional text)");

        Map<String, Object> data = this.mocker.getComponentUnderTest().provideData();
        assertEquals(2, data.size());
        assertEquals("7.0.4", data.get("servletContainerVersion"));
        assertEquals("Apache Tomcat", data.get("servletContainerName"));
    }
}
