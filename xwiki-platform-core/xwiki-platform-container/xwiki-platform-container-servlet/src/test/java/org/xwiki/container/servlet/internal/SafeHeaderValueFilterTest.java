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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.container.servlet.filters.internal.SafeHeaderValueFilter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Validate {@link SafeHeaderValueFilter}.
 * 
 * @version $Id$
 */
class SafeHeaderValueFilterTest
{
    private final HttpServletResponse httpServletResponse = mock();

    private HttpServletResponse safeServletResponse;

    @BeforeEach
    void setup() throws IOException, ServletException
    {
        new SafeHeaderValueFilter().doFilter(null, this.httpServletResponse,
            (request, response) -> this.safeServletResponse = (HttpServletResponse) response);
    }

    @Test
    void setHeader()
    {
        this.safeServletResponse.setHeader("X-Test", "first\nsecond\r\nthird");
        this.safeServletResponse.setHeader("X-Null", null);

        verify(this.httpServletResponse).setHeader("X-Test", "first second  third");
        verify(this.httpServletResponse).setHeader("X-Null", null);
    }

    @Test
    void addHeader()
    {
        this.safeServletResponse.addHeader("X-Test", "first\nsecond\r\nthird");
        this.safeServletResponse.addHeader("X-Null", null);

        verify(this.httpServletResponse).addHeader("X-Test", "first second  third");
        verify(this.httpServletResponse).addHeader("X-Null", null);
    }
}
