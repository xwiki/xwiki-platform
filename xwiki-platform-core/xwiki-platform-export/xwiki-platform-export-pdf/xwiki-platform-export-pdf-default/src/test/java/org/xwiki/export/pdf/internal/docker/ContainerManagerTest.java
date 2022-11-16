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
package org.xwiki.export.pdf.internal.docker;

import org.junit.jupiter.api.Test;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.PullResponseItem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContainerManager}.
 * 
 * @version $Id$
 */
@ComponentTest
class ContainerManagerTest
{
    @InjectMockComponents
    private ContainerManager containerManager;

    @MockComponent
    private DockerClientFactory dockerClientFactory;

    private DockerClient dockerClient;

    @BeforeComponent
    public void configure()
    {
        this.dockerClient = mock(DockerClient.class);
        when(this.dockerClientFactory.createDockerClient()).thenReturn(this.dockerClient);
    }

    @Test
    void isLocalImagePresent()
    {
        InspectImageCmd inspectFooCmd = mock(InspectImageCmd.class);
        when(this.dockerClient.inspectImageCmd("foo")).thenReturn(inspectFooCmd);

        InspectImageCmd inspectBarCmd = mock(InspectImageCmd.class);
        when(this.dockerClient.inspectImageCmd("bar")).thenReturn(inspectBarCmd);
        when(inspectBarCmd.exec()).thenThrow(new NotFoundException("Image not found!"));

        assertTrue(this.containerManager.isLocalImagePresent("foo"));
        assertFalse(this.containerManager.isLocalImagePresent("bar"));
    }

    @Test
    void pullImage() throws Exception
    {
        PullImageCmd pullImagedCmd = mock(PullImageCmd.class);
        when(this.dockerClient.pullImageCmd("test/image")).thenReturn(pullImagedCmd);

        ResultCallback.Adapter<PullResponseItem> pullImageCallback = mock(ResultCallback.Adapter.class);
        when(pullImagedCmd.start()).thenReturn(pullImageCallback);

        this.containerManager.pullImage("test/image");

        verify(pullImageCallback).awaitCompletion();
    }
}
