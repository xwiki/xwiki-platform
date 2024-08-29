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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.xwiki.test.docker.junit5.DockerTestException;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;

/**
 * Resolve {@link TestConfiguration} by finding and parsing {@link UITest} annotations on the current class and
 * on nested classes too, and merge them.
 *
 * @version $Id$
 * @since 12.8RC1
 */
public class ExtensionContextTestConfigurationResolver
{
    private UITestTestConfigurationResolver uiTestResolver = new UITestTestConfigurationResolver();

    /**
     * Resolve {@link TestConfiguration} by finding and parsing {@link UITest} annotations on the current class and
     * on nested classes too, and merge them.
     *
     * @param extensionContext the test context used to get the annotation and test class
     * @return the merged Test configuration
     */
    public TestConfiguration resolve(ExtensionContext extensionContext)
    {
        UITest uiTest = extensionContext.getRequiredTestClass().getAnnotation(UITest.class);
        Class<?> topLevelTestClass = extensionContext.getRequiredTestClass();

        TestConfiguration testConfiguration = this.uiTestResolver.resolve(uiTest);
        for (Class<?> nestedTestClass : topLevelTestClass.getDeclaredClasses()) {
            UITest nestedUITest = getAnnotationFromClassAndParent(nestedTestClass);
            if (nestedUITest != null) {
                try {
                    testConfiguration.merge(this.uiTestResolver.resolve(nestedUITest));
                } catch (DockerTestException e) {
                    throw new RuntimeException("Failed to merge @UITest annotation", e);
                }
            }
        }
        return testConfiguration;
    }

    private UITest getAnnotationFromClassAndParent(Class<?> clazz)
    {
        UITest uiTest;
        Class<?> current = clazz;
        boolean shouldStop = false;
        do {
            uiTest = current.getAnnotation(UITest.class);
            if (uiTest == null) {
                // Look in parent
                if (clazz.getSuperclass() != null) {
                    current = current.getSuperclass();
                } else {
                    shouldStop = true;
                }
            } else {
                shouldStop = true;
            }
        } while (!shouldStop);
        return uiTest;
    }
}
