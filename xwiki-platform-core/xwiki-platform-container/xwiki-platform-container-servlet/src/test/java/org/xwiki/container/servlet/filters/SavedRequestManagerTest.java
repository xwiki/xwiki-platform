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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.xwiki.container.servlet.filters.SavedRequestManager.SavedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test for {@link SavedRequestManager}.
 *
 * @version $Id$
 * @since 2.5M1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SavedRequestManagerTest
{
    /** Fake test URL. */
    private static final String TEST_URL = "http://localhost/xwiki/bin/view/Test/Page";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @BeforeEach
    void setUp()
    {
        Map<String, String[]> params = new HashMap<>();
        params.put("aaa", new String[] {"bbb"});
        params.put("srid", new String[] {"r4Nd0m"});

        when(this.request.getSession()).thenReturn(this.session);
        when(this.request.getParameterMap()).thenReturn(params);
        when(this.request.getRequestURL()).thenReturn(new StringBuffer(TEST_URL));
        when(this.request.getParameter("srid")).thenReturn("r4Nd0m");

        Map<String, SavedRequest> saveMap = new HashMap<>();
        saveMap.put("r4Nd0m", new SavedRequest(this.request));
        when(this.session.getAttribute(any())).thenReturn(saveMap);
    }

    @Test
    void getters()
    {
        assertEquals("srid", SavedRequestManager.getSavedRequestIdentifier());
        assertEquals(SavedRequest.class.getCanonicalName() + "_SavedRequests", SavedRequestManager.getSavedRequestKey());
    }

    @Test
    void save()
    {
        String srid = SavedRequestManager.saveRequest(this.request);
        assertNotNull(srid);
        assertFalse("".equals(srid));
    }

    @Test
    void savedUrl()
    {
        assertEquals(TEST_URL + "?srid=r4Nd0m", SavedRequestManager.getOriginalUrl(this.request));
    }
}
