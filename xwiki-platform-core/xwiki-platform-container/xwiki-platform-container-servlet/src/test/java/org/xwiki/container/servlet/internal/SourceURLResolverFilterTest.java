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
package org.xwiki.container.servlet.internal;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.container.servlet.filters.internal.SourceURLResolverFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link SourceURLResolverFilter}.
 * 
 * @version $Id$
 */
class SourceURLResolverFilterTest
{
    private static final String SCHEME = "http";

    private static final String OTHER_SCHEME = "https";

    private static final String SERVER_NAME = "servername";

    private static final String OTHER_SERVER_NAME = "otherservername";

    private static final int SERVER_PORT = 42;

    private static final int OTHER_SERVER_PORT = 43;

    private static final String REQUEST_URI = "/path";

    private static final String OTHER_REQUEST_URI = "/otherpath";

    private static final StringBuffer REQUEST_URL =
        new StringBuffer(SCHEME + "://" + SERVER_NAME + ':' + SERVER_PORT + REQUEST_URI);

    private static final StringBuffer OTHER_REQUEST_URL =
        new StringBuffer(OTHER_SCHEME + "://" + OTHER_SERVER_NAME + ':' + OTHER_SERVER_PORT + OTHER_REQUEST_URI);

    private HttpServletRequest filteredRequest;

    private final SourceURLResolverFilter filter = new SourceURLResolverFilter();

    private final FilterChain filterChain = new FilterChain()
    {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException
        {
            filteredRequest = (HttpServletRequest) request;
        }
    };

    private HttpServletRequest request;

    private ServletResponse response;

    @BeforeEach
    void beforeEach() throws IOException, ServletException
    {
        this.request = mock();
        when(this.request.getScheme()).thenReturn(SCHEME);
        when(this.request.getServerName()).thenReturn(SERVER_NAME);
        when(this.request.getServerPort()).thenReturn(SERVER_PORT);
        when(this.request.getRequestURI()).thenReturn(REQUEST_URI);
        when(this.request.getRequestURL()).thenReturn(REQUEST_URL);

        this.response = mock();

        this.filter.doFilter(this.request, this.response, filterChain);
    }

    private void changeRequest()
    {
        when(this.request.getScheme()).thenReturn(OTHER_SCHEME);
        when(this.request.getServerName()).thenReturn(OTHER_SERVER_NAME);
        when(this.request.getServerPort()).thenReturn(OTHER_SERVER_PORT);
        when(this.request.getRequestURI()).thenReturn(OTHER_REQUEST_URI);
        when(this.request.getRequestURL()).thenReturn(OTHER_REQUEST_URL);
    }

    @Test
    void getScheme()
    {
        assertEquals(SCHEME, this.filteredRequest.getScheme());

        changeRequest();

        assertEquals(OTHER_SCHEME, this.filteredRequest.getScheme());
    }

    @Test
    void getServerName()
    {
        assertEquals(SERVER_NAME, this.filteredRequest.getServerName());

        changeRequest();

        assertEquals(OTHER_SERVER_NAME, this.filteredRequest.getServerName());
    }

    @Test
    void getServerPort()
    {
        assertEquals(SERVER_PORT, this.filteredRequest.getServerPort());

        changeRequest();

        assertEquals(OTHER_SERVER_PORT, this.filteredRequest.getServerPort());
    }

    @Test
    void getRequestURL()
    {
        assertEquals(REQUEST_URL.toString(), this.filteredRequest.getRequestURL().toString());

        changeRequest();

        assertEquals(OTHER_REQUEST_URL.toString(), this.filteredRequest.getRequestURL().toString());
    }
}
