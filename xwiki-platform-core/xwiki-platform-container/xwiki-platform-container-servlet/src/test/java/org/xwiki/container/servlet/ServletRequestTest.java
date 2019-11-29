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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ServletRequest}.
 *
 * @version $Id$
 * @since 3.2M3
 */
@ExtendWith(MockitoExtension.class)
public class ServletRequestTest
{
    @Mock
    private HttpServletRequest httpRequest;

    @Test
    public void getPropertyWhenNoExistAsRequestParam()
    {
        when(this.httpRequest.getParameter("key")).thenReturn("value");

        ServletRequest request = new ServletRequest(this.httpRequest);
        assertEquals("value", request.getProperty("key"));
    }

    @Test
    public void getPropertyWhenNoExistAsAttributeParam()
    {
        when(this.httpRequest.getParameter("key")).thenReturn(null);
        when(this.httpRequest.getAttribute("key")).thenReturn("value");

        ServletRequest request = new ServletRequest(this.httpRequest);
        assertEquals("value", request.getProperty("key"));
    }

    @Test
    public void getPropertiesWhenNoConflict()
    {
        when(this.httpRequest.getParameterValues("key")).thenReturn(new String[]{ "value1", "value2" });
        when(this.httpRequest.getAttribute("key")).thenReturn("value3");

        ServletRequest request = new ServletRequest(this.httpRequest);
        List<Object> values = request.getProperties("key");
        assertEquals(Arrays.asList("value1", "value2", "value3"), values);
    }

    @Test
    public void getPropertiesWhenConflict()
    {
        when(this.httpRequest.getParameterValues("key")).thenReturn(new String[]{ "value" });
        when(this.httpRequest.getAttribute("key")).thenReturn("value");

        ServletRequest request = new ServletRequest(this.httpRequest);
        List<Object> values = request.getProperties("key");
        assertEquals(Arrays.asList("value", "value"), values);
    }

    @Test
    public void getPropertiesWhenNoExistAsRequestParam()
    {
        when(this.httpRequest.getParameterValues("key")).thenReturn(null);
        when(this.httpRequest.getAttribute("key")).thenReturn("value");

        ServletRequest request = new ServletRequest(this.httpRequest);
        List<Object> result = request.getProperties("key");
        assertEquals(1, result.size());
        assertEquals("value", result.get(0));
    }

    @Test
    public void getPropertiesWhenNoValueSetAsRequestAttribute()
    {
        when(this.httpRequest.getParameterValues("key")).thenReturn(new String[]{ "value" });
        when(this.httpRequest.getAttribute("key")).thenReturn(null);

        ServletRequest request = new ServletRequest(this.httpRequest);
        List<Object> result = request.getProperties("key");
        assertEquals(1, result.size());
        assertEquals("value", result.get(0));
    }
}
