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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.activeinstalls.internal.client.data.ServletContainerPingDataProvider;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import net.sf.json.JSONObject;

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
        assertEquals("{\"servletContainerVersion\":{\"index\":\"not_analyzed\",\"type\":\"string\"},"
                + "\"servletContainerName\":{\"index\":\"not_analyzed\",\"type\":\"string\"}}",
            JSONObject.fromObject(this.mocker.getComponentUnderTest().provideMapping()).toString()
        );
    }

    @Test
    public void provideData() throws Exception
    {
        Container container = this.mocker.getInstance(Container.class);
        ServletRequest servletRequest = mock(ServletRequest.class);
        when(container.getRequest()).thenReturn(servletRequest);
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHttpServletRequest()).thenReturn(httpServletRequest);
        HttpSession session = mock(HttpSession.class);
        when(httpServletRequest.getSession()).thenReturn(session);
        ServletContext servletContext = mock(ServletContext.class);
        when(session.getServletContext()).thenReturn(servletContext);
        when(servletContext.getServerInfo()).thenReturn("Apache Tomcat/7.0.4 (optional text)");

        assertEquals("{\"servletContainerVersion\":\"7.0.4\",\"servletContainerName\":\"Apache Tomcat\"}",
            JSONObject.fromObject(this.mocker.getComponentUnderTest().provideData()).toString());
    }
}
