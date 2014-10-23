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
package org.xwiki.lesscss.internal;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.lesscss.ColorThemeCache;
import org.xwiki.lesscss.LESSSkinFileCache;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
        when(xcontext.getWikiId()).thenReturn("wiki");
        CurrentColorThemeGetter currentColorThemeGetter = this.mocker.getInstance(CurrentColorThemeGetter.class);
        when(currentColorThemeGetter.getCurrentColorTheme("default")).thenReturn("colorTheme");

        this.mocker.getComponentUnderTest().onEvent(new ActionExecutingEvent("export"), null, xcontext);

        // The test is here: we verify that the clear API was called!
        LESSSkinFileCache cache = this.mocker.getInstance(LESSSkinFileCache.class);
        ColorThemeCache cache2 = this.mocker.getInstance(ColorThemeCache.class);
        verify(cache).clearFromColorTheme("colorTheme");
        verify(cache2).clearFromColorTheme("colorTheme");
    }

    @Test
    public void onEventWhenNonHTMLExport() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        XWikiRequest request = mock(XWikiRequest.class);
        when(xcontext.getRequest()).thenReturn(request);
        when(request.get("format")).thenReturn("xar");

        this.mocker.getComponentUnderTest().onEvent(new ActionExecutingEvent("export"), null, xcontext);

        // The test is here: we verify that the clear API was NOT called (since the export is not an HTML export).
        // Actually that the cache object was not called at all...
        LESSSkinFileCache cache = this.mocker.getInstance(LESSSkinFileCache.class);
        verifyZeroInteractions(cache);
        CurrentColorThemeGetter currentColorThemeGetter = this.mocker.getInstance(CurrentColorThemeGetter.class);
        verifyZeroInteractions(currentColorThemeGetter);
    }
}
