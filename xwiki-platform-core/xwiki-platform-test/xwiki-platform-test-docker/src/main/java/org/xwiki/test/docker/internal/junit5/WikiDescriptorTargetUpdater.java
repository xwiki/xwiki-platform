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

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UseWikiDescriptorTarget;
import org.xwiki.test.docker.junit5.WikiDescriptorTarget;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.TestUtils;

/**
 * Makes the wiki descriptor point to the host/port expected by the currently executing test, as specified with the
 * {@link UseWikiDescriptorTarget} annotation.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
public final class WikiDescriptorTargetUpdater
{
    private WikiDescriptorTargetUpdater()
    {
        // Prevents instantiation.
    }

    /**
     * Resolves the target expected by the currently executing test: the {@link UseWikiDescriptorTarget} annotation is
     * searched on the test method first and then on the test class and its enclosing classes, so that the closest
     * declaration wins. The value configured for the whole XWiki instance, that is
     * {@link org.xwiki.test.docker.junit5.UITest#wikiDescriptorTarget()} possibly overridden with the
     * {@code xwiki.test.ui.wikiDescriptorTarget} system property, is used when there's no annotation.
     *
     * @param extensionContext the context of the currently executing test
     * @param testConfiguration the configuration of the XWiki instance shared by all the tests
     * @return the target the wiki descriptor should point to
     */
    public static WikiDescriptorTarget resolve(ExtensionContext extensionContext, TestConfiguration testConfiguration)
    {
        Optional<ExtensionContext> currentContext = Optional.of(extensionContext);
        while (currentContext.isPresent()) {
            Optional<AnnotatedElement> element = currentContext.get().getElement();
            if (element.isPresent()) {
                Optional<UseWikiDescriptorTarget> annotation =
                    AnnotationSupport.findAnnotation(element.get(), UseWikiDescriptorTarget.class);
                if (annotation.isPresent()) {
                    return annotation.get().value();
                }
            }
            currentContext = currentContext.get().getParent();
        }

        return testConfiguration.getWikiDescriptorTarget();
    }

    /**
     * Updates the main wiki descriptor so that it points to the host/port expected by the currently executing test.
     * Since the XWiki instance is shared by all the tests, nothing is done when the descriptor already points to the
     * expected target.
     *
     * @param extensionContext the context of the currently executing test
     * @param testConfiguration the configuration of the XWiki instance shared by all the tests
     * @param setup the helper used to update the descriptor
     * @throws Exception when the descriptor cannot be updated
     */
    public static void apply(ExtensionContext extensionContext, TestConfiguration testConfiguration, TestUtils setup)
        throws Exception
    {
        WikiDescriptorTarget target = resolve(extensionContext, testConfiguration);
        XWikiExecutor executor = DockerTestUtils.getCurrentXWikiExecutor(extensionContext);
        boolean isBrowser = target == WikiDescriptorTarget.BROWSER;
        String host = isBrowser ? executor.getBrowserHost() : executor.getHttpClientHost();
        int port = isBrowser ? executor.getBrowserPort() : executor.getHttpClientPort();

        // Tests always access XWiki over HTTP.
        setup.setMainWikiDescriptorTarget(host, port, false);
    }
}
