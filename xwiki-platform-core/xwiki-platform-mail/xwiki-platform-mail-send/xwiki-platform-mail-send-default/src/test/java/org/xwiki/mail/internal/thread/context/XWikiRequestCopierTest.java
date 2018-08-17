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
package org.xwiki.mail.internal.thread.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link XWikiRequestCopier}.
 *
 * @version $Id$
 */
@ComponentTest
public class XWikiRequestCopierTest
{
    @InjectMockComponents
    private XWikiRequestCopier copier;

    XWikiServletRequestStub originalRequest;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        this.originalRequest = new XWikiServletRequestStub();
        this.originalRequest.setHost("host");
        this.originalRequest.setContextPath("contextPath");
        this.originalRequest.setScheme("scheme");
        this.originalRequest.setAttribute("attribute", "value");
        this.originalRequest.setServerName("server");
        this.originalRequest.setrequestURL(new StringBuffer("url"));
    }

    @Test
    public void copyRequest() throws Exception
    {
        XWikiRequest copy = this.copier.copy(this.originalRequest);
        assertNotSame(this.originalRequest, copy);

        // Check that each value on the cloned request are equal.
        assertEquals(this.originalRequest.getHeader("x-forwarded-host"), copy.getHeader("x-forwarded-host"));
        assertEquals(this.originalRequest.getContextPath(), copy.getContextPath());
        assertEquals(this.originalRequest.getScheme(), copy.getScheme());
        assertEquals(this.originalRequest.getAttributeNames(), copy.getAttributeNames());
        assertEquals(this.originalRequest.getAttribute("attribute"), copy.getAttribute("attribute"));
        assertEquals(this.originalRequest.getServerName(), copy.getServerName());
        assertNotSame(this.originalRequest.getRequestURL(), copy.getRequestURL());
        assertEquals(this.originalRequest.getRequestURL().toString(), copy.getRequestURL().toString());
    }

    @Test
    public void copyContextWhenNull() throws Exception
    {
        assertNull(this.copier.copy(null));
    }
}
