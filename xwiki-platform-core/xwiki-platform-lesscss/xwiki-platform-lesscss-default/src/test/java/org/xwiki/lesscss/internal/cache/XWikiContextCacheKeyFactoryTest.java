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
package org.xwiki.lesscss.internal.cache;

import java.net.URL;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class XWikiContextCacheKeyFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<XWikiContextCacheKeyFactory> mocker =
            new MockitoComponentMockingRule<>(XWikiContextCacheKeyFactory.class);

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    @Before
    public void setUp() throws Exception
    {
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
    }

    @Test
    public void getCacheKey() throws Exception
    {
        // Mocks
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(xcontext.getURLFactory()).thenReturn(urlFactory);
        
        when(urlFactory.createSkinURL(anyString(), anyString(), any(XWikiContext.class))).thenReturn(
                new URL("http://url"));
        
        // Test
        assertEquals("XWikiContext[URLFactory[" + urlFactory.getClass().getName() + ", http://url]]",
                mocker.getComponentUnderTest().getCacheKey());
    }
}
