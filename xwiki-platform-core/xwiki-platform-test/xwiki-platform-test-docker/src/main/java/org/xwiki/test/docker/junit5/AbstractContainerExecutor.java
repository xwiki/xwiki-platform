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
package org.xwiki.test.docker.junit5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

/**
 * Common code for container execution.
 *
 * @version $Id$
 * @since 10.10RC1
 */
public abstract class AbstractContainerExecutor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractContainerExecutor.class);

    protected void start(GenericContainer container, TestConfiguration testConfiguration)
    {
        if (testConfiguration.isVerbose()) {
            LOGGER.info(String.format("Docker image used: [%s]", container.getDockerImageName()));
            container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(this.getClass())));
        }

        // Get the latest image in case the tag has been updated on dockerhub.
        if (!testConfiguration.isOffline()) {
            container.getDockerClient().pullImageCmd(container.getDockerImageName());
        }

        container.start();

        // Display logs after the container has been started so that we can see problems happening in the containers
        DockerTestUtils.followOutput(container, getClass());
    }

    /**
     * Utility method to mount a directory from the host to the container with some optimization for different OS.
     * @param container the container on which to mount the directory.
     * @param sourceDirectory the path of the directory on the host to mount in the container.
     * @param targetDirectory the target where to mount the directory in the container.
     * @since 10.11RC1
     */
    protected static void mountFromHostToContainer(GenericContainer container, String sourceDirectory,
        String targetDirectory)
    {
        // File mounting is awfully slow on Mac OSX. For example starting Tomcat with XWiki mounted takes
        // 45s+, while doing a COPY first and then starting Tomcat takes 8s (+5s for the copy).
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("mac os x")) {
            MountableFile mountableDirectory = MountableFile.forHostPath(sourceDirectory);
            container.withCopyFileToContainer(mountableDirectory, targetDirectory);
        } else {
            container.withFileSystemBind(sourceDirectory, targetDirectory);
        }
    }
}
