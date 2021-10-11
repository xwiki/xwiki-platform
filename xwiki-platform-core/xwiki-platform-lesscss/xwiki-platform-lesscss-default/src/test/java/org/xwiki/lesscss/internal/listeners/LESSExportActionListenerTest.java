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
package org.xwiki.lesscss.internal.listeners;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.lesscss.internal.LESSContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LESSExportActionListener}.
 *
 * @version $Id$
 * @since 6.2RC1
 */
public class LESSExportActionListenerTest
{
    @Rule
    public MockitoComponentMockingRule<LESSExportActionListener> mocker =
        new MockitoComponentMockingRule<>(LESSExportActionListener.class);

    @Test
    public void getName() throws Exception
    {
        assertEquals("lessexport", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void getEvents() throws Exception
    {
        assertEquals(1, this.mocker.getComponentUnderTest().getEvents().size());
        assertEquals(new ActionExecutingEvent("export"), this.mocker.getComponentUnderTest().getEvents().get(0));
    }

    @Test
    public void onEventWhenHTMLExport() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        XWikiRequest request = mock(XWikiRequest.class);
        when(xcontext.getRequest()).thenReturn(request);
        when(request.get("format")).thenReturn("html");

        this.mocker.getComponentUnderTest().onEvent(new ActionExecutingEvent("export"), null, xcontext);

        // The test is here: we verify that the cache is disabled!
        LESSContext lessContext = mocker.getInstance(LESSContext.class);
        verify(lessContext).setHtmlExport(true);
    }

    @Test
    public void onEventWhenNonHTMLExport() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        XWikiRequest request = mock(XWikiRequest.class);
        when(xcontext.getRequest()).thenReturn(request);
        when(request.get("format")).thenReturn("xar");

        this.mocker.getComponentUnderTest().onEvent(new ActionExecutingEvent("export"), null, xcontext);

        // The test is here: we verify that the we do not disable the LESS cache (since the export is not an HTML
        // export). Actually that the context object was not called at all...
        LESSContext lessContext = mocker.getInstance(LESSContext.class);
        verifyNoInteractions(lessContext);
    }
}
