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

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.lesscss.internal.LESSContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
@ComponentTest
class LESSExportActionListenerTest
{
    @InjectMockComponents
    private LESSExportActionListener lessExportActionListener;

    @MockComponent
    private LESSContext lessContext;

    @Test
    void getName()
    {
        assertEquals("lessexport", this.lessExportActionListener.getName());
    }

    @Test
    void getEvents()
    {
        assertEquals(1, this.lessExportActionListener.getEvents().size());
        assertEquals(new ActionExecutingEvent("export"), this.lessExportActionListener.getEvents().get(0));
    }

    @Test
    void onEventWhenHTMLExport()
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        XWikiRequest request = mock(XWikiRequest.class);
        when(xcontext.getRequest()).thenReturn(request);
        when(request.get("format")).thenReturn("html");

        this.lessExportActionListener.onEvent(new ActionExecutingEvent("export"), null, xcontext);

        // The test is here: we verify that the cache is disabled!
        verify(this.lessContext).setHtmlExport(true);
    }

    @Test
    void onEventWhenNonHTMLExport()
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        XWikiRequest request = mock(XWikiRequest.class);
        when(xcontext.getRequest()).thenReturn(request);
        when(request.get("format")).thenReturn("xar");

        this.lessExportActionListener.onEvent(new ActionExecutingEvent("export"), null, xcontext);

        // The test is here: we verify that the we do not disable the LESS cache (since the export is not an HTML
        // export). Actually that the context object was not called at all...
        verifyNoInteractions(this.lessContext);
    }
}
