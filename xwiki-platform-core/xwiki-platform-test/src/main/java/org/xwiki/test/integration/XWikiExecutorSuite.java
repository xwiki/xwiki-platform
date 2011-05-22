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
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/**
 * Starts/Stop XWiki before/after all tests and run all tests found in the current classloader using
 * <a href="http://www.johanneslink.net/projects/cpsuite.jsp">cpsuite</a> (we extend it).
 *
 * Tests can be filtered by passing the "pattern" System Property.
 *
 * @version $Id$
 * @since 3.0RC1
 */
public class XWikiExecutorSuite extends ClasspathSuite
{
    public static final String PATTERN = ".*" + System.getProperty("pattern", "");

    private List<XWikiExecutor> executors = new ArrayList<XWikiExecutor>();

    public XWikiExecutorSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError
    {
        super(klass, builder);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Executors {
        public int value() default 1;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface PreStart
    {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface PostStart
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Runner> getChildren()
    {
        List<Runner> runners = new ArrayList<Runner>();

        // Filter classes to run
        for (Runner runner : super.getChildren()) {
            if (runner.getDescription().getClassName().matches(PATTERN)) {
                runners.add(runner);
            }
        }

        return runners;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(RunNotifier notifier)
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
            throw new RuntimeException("Failed to initialize XWiki Executors befpre start", e);
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

        try {
            super.run(notifier);
        } finally {
            try {
                for (XWikiExecutor executor : this.executors) {
                    executor.stop();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to stop XWiki", e);
            }
        }
    }
}
