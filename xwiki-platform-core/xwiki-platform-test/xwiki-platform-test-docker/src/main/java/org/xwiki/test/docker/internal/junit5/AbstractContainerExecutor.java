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
package org.xwiki.test.docker.internal.junit5;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;
import org.xwiki.test.docker.junit5.TestConfiguration;

import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.isInAContainer;
import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.startContainer;

/**
 * Common code for container execution.
 *
 * @version $Id$
 * @since 10.10RC1
 */
public abstract class AbstractContainerExecutor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractContainerExecutor.class);

    protected void start(GenericContainer<?> container, TestConfiguration testConfiguration) throws Exception
    {
        if (testConfiguration.isVerbose()) {
            LOGGER.info("Docker image used: [{}]", container.getDockerImageName());
        }

        // When not in verbose mode, only print WARN and ERROR coming from the container startup
        container.withLogConsumer(new XWikiSlf4jLogConsumer(LoggerFactory.getLogger(this.getClass()),
            testConfiguration.isVerbose()));

        startContainer(container, testConfiguration);
    }

    /**
     * Utility method to mount a directory from the host to the container with some optimization for different OS.
     *
     * @param container the container on which to mount the directory.
     * @param sourceDirectory the path of the directory on the host to mount in the container.
     * @param targetDirectory the target where to mount the directory in the container.
     * @since 10.11RC1
     */
    protected void mountFromHostToContainer(GenericContainer<?> container, String sourceDirectory,
        String targetDirectory)
    {
        // Note 1: File mounting is awfully slow on Mac OSX. For example starting Tomcat with XWiki mounted takes
        // 45s+, while doing a COPY first and then starting Tomcat takes 8s (+5s for the copy).
        // Note 2: For the DOOD use case, we also do the copy instead of the volume mounting since that would require
        // to have the sourceDirectory path mounted from the host, and this is not possible since the Jenkins workspace
        // directory doesn't exist on the host (it's created only inside the agent, i.e. inside the xwiki docker build
        // container). Even if it were possible, it would prevent being able to have several builds in parallel on the
        // same host.
        String osName = System.getProperty("os.name").toLowerCase();
        if (isInAContainer() || osName.startsWith("mac os x")) {
            MountableFile mountableDirectory = MountableFile.forHostPath(sourceDirectory);
            container.withCopyFileToContainer(mountableDirectory, targetDirectory);
        } else {
            container.withFileSystemBind(sourceDirectory, targetDirectory);
        }
    }

    /**
     * Merge two set of properties and generate a String in the Docker command format.
     *
     * @param defaultCommands the default command that can be overridden
     * @param overrideCommands the command overrides
     * @return the string in the format {@code --key=value}
     */
    protected String mergeCommands(Properties defaultCommands, Properties overrideCommands)
    {
        Properties mergedCommands = new Properties();
        mergedCommands.putAll(defaultCommands);
        mergedCommands.putAll(overrideCommands);

        List<String> commands = new ArrayList<>();
        for (String key : mergedCommands.stringPropertyNames()) {
            StringBuilder builder = new StringBuilder();
            builder.append("--").append(key).append('=').append(mergedCommands.getProperty(key));
            commands.add(builder.toString());
        }

        return StringUtils.join(commands, ' ');
    }
}
