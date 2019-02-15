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
package com.xpn.xwiki.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponseStub;
import com.xpn.xwiki.web.XWikiServletURLFactory;
import com.xpn.xwiki.web.XWikiURLFactoryService;

import static com.xpn.xwiki.test.mockito.OldcoreMatchers.anyXWikiContext;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultXWikiStubContextProvider}.
 * 
 * @version $Id$
 */
@OldcoreTest
public class DefaultXWikiStubContextProviderTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private DefaultXWikiStubContextProvider provider;

    private XWikiURLFactoryService urlFactoryService = mock(XWikiURLFactoryService.class);

    @BeforeEach
    public void beforeEach()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequestStub());
        this.oldcore.getXWikiContext().setResponse(new XWikiServletResponseStub());

        when(this.oldcore.getSpyXWiki().getURLFactoryService()).thenReturn(this.urlFactoryService);
        when(this.urlFactoryService.createURLFactory(same(XWikiContext.MODE_SERVLET), anyXWikiContext()))
            .thenAnswer(invocation -> new XWikiServletURLFactory());

        this.provider.initialize(this.oldcore.getXWikiContext());
    }

    @Test
    public void createStubContext()
    {
        XWikiContext xcontext1 = this.provider.createStubContext();
        XWikiContext xcontext2 = this.provider.createStubContext();

        assertNotSame(xcontext1, xcontext2);
        assertNotSame(xcontext1.getRequest(), xcontext2.getRequest());
        assertNotSame(xcontext1.getResponse(), xcontext2.getResponse());
        assertNotSame(xcontext1.getURLFactory(), xcontext2.getURLFactory());
    }
}
