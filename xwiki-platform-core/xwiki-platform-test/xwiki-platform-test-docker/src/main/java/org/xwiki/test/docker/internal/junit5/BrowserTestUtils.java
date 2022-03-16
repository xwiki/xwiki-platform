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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.xwiki.test.docker.internal.junit5.browser.XWikiBrowserWebDriverContainer;
import org.xwiki.test.docker.junit5.TestConfiguration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.PullImageResultCallback;

/**
 * Force pulling the selenium FF and Chrome docker images. Workaround for
 * <a href="https://github.com/testcontainers/testcontainers-java/issues/4608">4608</a>. Remove this class once it's
 * fixed.
 *
 * @version $Id$
 */
public final class BrowserTestUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserTestUtils.class);

    private static final String SELENIUM_FIREFOX_DOCKER_IMAGE_NAME = "selenium/standalone-firefox-debug:3.141.59";

    private static final String SELENIUM_CHROME_DOCKER_IMAGE_NAME = "selenium/standalone-chrome-debug:3.141.59";

    private static List<String> pulledImages = new ArrayList<>();

    private BrowserTestUtils()
    {
        // Prevents instantiation.
    }

    /**
     * Force pulling the selenium FF and Chrome docker images.
     *
     * @param container the container to start
     * @param testConfiguration the configuration to build (database, debug mode, etc). Used to verify if we're online
     *        to pull the image
     */
    public static void pullBrowserImages(GenericContainer<?> container, TestConfiguration testConfiguration)
    {
        if (container instanceof XWikiBrowserWebDriverContainer && !testConfiguration.isOffline()) {
            if (!pulledImages.contains(SELENIUM_FIREFOX_DOCKER_IMAGE_NAME)) {
                pullImage(container.getDockerClient(), SELENIUM_FIREFOX_DOCKER_IMAGE_NAME);
                pulledImages.add(SELENIUM_FIREFOX_DOCKER_IMAGE_NAME);

            }
            if (!pulledImages.contains(SELENIUM_CHROME_DOCKER_IMAGE_NAME)) {
                pullImage(container.getDockerClient(), SELENIUM_CHROME_DOCKER_IMAGE_NAME);
                pulledImages.add(SELENIUM_CHROME_DOCKER_IMAGE_NAME);

            }
        }
    }

    private static void pullImage(DockerClient dockerClient, String imageName)
    {
        PullImageResultCallback pullImageResultCallback = dockerClient
            .pullImageCmd(imageName)
            .exec(new PullImageResultCallback());
        wait(pullImageResultCallback);
    }

    private static void wait(ResultCallbackTemplate template)
    {
        try {
            template.awaitCompletion();
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted thread [{}]. Root cause: [{}]", Thread.currentThread().getName(),
                org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage(e));
            // Restore interrupted state to be a good citizen...
            Thread.currentThread().interrupt();
        }
    }
}
