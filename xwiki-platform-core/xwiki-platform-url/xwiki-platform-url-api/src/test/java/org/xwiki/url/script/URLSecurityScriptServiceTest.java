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
package org.xwiki.url.script;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.URLSecurityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link URLSecurityScriptService}.
 *
 * @version $Id$
 * @since 14.10.4
 * @since 15.0RC1
 */
@ComponentTest
class URLSecurityScriptServiceTest
{
    @InjectMockComponents
    private URLSecurityScriptService scriptService;

    @MockComponent
    private URLSecurityManager urlSecurityManager;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void isURITrusted() throws URISyntaxException
    {
        assertFalse(this.scriptService.isURITrusted(""));
        when(this.urlSecurityManager.isURITrusted(new URI(""))).thenReturn(true);
        assertTrue(this.scriptService.isURITrusted(""));

        assertFalse(this.scriptService.isURITrusted("/xwiki/\n/something/"));
        assertEquals(1, this.logCapture.size());
        assertEquals("Trying to check if [/xwiki/\n"
            + "/something/] is a trusted URI returned false because URI parsing failed: [URISyntaxException: "
            + "Illegal character in path at index 7: /xwiki/\n"
            + "/something/]", this.logCapture.getMessage(0));
        verify(this.urlSecurityManager, times(2)).isURITrusted(any());

        assertFalse(this.scriptService.isURITrusted("//xwiki.org/xwiki/something/"));

        URI expectedURI = new URI("//xwiki.org/xwiki/something/");
        when(this.urlSecurityManager.isURITrusted(expectedURI)).thenReturn(true);
        assertTrue(this.scriptService.isURITrusted("//xwiki.org/xwiki/something/"));
    }
}