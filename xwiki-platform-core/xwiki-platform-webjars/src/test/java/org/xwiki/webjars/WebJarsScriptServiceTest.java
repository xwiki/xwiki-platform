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
package org.xwiki.webjars;

import org.junit.*;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.webjars.script.WebJarsScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.webjars.script.WebJarsScriptService}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class WebJarsScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<WebJarsScriptService> componentManager =
        new MockitoComponentMockingRule<WebJarsScriptService>(WebJarsScriptService.class);

    @Test
    public void computeURL() throws Exception
    {
        Execution execution = this.componentManager.getInstance(Execution.class);
        ExecutionContext context = new ExecutionContext();
        XWikiContext xwikiContext = mock(XWikiContext.class);
        context.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xwikiContext);
        when(execution.getContext()).thenReturn(context);
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(xwikiContext.getURLFactory()).thenReturn(urlFactory);

        this.componentManager.getComponentUnderTest().url("angular/2.1.11/angular.js");

        verify(urlFactory).createURL("resources", "path", "webjars", "value=angular/2.1.11/angular.js", null,
            xwikiContext);
    }
}
