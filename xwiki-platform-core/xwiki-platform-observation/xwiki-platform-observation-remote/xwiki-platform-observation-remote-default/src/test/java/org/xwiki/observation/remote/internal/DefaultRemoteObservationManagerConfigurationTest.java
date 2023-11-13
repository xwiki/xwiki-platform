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
package org.xwiki.observation.remote.internal;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.xwiki.environment.Environment;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultRemoteObservationManagerConfiguration}.
 *
 * @version $Id$
 * @since 14.1RC1
 */
@ComponentTest
class DefaultRemoteObservationManagerConfigurationTest
{
    @InjectMockComponents
    private DefaultRemoteObservationManagerConfiguration remoteObservationManagerConfiguration;

    @MockComponent
    private Environment environment;

    @XWikiTempDir
    private File tmpDir;

    @BeforeComponent
    public void configure() throws Exception
    {
        // getPermanentDirectory needs to be mocked before on remoteObservationManagerConfiguration during the component
        // initialization.
        when(this.environment.getPermanentDirectory()).thenReturn(this.tmpDir);
    }
    
    @Test
    void initialize() throws Exception
    {
        String firstInitId = this.remoteObservationManagerConfiguration.getId();
        assertNotNull(firstInitId);
        // Simulate a restart by re-initializing.
        this.remoteObservationManagerConfiguration.initialize();
        assertEquals(firstInitId, this.remoteObservationManagerConfiguration.getId());
    }
}
