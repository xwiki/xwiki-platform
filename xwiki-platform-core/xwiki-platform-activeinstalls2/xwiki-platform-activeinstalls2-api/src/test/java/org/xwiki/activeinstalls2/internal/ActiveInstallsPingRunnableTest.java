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
package org.xwiki.activeinstalls2.internal;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.activeinstalls2.ActiveInstallsConfiguration;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ActiveInstallsPingRunnable}.
 *
 * @version $Id$
 */
@ComponentTest
class ActiveInstallsPingRunnableTest
{
    @MockComponent
    private ActiveInstallsConfiguration configuration;

    @MockComponent
    private PingSender pingSender;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void sendPingWhenFailing() throws Exception
    {
        when(this.configuration.getPingInstanceURL()).thenReturn("http://someurl");
        doThrow(new Exception("error")).when(this.pingSender).sendPing();

        ActiveInstallsPingRunnable thread = new ActiveInstallsPingRunnable(this.configuration, this.pingSender, 1,
            TimeUnit.DAYS);
        thread.setRetryTimeout(0L);
        thread.sendPing();

        assertEquals(3, logCapture.size());
        assertEquals("Failed to send Active Installation ping to [http://someurl] (try [1]). "
            + "Error = [Exception: error].", logCapture.getMessage(0));
        assertEquals("Failed to send Active Installation ping to [http://someurl] (try [2]). "
            + "Error = [Exception: error].", logCapture.getMessage(1));
        assertEquals("Failed to send Active Installation ping to [http://someurl] (try [3]). "
            + "Error = [Exception: error]. Will retry in [1 days]...", logCapture.getMessage(2));
    }

    @Test
    void sendPingWhenOk() throws Exception
    {
        ActiveInstallsPingRunnable thread = new ActiveInstallsPingRunnable(this.configuration, this.pingSender, 1,
            TimeUnit.DAYS);
        thread.setRetryTimeout(0L);
        thread.sendPing();

        verify(this.pingSender).sendPing();
        assertEquals(0, logCapture.size());
    }
}
