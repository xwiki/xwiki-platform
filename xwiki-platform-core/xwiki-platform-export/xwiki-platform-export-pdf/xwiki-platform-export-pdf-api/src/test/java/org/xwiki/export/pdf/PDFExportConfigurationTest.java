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
package org.xwiki.export.pdf;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the default methods of {@link PDFExportConfiguration}.
 * 
 * @version $Id$
 */
class PDFExportConfigurationTest
{
    private PDFExportConfiguration config = mock(PDFExportConfiguration.class);

    @Test
    void isXWikiURISpecified() throws Exception
    {
        when(this.config.isXWikiURISpecified()).thenCallRealMethod();

        when(this.config.getXWikiURI()).thenReturn(new URI("http://www.xwiki.org"));
        assertTrue(this.config.isXWikiURISpecified());

        when(this.config.getXWikiURI()).thenReturn(new URI(PDFExportConfiguration.DEFAULT_XWIKI_URI));
        assertFalse(this.config.isXWikiURISpecified());

        when(this.config.getXWikiURI()).thenThrow(new URISyntaxException("some URI", "some reason"));
        assertTrue(this.config.isXWikiURISpecified());
    }

    @Test
    void getChromeRemoteDebuggingTimeout()
    {
        when(this.config.getChromeRemoteDebuggingTimeout()).thenCallRealMethod();
        when(this.config.getPageReadyTimeout()).thenReturn(300);
        assertEquals(300 / 6, this.config.getChromeRemoteDebuggingTimeout());
    }
}
