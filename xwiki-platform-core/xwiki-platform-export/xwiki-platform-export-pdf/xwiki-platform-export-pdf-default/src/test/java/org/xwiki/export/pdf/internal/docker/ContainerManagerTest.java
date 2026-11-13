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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallback.Adapter;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.command.UnpauseContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.WaitResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

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

        Adapter<PullResponseItem> pullImageCallback = mock(Adapter.class);
        when(pullImagedCmd.exec(any(Adapter.class))).thenReturn(pullImageCallback);

        this.containerManager.pullImage("test/image");

        verify(pullImageCallback).awaitCompletion();
    }

    @Test
    void maybeReuseContainerByName()
    {
        Container container = mock(Container.class);
        when(container.getNames()).thenReturn(new String[] {"/alias", "/container-name"});
        when(container.getId()).thenReturn("containerId");

        Container other = mock(Container.class, "other");
        when(other.getNames()).thenReturn(new String[] {"container-name", "/container-name-other"});

        ListContainersCmd listContainersCmd = mock(ListContainersCmd.class);
        when(this.dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withNameFilter(List.of("container-name"))).thenReturn(listContainersCmd);
        when(listContainersCmd.withNameFilter(List.of("missing-container"))).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(true)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(List.of(other, container));

        InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        when(this.dockerClient.inspectContainerCmd("containerId")).thenReturn(inspectCmd);
        InspectContainerResponse inspectResponse = mock(InspectContainerResponse.class);
        when(inspectCmd.exec()).thenReturn(inspectResponse);

        ContainerState state = mock(ContainerState.class);
        when(inspectResponse.getState()).thenReturn(state);
        when(inspectResponse.getId()).thenReturn("containerId");

        StartContainerCmd startCmd = mock(StartContainerCmd.class);
        when(this.dockerClient.startContainerCmd("containerId")).thenReturn(startCmd);

        UnpauseContainerCmd unpauseCmd = mock(UnpauseContainerCmd.class);
        when(this.dockerClient.unpauseContainerCmd("containerId")).thenReturn(unpauseCmd);

        RemoveContainerCmd removeCmd = mock(RemoveContainerCmd.class);
        when(this.dockerClient.removeContainerCmd("containerId")).thenReturn(removeCmd);

        // Missing container.
        assertNull(this.containerManager.maybeReuseContainerByName("missing-container"));

        // Running container.
        when(state.getRunning()).thenReturn(true);
        assertEquals("containerId", this.containerManager.maybeReuseContainerByName("container-name"));
        verify(startCmd, never()).exec();
        verify(unpauseCmd, never()).exec();
        verify(removeCmd, never()).exec();

        // Stopped container.
        when(state.getRunning()).thenReturn(false);
        assertEquals("containerId", this.containerManager.maybeReuseContainerByName("container-name"));
        verify(startCmd).exec();
        verify(unpauseCmd, never()).exec();
        verify(removeCmd, never()).exec();

        // Paused container.
        when(state.getPaused()).thenReturn(true);
        assertEquals("containerId", this.containerManager.maybeReuseContainerByName("container-name"));
        verify(unpauseCmd).exec();
        verify(removeCmd, never()).exec();

        // Dead container.
        when(state.getDead()).thenReturn(true);
        assertNull(this.containerManager.maybeReuseContainerByName("container-name"));
        verify(removeCmd).exec();
    }

    @Test
    void stopContainerWhenRemovedBeforeWaiting()
    {
        StopContainerCmd stopCommand = mock(StopContainerCmd.class);
        when(this.dockerClient.stopContainerCmd("containerId")).thenReturn(stopCommand);
        WaitContainerCmd waitCommand = mock(WaitContainerCmd.class);
        when(this.dockerClient.waitContainerCmd("containerId")).thenReturn(waitCommand);
        // Docker removes the stopped container (automatic removal) before the wait request reaches it.
        when(waitCommand.exec(any())).thenAnswer(invocation -> {
            ResultCallback<WaitResponse> callback = invocation.getArgument(0);
            callback.onError(new NotFoundException("No such container: containerId"));
            return callback;
        });

        this.containerManager.stopContainer("containerId");

        verify(stopCommand).exec();
        verify(waitCommand).exec(any());
    }

    @Test
    void stopContainerWhenWaitingFails()
    {
        StopContainerCmd stopCommand = mock(StopContainerCmd.class);
        when(this.dockerClient.stopContainerCmd("containerId")).thenReturn(stopCommand);
        WaitContainerCmd waitCommand = mock(WaitContainerCmd.class);
        when(this.dockerClient.waitContainerCmd("containerId")).thenReturn(waitCommand);
        DockerException failure = new DockerException("Server error", 500);
        when(waitCommand.exec(any())).thenAnswer(invocation -> {
            ResultCallback<WaitResponse> callback = invocation.getArgument(0);
            callback.onError(failure);
            return callback;
        });

        // Other failures are still reported.
        assertSame(failure,
            assertThrows(DockerException.class, () -> this.containerManager.stopContainer("containerId")));
        assertEquals("Error during callback", this.logCapture.getMessage(0));
    }
}
