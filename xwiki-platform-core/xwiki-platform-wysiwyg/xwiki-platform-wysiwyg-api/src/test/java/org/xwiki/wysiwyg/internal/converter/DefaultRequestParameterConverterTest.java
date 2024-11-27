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
package org.xwiki.wysiwyg.internal.converter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.URLSecurityManager;
import org.xwiki.wysiwyg.converter.HTMLConverter;
import org.xwiki.wysiwyg.internal.filter.http.MutableHttpServletRequestFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultRequestParameterConverter}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ MutableHttpServletRequestFactory.class })
class DefaultRequestParameterConverterTest
{
    @InjectMockComponents
    private DefaultRequestParameterConverter converter;

    @MockComponent
    private HTMLConverter htmlConverter;

    @MockComponent
    private URLSecurityManager urlSecurityManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void convertWithError() throws URISyntaxException, IOException
    {
        HttpServletRequest servletRequest = mock();
        String domain = "domain";
        when(servletRequest.getServerName()).thenReturn(domain);
        when(servletRequest.getSession()).thenReturn(mock());
        HttpServletResponse servletResponse = mock();

        String parameterName = "test";
        when(servletRequest.getParameterValues("RequiresHTMLConversion")).thenReturn(new String[] { parameterName });
        String testContent = "testContent";
        String testSyntax = "testSyntax";
        String errorURL = "errorURL";
        when(servletRequest.getParameterMap()).thenReturn(
            Map.of(parameterName, new String[] { testContent },
                parameterName + "_syntax", new String[] { testSyntax },
                "xerror", new String[] { errorURL }));

        String testMessage = "TestException";
        IllegalArgumentException testException = new IllegalArgumentException(testMessage);
        when(this.htmlConverter.fromHTML(testContent, testSyntax)).thenThrow(testException);

        String safeURL = "https://www.xwiki.org";
        when(this.urlSecurityManager.parseToSafeURI(startsWith(errorURL), eq(domain)))
            .thenReturn(new URI(safeURL));

        Optional<ServletRequest> result = this.converter.convert(servletRequest, servletResponse);

        assertTrue(result.isEmpty());

        verify(servletResponse).sendRedirect(safeURL);

        assertEquals(1, this.logCapture.size());
        ILoggingEvent logEvent = this.logCapture.getLogEvent(0);
        assertEquals(testMessage, logEvent.getMessage());
    }
}
