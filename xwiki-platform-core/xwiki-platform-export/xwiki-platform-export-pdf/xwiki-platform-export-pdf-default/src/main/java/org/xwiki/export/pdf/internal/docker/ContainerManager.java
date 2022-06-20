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
import org.xwiki.export.pdf.PDFExportConfiguration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.AsyncDockerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.SyncDockerCmd;
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
 * @since 14.4.2
 * @since 14.5RC1
 */
@Component(roles = ContainerManager.class)
@Singleton
public class ContainerManager implements Initializable
{
    private static final String DOCKER_SOCK = "/var/run/docker.sock";

    @Inject
    private Logger logger;

    @Inject
    private PDFExportConfiguration configuration;

    private DockerClient client;

    @Override
    public void initialize() throws InitializationException
    {
        this.logger.debug("Initializing the Docker client.");
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig()).build();
        this.client = DockerClientImpl.getInstance(config, httpClient);
    }

    /**
     * Attempts to reuse the container with the specified name.
     * 
     * @param containerName the name of the container to reuse
     * @return the container id, if a container with the specified name exists and can be reused, otherwise {@code null}
     */
    public String maybeReuseContainerByName(String containerName)
    {
        this.logger.debug("Looking for an existing Docker container with name [{}].", containerName);
        List<Container> containers =
            exec(this.client.listContainersCmd().withNameFilter(Arrays.asList(containerName)).withShowAll(true));
        if (containers.isEmpty()) {
            this.logger.debug("Could not find any Docker container with name [{}].", containerName);
            // There's no container with the specified name.
            return null;
        }

        this.logger.debug("Inspecting the state of the Docker container [{}].", containers.get(0).getId());
        InspectContainerResponse container = exec(this.client.inspectContainerCmd(containers.get(0).getId()));
        if (container.getState().getDead() == Boolean.TRUE) {
            this.logger.debug("Docker container [{}] is dead. Removing it.", container.getId());
            // The container is not reusable. Try to remove it so it can be recreated.
            exec(this.client.removeContainerCmd(container.getId()));
            return null;
        } else if (container.getState().getPaused() == Boolean.TRUE) {
            this.logger.debug("Docker container [{}] is paused. Unpausing it.", container.getId());
            exec(this.client.unpauseContainerCmd(container.getId()));
        } else if (container.getState().getRunning() != Boolean.TRUE
            && container.getState().getRestarting() != Boolean.TRUE) {
            this.logger.debug("Docker container [{}] is neither running nor restarting. Starting it.",
                container.getId());
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
            exec(this.client.inspectImageCmd(imageName));
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    /**
     * Pull the specified Docker image.
     *
     * @param imageName the image to pull
     */
    public void pullImage(String imageName)
    {
        this.logger.debug("Pulling the Docker image [{}].", imageName);
        wait(this.client.pullImageCmd(imageName));
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
        this.logger.debug("Creating a Docker container with name [{}] using image [{}], remote debugging port [{}]"
            + " and parameters [{}].", containerName, imageName, remoteDebuggingPort, parameters);
        ExposedPort exposedPort = ExposedPort.tcp(remoteDebuggingPort);
        Ports portBindings = new Ports();
        portBindings.bind(exposedPort, Ports.Binding.bindPort(remoteDebuggingPort));

        CreateContainerResponse container =
            exec(this.client.createContainerCmd(imageName).withCmd(parameters).withExposedPorts(exposedPort)
                // The extra host is needed in order to be able to access the XWiki instance running on the same machine
                // as the Docker container itself.
                .withHostConfig(HostConfig.newHostConfig()
                    .withExtraHosts(this.configuration.getChromeDockerHostName() + ":host-gateway").withBinds(
                        // Make sure it also works when XWiki is running in Docker.
                        new Bind(DOCKER_SOCK, new Volume(DOCKER_SOCK)))
                    .withPortBindings(portBindings))
                .withName(containerName));
        this.logger.debug("Created the Docker container with id [{}].", container.getId());
        return container.getId();
    }

    /**
     * Start the specified container.
     *
     * @param containerId the id of the container to start
     */
    public void startContainer(String containerId)
    {
        this.logger.debug("Starting the Docker container with id [{}].", containerId);
        exec(this.client.startContainerCmd(containerId));
    }

    /**
     * Stop the specified container.
     * 
     * @param containerId the if of the container to stop
     */
    public void stopContainer(String containerId)
    {
        if (containerId != null) {
            this.logger.debug("Stopping the Docker container with id [{}].", containerId);
            exec(this.client.stopContainerCmd(containerId));

            // Wait for the container to be fully stopped before continuing.
            this.logger.debug("Wait for the Docker container [{}] to stop.", containerId);
            wait(this.client.waitContainerCmd(containerId));
        }
    }

    private void wait(AsyncDockerCmd<?, ?> command)
    {
        // Can't write simply try(command) because spoon complains about "Variable resource not allowed here for source
        // level below 9".
        try (AsyncDockerCmd<?, ?> cmd = command) {
            cmd.start().awaitCompletion();
        } catch (InterruptedException e) {
            this.logger.warn("Interrupted thread [{}]. Root cause: [{}].", Thread.currentThread().getName(),
                ExceptionUtils.getRootCauseMessage(e));
            // Restore the interrupted state.
            Thread.currentThread().interrupt();
        }
    }

    private <T> T exec(SyncDockerCmd<T> command)
    {
        // Can't write simply try(command) because spoon complains about "Variable resource not allowed here for source
        // level below 9".
        try (SyncDockerCmd<T> cmd = command) {
            return cmd.exec();
        }
    }
}
