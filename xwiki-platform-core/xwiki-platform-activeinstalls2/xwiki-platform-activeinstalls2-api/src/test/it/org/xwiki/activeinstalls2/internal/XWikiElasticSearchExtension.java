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
package org.xwiki.activeinstalls2.internal;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstances;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import org.xwiki.component.util.ReflectionUtils;

/**
 * We use an extension so that we can start ES before we start XWiki (by declaring the use of this extension before
 * the use of {@code @UITest}). This is because when XWiki starts, it tries to send a ping to the ES instance, so
 * we need it running before XWiki starts.
 *
 * @version $Id$
 * @since 14.5
 */
public class XWikiElasticSearchExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver,
    BeforeEachCallback
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiElasticSearchExtension.class);

    // Provide a default version to make it easy to execute from the IDE, as otherwise you need to pass the
    // elasticsearch.version system property.
    private static final String ELASTICSEARCH_VERSION = System.getProperty("elasticsearch.version", "8.2.0");
    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(XWikiElasticSearchExtension.class);
    @Override
    public void beforeAll(ExtensionContext extensionContext)
    {
        if (isInNestedTest(extensionContext)) {
            return;
        }

        DockerImageName imageName =
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch").withTag(ELASTICSEARCH_VERSION);
        ElasticsearchContainer container = new ElasticsearchContainer(imageName);
        // - single node since we don't need a cluster for testing
        // - disabled security to avoid the warning messages and because security is not needed for testing as
        //   the data is discarded and the testing environment is secure.
        container.setEnv(List.of(
            "discovery.type=single-node",
            "xpack.security.enabled=false",
            // ES uses 50% of the available RAM by default. We don't need that much for our functional tests and this
            // can bring the build machine to its knees. Thus, we force a max RAM limit.
            "ES_JAVA_OPTS=-Xms256m -Xmx256m"));
        LOGGER.info("(*) Starting Elasticsearch [{}]...", ELASTICSEARCH_VERSION);
        container.start();

        saveContainer(extensionContext, container);
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext)
    {
        // Inject the container instance into the @InjectElasticSearchContainer fields from the test class
        Optional<TestInstances> testInstances = extensionContext.getTestInstances();
        if (testInstances.isPresent()) {
            // Initialize all test classes, including nested ones.
            for (Object testInstance : testInstances.get().getAllInstances()) {
                for (Field field : ReflectionUtils.getAllFields(testInstance.getClass())) {
                    if (field.isAnnotationPresent(InjectElasticSearchContainer.class)) {
                        ReflectionUtils.setFieldValue(testInstance, field.getName(), loadContainer(extensionContext));
                    }
                }
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext)
    {
        if (isInNestedTest(extensionContext)) {
            return;
        }

        ElasticsearchContainer container = loadContainer(extensionContext);
        if (container != null) {
            LOGGER.info("(*) Stopping Elasticsearch...");
            container.stop();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException
    {
        Class<?> type = parameterContext.getParameter().getType();
        return ElasticsearchContainer.class.isAssignableFrom(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException
    {
        return loadContainer(extensionContext);
    }

    public static ExtensionContext.Store getStore(ExtensionContext context)
    {
        return context.getRoot().getStore(NAMESPACE);
    }

    private void saveContainer(ExtensionContext context, ElasticsearchContainer container)
    {
        getStore(context).put(ElasticsearchContainer.class, container);
    }

    protected ElasticsearchContainer loadContainer(ExtensionContext context)
    {
        return getStore(context).get(ElasticsearchContainer.class, ElasticsearchContainer.class);
    }

    private boolean isInNestedTest(ExtensionContext context)
    {
        // This method is going to be called for the top level test class but also for nested test classes. We want
        // to start ES only once, and thus we only start it for the top level context.
        // Note: the top level context is the JUnitJupiterExtensionContext one, and it doesn't contain any test, and
        // thus we skip it.
        return context.getParent().get().getParent().isPresent();
    }
}
