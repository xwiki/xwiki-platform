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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;
import org.xwiki.test.docker.internal.junit5.browser.XWikiBrowserWebDriverContainer;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.browser.Browser;

import static org.xwiki.test.docker.junit5.browser.Browser.CHROME;

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

    private static final String LATEST = "latest";

    private static final String SELENIUM_FIREFOX_DOCKER_IMAGE_NAME = "selenium/standalone-firefox:%s";

    private static final String SELENIUM_CHROME_DOCKER_IMAGE_NAME = "selenium/standalone-chrome:%s";

    private static final String SELENIUM_CHROMIUM_DOCKER_IMAGE_NAME = "selenium/standalone-chromium:%s";

    private static final boolean IS_ARM64 = "aarch64".equals(System.getProperty("os.arch"));

    private static final long DAY = 1000L * 60L * 60L * 24L;

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
            DockerImageName din = getSeleniumDockerImageName(testConfiguration);
            if (!pulledImages.contains(din.asCanonicalNameString())) {
                pullImage(din);
                pulledImages.add(din.asCanonicalNameString());
            }
        }
    }

    /**
     * @param testConfiguration the configuration to build (database, debug mode, etc). Used to verify what browser is
     *         being asked so that we return an appropriate image for it
     * @return the docker image to be used for BrowserWebDriverContainer
     */
    public static DockerImageName getSeleniumDockerImageName(TestConfiguration testConfiguration)
    {
        return IS_ARM64 && Browser.CHROME.equals(testConfiguration.getBrowser())
            ? DockerImageName.parse(getImageName(testConfiguration, true))
              .asCompatibleSubstituteFor(getImageName(testConfiguration, false))
            : DockerImageName.parse(getImageName(testConfiguration, false));
    }

    private static void pullImage(DockerImageName imageName)
    {
        DurationImagePullPolicy pullPolicy = new DurationImagePullPolicy(DAY);
        try {
            // Delegate the pull to TestContainers so that we benefit from its retry logic on transient registry
            // errors, from its image name substitution and from its local image cache handling.
            //
            // Note that we can't simply set this policy on the container: BrowserWebDriverContainer#configure()
            // calls GenericContainer#setDockerImageName() which replaces the RemoteDockerImage (and thus drops any
            // policy set on the container) with one using the default policy. That's the very bug this class works
            // around, see https://github.com/testcontainers/testcontainers-java/issues/4608. Since we create and
            // resolve the RemoteDockerImage ourselves here, the policy is honored.
            //
            // Only pull once a day to avoid the dockerhub pull rate limit, and to reduce the number of times a
            // registry outage can break the build.
            new RemoteDockerImage(imageName).withImagePullPolicy(pullPolicy).get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                // Restore interrupted state to be a good citizen...
                Thread.currentThread().interrupt();
            }

            // The policy records the pull date before the pull actually happens, so forget it again: we don't want a
            // registry outage to freeze the browser version for a whole day.
            pullPolicy.clearPullDate(imageName);

            // We couldn't reach the registry. We only pull here to make sure that we test with the latest browser
            // version, so don't fail the whole test module because of an infrastructure problem: TestContainers
            // still pulls the image itself when starting the container if it's missing locally (that's its default
            // pull policy, only the "keep it up to date" part is broken by the bug mentioned above), and otherwise
            // the tests run with the locally-available image.
            LOGGER.warn("Failed to pull image [{}]. The tests will run with the locally-available image, which may "
                + "not be the latest one. Root cause: [{}]", imageName, ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private static String getImageTag(TestConfiguration testConfiguration)
    {
        return (StringUtils.isBlank(testConfiguration.getBrowserTag())) ? LATEST : testConfiguration.getBrowserTag();
    }

    private static String getImageName(TestConfiguration testConfiguration, boolean useChromium)
    {
        String imageTag = getImageTag(testConfiguration);
        String baseImageName;
        if (useChromium) {
            baseImageName = SELENIUM_CHROMIUM_DOCKER_IMAGE_NAME;
        } else {
            boolean isChrome = CHROME.equals(testConfiguration.getBrowser());
            baseImageName = isChrome ? SELENIUM_CHROME_DOCKER_IMAGE_NAME : SELENIUM_FIREFOX_DOCKER_IMAGE_NAME;
        }
        return String.format(baseImageName, imageTag);
    }
}
