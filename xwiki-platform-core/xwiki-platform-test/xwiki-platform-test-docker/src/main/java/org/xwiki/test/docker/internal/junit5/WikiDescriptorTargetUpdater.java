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

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UseWikiDescriptorTarget;
import org.xwiki.test.docker.junit5.WikiDescriptorTarget;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.wiki.internal.descriptor.document.XWikiServerClassDocumentInitializer;
import org.xwiki.wiki.internal.descriptor.document.XWikiServerXwikiDocumentInitializer;

import com.xpn.xwiki.XWiki;

/**
 * Makes the wiki descriptor point to the host/port expected by the currently executing test, as specified with the
 * {@link UseWikiDescriptorTarget} annotation.
 *
 * @version $Id$
 * @since 16.10.19
 * @since 17.10.11
 * @since 18.4.4
 * @since 18.7.0RC1
 */
public final class WikiDescriptorTargetUpdater
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiDescriptorTargetUpdater.class);

    private static final String STORE_KEY = "wikiDescriptorTarget";

    private static final DocumentReference MAIN_WIKI_DESCRIPTOR = new DocumentReference(XWiki.DEFAULT_MAIN_WIKI,
        XWiki.SYSTEM_SPACE, XWikiServerXwikiDocumentInitializer.DOCUMENT_NAME);

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
     * @param setup the helper used to update the descriptor (as superadmin, over REST, so that the browser state and
     *            the credentials used by the test are left untouched)
     * @throws Exception when the descriptor cannot be updated
     */
    public static void apply(ExtensionContext extensionContext, TestConfiguration testConfiguration, TestUtils setup)
        throws Exception
    {
        WikiDescriptorTarget target = resolve(extensionContext, testConfiguration);
        ExtensionContext.Store store = DockerTestUtils.getStore(extensionContext);
        if (target == store.get(STORE_KEY, WikiDescriptorTarget.class)) {
            return;
        }

        XWikiExecutor executor = DockerTestUtils.getCurrentXWikiExecutor(extensionContext);
        boolean isBrowser = target == WikiDescriptorTarget.BROWSER;
        String host = isBrowser ? executor.getBrowserHost() : executor.getHttpClientHost();
        int port = isBrowser ? executor.getBrowserPort() : executor.getHttpClientPort();

        LOGGER.info("(*) Making the wiki descriptor target the [{}] at [{}:{}]...", target, host, port);

        // Note: the test may have logged in as another user, and thus have changed the credentials used for REST calls.
        UsernamePasswordCredentials previousCredentials =
            setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);
        try {
            org.xwiki.rest.model.jaxb.Object descriptorObject = setup.rest().object(MAIN_WIKI_DESCRIPTOR,
                XWikiServerClassDocumentInitializer.SERVER_CLASS_STRING);
            descriptorObject.withProperties(
                TestUtils.RestTestUtils.property(XWikiServerClassDocumentInitializer.FIELD_SERVER, host),
                TestUtils.RestTestUtils.property(XWikiServerClassDocumentInitializer.FIELD_PORT, port),
                // Tests always access XWiki over HTTP.
                TestUtils.RestTestUtils.property(XWikiServerClassDocumentInitializer.FIELD_SECURE, 0));
            setup.rest().update(descriptorObject);
        } finally {
            setup.setDefaultCredentials(previousCredentials);
        }

        store.put(STORE_KEY, target);
    }
}
