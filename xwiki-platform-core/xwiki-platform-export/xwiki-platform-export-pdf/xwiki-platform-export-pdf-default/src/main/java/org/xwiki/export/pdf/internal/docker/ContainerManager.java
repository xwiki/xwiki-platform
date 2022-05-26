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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

/**
 * Help perform various operations on Docker containers.
 * 
 * @version $Id$
 * @since 14.4.1
 * @since 14.5RC1
 */
@Component(roles = ContainerManager.class)
@Singleton
public class ContainerManager implements Initializable
{
    private static final String DOCKER_SOCK = "/var/run/docker.sock";

    @Inject
    private Logger logger;

    private DockerClient client;

    @Override
    public void initialize() throws InitializationException
    {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig()).build();
        this.client = DockerClientImpl.getInstance(config, httpClient);
    }

    public String maybeReuseContainerByName(String containerName)
    {
        List<Container> containers =
            this.client.listContainersCmd().withNameFilter(Arrays.asList(containerName)).exec();
        if (containers.isEmpty()) {
            // There's no container with the specified name.
            return null;
        }

        InspectContainerResponse container = this.client.inspectContainerCmd(containers.get(0).getId()).exec();
        if (container.getState().getDead() == Boolean.TRUE) {
            // The container is not reusable. Try to remove it so it can be recreated.
            this.client.removeContainerCmd(container.getId()).exec();
            return null;
        } else if (container.getState().getPaused() == Boolean.TRUE) {
            this.client.unpauseContainerCmd(container.getId()).exec();
        } else if (container.getState().getRunning() != Boolean.TRUE
            && container.getState().getRestarting() != Boolean.TRUE) {
            this.startContainer(container.getId());
        }

        return container.getId();
    }

    /**
     * Checks if a given image is already pulled locally.
     * 
     * @param imageName the image to check
     * @return {@code true} if the specified image was already pulled, {@code false} otherwise
     */
    public boolean isLocalImagePresent(String imageName)
    {
        try {
            this.client.inspectImageCmd(imageName).exec();
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    /**
     * Docker-pull the passed image.
     *
     * @param imageName the image to pull
     */
    public void pullImage(String imageName)
    {
        PullImageResultCallback pullImageResultCallback =
            this.client.pullImageCmd(imageName).exec(new PullImageResultCallback());
        wait(pullImageResultCallback);
    }

    /**
     * Creates a new container based on the specified image.
     * 
     * @param imageName the image to use for the new container
     * @param containerName the name to associate with the created container
     * @param remoteDebuggingPort the port used for remote debugging
     * @param parameters the parameters to specify when creating the container
     * @return the id of the created container
     */
    public String createContainer(String imageName, String containerName, int remoteDebuggingPort,
        List<String> parameters)
    {
        ExposedPort exposedPort = ExposedPort.tcp(remoteDebuggingPort);
        Ports portBindings = new Ports();
        portBindings.bind(exposedPort, Ports.Binding.bindPort(remoteDebuggingPort));

        CreateContainerCmd command = this.client.createContainerCmd(imageName);
        CreateContainerResponse container = command.withCmd(parameters).withExposedPorts(exposedPort)
            .withHostConfig(HostConfig.newHostConfig().withBinds(
                // Make sure it also works when XWiki is running in Docker.
                new Bind(DOCKER_SOCK, new Volume(DOCKER_SOCK))).withPortBindings(portBindings))
            .withName(containerName).exec();
        return container.getId();
    }

    /**
     * Start the specified container and wait until it is ready.
     *
     * @param containerId the id of the container to start
     */
    public void startContainer(String containerId)
    {
        // Start (and stop and remove automatically when the conversion is finished, thanks to the autoremove above).
        this.client.startContainerCmd(containerId).exec();

        // Wait for the container to be ready before continuing.
        WaitContainerResultCallback resultCallback = new WaitContainerResultCallback();
        this.client.waitContainerCmd(containerId).exec(resultCallback);
        wait(resultCallback);
    }

    /**
     * Stop the specified container.
     * 
     * @param containerId the if of the container to stop
     */
    public void stopContainer(String containerId)
    {
        if (containerId != null) {
            this.client.stopContainerCmd(containerId).exec();

            // Wait for the container to be fully stopped before continuing.
            WaitContainerResultCallback resultCallback = new WaitContainerResultCallback();
            this.client.waitContainerCmd(containerId).exec(resultCallback);
            wait(resultCallback);
        }
    }

    private void wait(ResultCallbackTemplate<?, ?> template)
    {
        try {
            template.awaitCompletion();
        } catch (InterruptedException e) {
            this.logger.warn("Interrupted thread [{}]. Root cause: [{}]", Thread.currentThread().getName(),
                ExceptionUtils.getRootCauseMessage(e));
            // Restore interrupted state to be a good citizen...
            Thread.currentThread().interrupt();
        }
    }
}
