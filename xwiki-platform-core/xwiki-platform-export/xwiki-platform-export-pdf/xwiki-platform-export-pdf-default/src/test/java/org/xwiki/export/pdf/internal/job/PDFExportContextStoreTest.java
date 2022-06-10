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
package org.xwiki.export.pdf.internal.job;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;
import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PDFExportContextStore}.
 * 
 * @version $Id$
 */
@ComponentTest
class PDFExportContextStoreTest
{
    @InjectMockComponents
    private PDFExportContextStore contextStore;

    @MockComponent
    private Provider<XWikiContext> writeProvider;

    @MockComponent
    @Named("readonly")
    private Provider<XWikiContext> readProvider;

    @Captor
    private ArgumentCaptor<XWikiRequest> requestCaptor;

    @Test
    void saveRestore()
    {
        XWikiContext firstXContext = mock(XWikiContext.class, "first");
        when(this.readProvider.get()).thenReturn(firstXContext);

        XWikiRequest firstRequest = mock(XWikiRequest.class, "first");
        when(firstXContext.getRequest()).thenReturn(firstRequest);

        Cookie[] cookies = new Cookie[] {new Cookie("color", "blue")};
        when(firstRequest.getCookies()).thenReturn(cookies);

        Map<String, Serializable> context = new HashMap<>();
        this.contextStore.save(context, this.contextStore.getSupportedEntries());

        XWikiContext secondXContext = mock(XWikiContext.class, "second");
        when(this.writeProvider.get()).thenReturn(secondXContext);

        XWikiRequest secondRequest = mock(XWikiRequest.class, "second");
        when(secondXContext.getRequest()).thenReturn(secondRequest);
        when(secondRequest.getRequestURL()).thenReturn(new StringBuffer("http://xwiki.org"));
        when(secondRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        this.contextStore.restore(context);

        verify(this.writeProvider.get()).setRequest(this.requestCaptor.capture());
        assertSame(cookies, this.requestCaptor.getValue().getCookies());
        assertEquals("blue", this.requestCaptor.getValue().getCookie("color").getValue());
    }
}
