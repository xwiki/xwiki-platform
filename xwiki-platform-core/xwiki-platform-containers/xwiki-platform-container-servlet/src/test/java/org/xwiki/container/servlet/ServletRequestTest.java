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
package org.xwiki.container.servlet;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link ServletRequest}.
 *
 * @version $Id$
 * @since 3.2M3
 */
public class ServletRequestTest
{
    private Mockery mockery = new Mockery();

    @Test
    public void getPropertyWhenNoExistAsRequestParam()
    {
        final HttpServletRequest httpRequest = mockery.mock(HttpServletRequest.class);
        this.mockery.checking(new Expectations() {{
            oneOf(httpRequest).getParameter("key");
                will(returnValue("value"));
        }});

        ServletRequest request = new ServletRequest(httpRequest);
        Assert.assertEquals("value", request.getProperty("key"));
    }

    @Test
    public void getPropertyWhenNoExistAsAttributeParam()
    {
        final HttpServletRequest httpRequest = mockery.mock(HttpServletRequest.class);
        this.mockery.checking(new Expectations() {{
            oneOf(httpRequest).getParameter("key");
                will(returnValue(null));
            oneOf(httpRequest).getAttribute("key");
                will(returnValue("value"));
        }});

        ServletRequest request = new ServletRequest(httpRequest);
        Assert.assertEquals("value", request.getProperty("key"));
    }

    @Test
    public void getPropertiesWhenNoConflict()
    {
        final HttpServletRequest httpRequest = mockery.mock(HttpServletRequest.class);
        this.mockery.checking(new Expectations() {{
            oneOf(httpRequest).getParameterValues("key");
                will(returnValue(new String[] {"value1", "value2"}));
            oneOf(httpRequest).getAttribute("key");
                will(returnValue("value3"));
        }});

        ServletRequest request = new ServletRequest(httpRequest);
        List<Object> values = request.getProperties("key");
        Assert.assertEquals(Arrays.asList("value1", "value2", "value3"), values);
    }

    @Test
    public void getPropertiesWhenConflict()
    {
        final HttpServletRequest httpRequest = mockery.mock(HttpServletRequest.class);
        this.mockery.checking(new Expectations() {{
            oneOf(httpRequest).getParameterValues("key");
                will(returnValue(new String[] {"value"}));
            oneOf(httpRequest).getAttribute("key");
                will(returnValue("value"));
        }});

        ServletRequest request = new ServletRequest(httpRequest);
        List<Object> values = request.getProperties("key");
        Assert.assertEquals(Arrays.asList("value", "value"), values);
    }
}
