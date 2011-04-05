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
package org.xwiki.container.servlet.filters;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.container.servlet.filters.SavedRequestManager.SavedRequest;


/**
 * Test for {@link SavedRequestManager}.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class SavedRequestManagerTest
{
    /** Fake test URL. */
    private static final String TEST_URL = "http://localhost/xwiki/bin/view/Test/Page";

    /** Mocked request. */
    private HttpServletRequest request;

    @Before
    public void setUp()
    {
        Mockery mockery = new Mockery();
        final HttpSession mockSession = mockery.mock(HttpSession.class);
        final HttpServletRequest mockRequest = mockery.mock(HttpServletRequest.class);
        final Map<String, String[]> params = new HashMap<String, String[]>();
        params.put("aaa", new String[]{"bbb"});
        params.put("srid", new String[]{"r4Nd0m"});
        // request
        mockery.checking(new Expectations() {{
            allowing(mockRequest).getSession();
            will(returnValue(mockSession));
            allowing(mockRequest).getParameterMap();
            will(returnValue(params));
            allowing(mockRequest).getRequestURL();
            will(returnValue(new StringBuffer(TEST_URL)));
            allowing(mockRequest).getParameter("srid");
            will(returnValue("r4Nd0m"));
        }});
        final Map<String, SavedRequest> saveMap = new HashMap<String, SavedRequest>();
        saveMap.put("r4Nd0m", new SavedRequest(mockRequest));
        // session
        mockery.checking(new Expectations() {{
            allowing(mockSession).getAttribute(with(any(String.class)));
            will(returnValue(saveMap));
        }});
        this.request = mockRequest;
    }

    @Test
    public void testGetters()
    {
        Assert.assertEquals("srid", SavedRequestManager.getSavedRequestIdentifier());
        Assert.assertEquals(SavedRequest.class.getCanonicalName() + "_SavedRequests", SavedRequestManager.getSavedRequestKey());
    }

    @Test
    public void testSave()
    {
        String srid = SavedRequestManager.saveRequest(this.request);
        Assert.assertNotNull(srid);
        Assert.assertFalse("".equals(srid));
    }

    @Test
    public void testSavedUrl()
    {
        Assert.assertEquals(TEST_URL + "?srid=r4Nd0m", SavedRequestManager.getOriginalUrl(this.request));
    }
}

