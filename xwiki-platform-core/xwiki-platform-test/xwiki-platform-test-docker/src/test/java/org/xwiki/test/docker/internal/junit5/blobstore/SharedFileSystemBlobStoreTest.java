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
package org.xwiki.test.docker.internal.junit5.blobstore;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.xwiki.test.docker.junit5.TestConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SharedFileSystemBlobStore}.
 *
 * @version $Id$
 */
class SharedFileSystemBlobStoreTest
{
    private TestConfiguration testConfiguration;

    @BeforeEach
    void beforeEach()
    {
        this.testConfiguration = mock(TestConfiguration.class);
    }

    @Test
    void newVolumeName()
    {
        String volumeName = SharedFileSystemBlobStore.newVolumeName();

        assertTrue(volumeName.startsWith("xwiki-blobstore-" + DockerClientFactory.SESSION_ID + '-'), volumeName);
        // Each cluster gets its own volume.
        assertNotEquals(volumeName, SharedFileSystemBlobStore.newVolumeName());
    }

    @Test
    void getVolumeLabels()
    {
        // The volume is removed with the resources of the Testcontainers session.
        assertEquals(DockerClientFactory.SESSION_ID,
            SharedFileSystemBlobStore.getVolumeLabels().get(DockerClientFactory.TESTCONTAINERS_SESSION_ID_LABEL));
    }

    @Test
    void getDirectory()
    {
        when(this.testConfiguration.getOutputDirectory()).thenReturn("target/mysql-latest");

        assertEquals(new File("target/mysql-latest", "blobstore"),
            SharedFileSystemBlobStore.getDirectory(this.testConfiguration));
    }
}
