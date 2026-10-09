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
package com.xpn.xwiki.web;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link XWikiServletResponse}.
 *
 * @version $Id$
 */
class XWikiServletResponseTest
{
    private final HttpServletResponse httpResponse = mock();

    private final XWikiServletResponse response = new XWikiServletResponse(this.httpResponse);

    @Test
    void setHeaderReplacesLineBreaks()
    {
        this.response.setHeader("X-Test", "first\nsecond\r\nthird");

        verify(this.httpResponse).setHeader("X-Test", "first second  third");
    }

    @Test
    void addHeaderReplacesLineBreaks()
    {
        this.response.addHeader("X-Test", "first\nsecond\r\nthird");
        this.response.addHeader("X-Null", null);

        verify(this.httpResponse).addHeader("X-Test", "first second  third");
        verify(this.httpResponse).addHeader("X-Null", null);
    }
}
