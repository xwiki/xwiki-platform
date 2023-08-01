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
package org.xwiki.extension.security.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.extension.security.ExtensionSecurityConfiguration;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ExtensionSecurityScheduler}.
 *
 * @version $Id$
 */
@ComponentTest
class ExtensionSecuritySchedulerTest
{
    @InjectMockComponents
    private ExtensionSecurityScheduler scheduler;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private ExtensionSecurityConfiguration extensionSecurityConfiguration;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @Mock
    private Job mock;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.extensionSecurityConfiguration.getScanDelay()).thenReturn(1);
        when(this.jobExecutor.execute(anyString(), any())).thenReturn(this.mock);
    }

    @Test
    void start()
    {
        this.scheduler.start();
        verify(this.extensionSecurityConfiguration, timeout(1000)).isSecurityScanEnabled();
        this.scheduler.start();
        verify(this.extensionSecurityConfiguration).isSecurityScanEnabled();
        assertEquals("Extension security scan disabled.", this.logCapture.getMessage(0));
    }

    @Test
    void startEnabled() throws Exception
    {
        when(this.extensionSecurityConfiguration.isSecurityScanEnabled()).thenReturn(true);
        this.scheduler.start();
        verify(this.extensionSecurityConfiguration, timeout(1000)).isSecurityScanEnabled();
        verify(this.jobExecutor, timeout(1000)).execute(ExtensionSecurityJob.JOBTYPE, new ExtensionSecurityRequest());
        this.scheduler.start();
        verify(this.extensionSecurityConfiguration).isSecurityScanEnabled();
        verify(this.jobExecutor).execute(ExtensionSecurityJob.JOBTYPE, new ExtensionSecurityRequest());
    }

    @Test
    void restart()
    {
        this.scheduler.start();
        verify(this.extensionSecurityConfiguration, timeout(1000)).isSecurityScanEnabled();
        this.scheduler.restart();
        verify(this.extensionSecurityConfiguration, timeout(1000).times(2)).isSecurityScanEnabled();
        assertEquals("Extension security scan disabled.", this.logCapture.getMessage(0));
        assertEquals("Extension security scan disabled.", this.logCapture.getMessage(1));
    }
}
