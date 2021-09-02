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
package org.xwiki.rest.internal.exceptions;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static ch.qos.logback.classic.Level.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test of {@link ExceptionExceptionMapper}.
 *
 * @version $Id$
 * @since 13.8RC1
 * @since 13.7.1
 * @since 13.4.4
 */
@ComponentTest
class ExceptionExceptionMapperTest
{
    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @InjectMockComponents
    private ExceptionExceptionMapper exceptionExceptionMapper;

    @Test
    void toResponseUnchecked()
    {
        IOException cause = new IOException("file not found");
        Response response = this.exceptionExceptionMapper.toResponse(cause);

        assertEquals("No ExceptionMapper was found for [java.io.IOException: file not found]. "
            + "Cause: [IOException: file not found].", response.getEntity());
        assertEquals(500, response.getStatus());
        assertEquals(1, this.logCapture.size());
        assertEquals("A REST endpoint failed with an unmapped exception.", this.logCapture.getMessage(0));
        assertEquals("java.io.IOException", this.logCapture.getLogEvent(0).getThrowableProxy().getClassName());
        assertEquals("file not found", this.logCapture.getLogEvent(0).getThrowableProxy().getMessage());
        assertEquals(ERROR, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void toResponseRuntimeException()
    {
        RuntimeException runtimeException = new RuntimeException("file not found");
        RuntimeException runtimeExceptionThrown = assertThrows(RuntimeException.class,
            () -> this.exceptionExceptionMapper.toResponse(runtimeException));
        assertSame(runtimeException, runtimeExceptionThrown);
    }
}
