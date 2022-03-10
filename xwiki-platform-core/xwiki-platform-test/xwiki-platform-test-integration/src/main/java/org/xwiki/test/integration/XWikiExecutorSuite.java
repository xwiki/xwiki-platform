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
package org.xwiki.test.integration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts/Stop XWiki before/after all tests and run all tests found in the current classloader using <a
 * href="http://www.johanneslink.net/projects/cpsuite.jsp">cpsuite</a> (we extend it). Tests can be filtered by passing
 * the "pattern" System Property.
 * <p>
 * More details on the "pattern" System Property, its syntax and examples are found in the
 * {@link XWikiExecutorTestMethodFilter} class.
 * 
 * @version $Id$
 * @since 3.0RC1
 */
public class XWikiExecutorSuite extends ClasspathSuite
{
    public static final String PATTERN = String.format(".*(%s)", System.getProperty("pattern", ""));

    protected static final Logger LOGGER = LoggerFactory.getLogger(XWikiExecutorSuite.class);

    private static final Filter METHOD_FILTER = new XWikiExecutorTestMethodFilter(PATTERN);

    private List<XWikiExecutor> executors = new ArrayList<>();

    public XWikiExecutorSuite(Class< ? > klass, RunnerBuilder builder) throws InitializationError
    {
        super(klass, builder);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Executors
    {
        /**
         * @return the number of executors to run
         */
        int value() default 1;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PreStart
    {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PostStart
    {
    }

    public List<XWikiExecutor> getExecutors()
    {
        return this.executors;
    }

    @Override
    protected List<Runner> getChildren()
    {
        List<Runner> runners = new ArrayList<>();

        // Filter the test classes to run.
        for (Runner runner : super.getChildren()) {
            Description description = runner.getDescription();
            String runnerName = description.getClassName();

            if (runnerName.matches(PATTERN)) {
                // If the entire test class matches, add it.
                runners.add(runner);
            } else {
                // Otherwise, filter the test methods to run.
                try {
                    METHOD_FILTER.apply(runner);
                    // If the runner still has tests remaining after the filtering, add it.
                    runners.add(runner);
                } catch (NoTestsRemainException e) {
                    LOGGER.info("Skipping test class: {}", description.getClassName());
                }
            }
        }

        return runners;
    }

    /**
     * Called before test execution.
     */
    protected void beforeTests()
    {
        // Construct as many executors as specified in the Executors annotation or 1 if annotation is not present.
        int executorNb = 1;
        Executors executorsAnnotation = getTestClass().getJavaClass().getAnnotation(Executors.class);
        if (executorsAnnotation != null) {
            executorNb = executorsAnnotation.value();
        }

        for (int i = 0; i < executorNb; i++) {
            this.executors.add(new XWikiExecutor(i));
        }

        // Callback to setup executors in the suite class before containers are started
        try {
            for (Method method : getTestClass().getJavaClass().getMethods()) {
                PreStart preStartAnnotation = method.getAnnotation(PreStart.class);
                if (preStartAnnotation != null) {
                    // Call it!
                    Object instance = getTestClass().getJavaClass().newInstance();
                    method.invoke(instance, this.executors);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize XWiki Executors before start", e);
        }

        try {
            for (XWikiExecutor executor : this.executors) {
                executor.start();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to start XWiki", e);
        }

        // Callback to setup executors in the suite class after containers have been started
        try {
            for (Method method : getTestClass().getJavaClass().getMethods()) {
                PostStart postStartAnnotation = method.getAnnotation(PostStart.class);
                if (postStartAnnotation != null) {
                    // Call it!
                    Object instance = getTestClass().getJavaClass().newInstance();
                    method.invoke(instance, this.executors);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize XWiki Executors after start", e);
        }
    }

    /**
     * Called after test execution.
     */
    protected void afterTests()
    {
        try {
            for (XWikiExecutor executor : this.executors) {
                executor.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop XWiki", e);
        }
    }

    @Override
    public void run(RunNotifier notifier)
    {
        beforeTests();

        try {
            super.run(notifier);
        } finally {
            afterTests();
        }
    }
}
