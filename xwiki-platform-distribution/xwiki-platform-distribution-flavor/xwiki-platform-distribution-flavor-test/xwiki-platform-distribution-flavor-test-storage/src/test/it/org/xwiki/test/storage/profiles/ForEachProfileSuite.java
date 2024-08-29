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
package org.xwiki.test.storage.profiles;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.extensions.cpsuite.ClassTester;
import org.junit.extensions.cpsuite.ClasspathClassesFinder;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.xwiki.component.phase.Initializable;
import org.xwiki.test.integration.XWikiExecutor;

/**
 * Run all tests in multiple configuration profiles and start/stop XWiki for each profile. Run all tests found in the
 * current classloader using <a href="http://www.johanneslink.net/projects/cpsuite.jsp">cpsuite</a> (we extend it).
 * Tests can be filtered by passing the "pattern" System Property.
 * 
 * @version $Id$
 * @since 3.0RC1
 */
public class ForEachProfileSuite extends ClasspathSuite
{
    public static final String PATTERN = ".*" + System.getProperty("pattern", "");

    public ForEachProfileSuite(Class< ? > klass, RunnerBuilder builder) throws InitializationError
    {
        super(klass, builder);
    }

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

    @Override
    public void run(RunNotifier notifier)
    {
        // Get the list of test profiles.
        final List<Class< ? >> profiles =
            new ClasspathClassesFinder(IsProfileTester.INSTANCE, "java.class.path").find();

        final Map<Profile, XWikiExecutor> executorByProfile = new HashMap<Profile, XWikiExecutor>();
        for (int i = 0; i < profiles.size(); i++) {
            try {
                // All executors are #0 because they will not be run in parallel.
                executorByProfile.put(((Class<Profile>) profiles.get(i)).newInstance(), new XWikiExecutor(0));
            } catch (Exception e) {
                throw new RuntimeException("Failed to instanciate configuration profile.", e);
            }
        }

        // Callback to setup executors in the suite class.
        try {
            for (Profile profile : executorByProfile.keySet()) {
                profile.apply(executorByProfile.get(profile));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize XWiki Executors", e);
        }

        for (final Profile profile : executorByProfile.keySet()) {
            final XWikiExecutor executor = executorByProfile.get(profile);
            try {

                try {
                    executor.start();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to start XWiki", e);
                }

                try {
                    Object instance = this.getTestClass().getJavaClass().newInstance();

                    // If there is a field which is an XWikiExecutor type
                    // and has an @Inject annotation, inject the current executor.
                    for (Field field : this.getTestClass().getJavaClass().getDeclaredFields()) {
                        if (field.getType() == XWikiExecutor.class && field.getAnnotation(Inject.class) != null) {
                            field.setAccessible(true);
                            field.set(instance, executor);
                        }
                    }

                    // If the class is initializable then call initialize.
                    final Class< ? >[] interfaces = this.getTestClass().getJavaClass().getInterfaces();
                    for (int i = 0; i < interfaces.length; i++) {
                        if (interfaces[i] == Initializable.class) {
                            this.getTestClass().getJavaClass().getMethod("initialize").invoke(instance);
                        }
                    }

                } catch (Exception e) {
                    throw new RuntimeException("Failed to prepare tests to run in config profile.", e);
                }

                super.run(notifier);
            } finally {
                try {
                    executor.stop();
                } catch (Exception e) {
                    // Squash this and let the original exception be thrown.
                }
            }
        }
    }

    /**
     * Tester which will help ClassPathSuite find all Profiles.
     */
    private static final class IsProfileTester implements ClassTester
    {
        public static IsProfileTester INSTANCE = new IsProfileTester();

        private final String packageName = this.getClass().getPackage().getName();

        @Override
        public boolean acceptClass(Class< ? > klass)
        {
            final Class< ? >[] interfaces = klass.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                if (interfaces[i] == Profile.class) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean acceptClassName(String className)
        {
            return className.startsWith(this.packageName);
        }

        @Override
        public boolean acceptInnerClass()
        {
            return false;
        }

        @Override
        public boolean searchInJars()
        {
            return false;
        }
    }
}
