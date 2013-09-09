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
package org.xwiki.wikistream.test.integration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;

/**
 * 
 * @version $Id$
 * @since 5.2M2
 */
public class WikiStreamTestSuite extends Suite
{
    private static final TestDataGenerator GENERATOR = new TestDataGenerator();

    private static final String DEFAULT_PATTERN = ".*\\.test";

    private final Object klassInstance;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface Initialized
    {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface Scope
    {
        /**
         * @return the classpath prefix to search in
         */
        String value() default "";

        /**
         * @return the regex pattern to filter *.test files to execute
         */
        String pattern() default DEFAULT_PATTERN;
    }

    private class TestClassRunnerForParameters extends BlockJUnit4ClassRunner
    {
        private final MockitoComponentManager mockitoComponentManager = new MockitoComponentManager();

        private final TestConfiguration configuration;

        TestClassRunnerForParameters(Class< ? > type, TestConfiguration configuration) throws InitializationError
        {
            super(type);

            this.configuration = configuration;
        }

        @Override
        public Object createTest() throws Exception
        {
            return getTestClass().getOnlyConstructor().newInstance(
                new Object[] {this.configuration, getComponentManager()});
        }

        @Override
        protected String getName()
        {
            return this.configuration.name != null ? this.configuration.name : super.getName();
        }

        @Override
        protected String testName(final FrameworkMethod method)
        {
            return getName();
        }

        @Override
        protected void validateConstructor(List<Throwable> errors)
        {
            validateOnlyOneConstructor(errors);
        }

        @Override
        protected Statement classBlock(RunNotifier notifier)
        {
            return childrenInvoker(notifier);
        }

        /**
         * Initialize the Component Manager and call all methods annotated with {@link Initialized} in the suite, before
         * each test is executed, to ensure test isolation.
         */
        @Override
        protected void runChild(FrameworkMethod method, RunNotifier notifier)
        {
            initializeComponentManager(notifier);

            // Check all methods for a ComponentManager annotation and call the found ones.
            try {
                for (Method klassMethod : klassInstance.getClass().getMethods()) {
                    Initialized componentManagerAnnotation = klassMethod.getAnnotation(Initialized.class);
                    if (componentManagerAnnotation != null) {
                        // Call it!
                        klassMethod.invoke(klassInstance, getComponentManager());
                    }
                }
            } catch (Exception e) {
                notifier.fireTestFailure(new Failure(getDescription(), new RuntimeException(
                    "Failed to call Component Manager initialization method", e)));
            }

            try {
                super.runChild(method, notifier);
            } finally {
                shutdownComponentManager(notifier);
            }
        }

        private void initializeComponentManager(RunNotifier notifier)
        {
            try {
                this.mockitoComponentManager.initializeTest(klassInstance);
                this.mockitoComponentManager.registerMemoryConfigurationSource();
            } catch (Exception e) {
                notifier.fireTestFailure(new Failure(getDescription(), new RuntimeException(
                    "Failed to initialize Component Manager", e)));
            }

        }

        private void shutdownComponentManager(RunNotifier notifier)
        {
            try {
                this.mockitoComponentManager.shutdownTest();
            } catch (Exception e) {
                notifier.fireTestFailure(new Failure(getDescription(), new RuntimeException(
                    "Failed to shutdown Component Manager", e)));
            }
        }

        private ComponentManager getComponentManager() throws Exception
        {
            return this.mockitoComponentManager;
        }
    }

    private final ArrayList<Runner> runners = new ArrayList<Runner>();

    /**
     * Only called reflectively. Do not use programmatically.
     */
    public WikiStreamTestSuite(Class< ? > klass) throws Throwable
    {
        super(klass, Collections.<Runner> emptyList());

        try {
            this.klassInstance = klass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct instance of [" + klass.getName() + "]", e);
        }

        // If a Scope Annotation is present then use it to define the scope
        Scope scopeAnnotation = klass.getAnnotation(Scope.class);
        String packagePrefix = "";
        String pattern = DEFAULT_PATTERN;
        if (scopeAnnotation != null) {
            packagePrefix = scopeAnnotation.value();
            pattern = scopeAnnotation.pattern();
        }

        for (TestConfiguration testConfiguration : GENERATOR.generateData(packagePrefix, pattern)) {
            this.runners.add(new TestClassRunnerForParameters(WikiStreamTest.class, testConfiguration));
        }
    }

    @Override
    protected List<Runner> getChildren()
    {
        return this.runners;
    }

    /**
     * {@inheritDoc} We override this method so that the JUnit results are not displayed in a test hierarchy with a
     * single test result for each node (as it would be otherwise since RenderingTest has a single test method).
     */
    @Override
    public Description getDescription()
    {
        return Description.createSuiteDescription(getTestClass().getJavaClass());
    }
}
