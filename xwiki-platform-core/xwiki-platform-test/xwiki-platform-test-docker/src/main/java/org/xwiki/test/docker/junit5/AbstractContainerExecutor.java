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
            container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(this.getClass())));
        }

        if (testConfiguration.isVerbose()) {
            LOGGER.info(String.format("Docker image used: [%s]", container.getDockerImageName()));
        }

        // Get the latest image in case the tag has been updated on dockerhub.
        if (!testConfiguration.isOffline()) {
            container.getDockerClient().pullImageCmd(container.getDockerImageName());
        }

        container.start();
    }
}
