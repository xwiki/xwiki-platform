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

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.xwiki.test.docker.junit5.ScreenshotComparator;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * Add support for injecting {@link ScreenshotComparator} as a parameter in JUnit 5 tests.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class ScreenshotComparatorParameterResolver implements ParameterResolver
{
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    {
        return parameterContext.getParameter().getType() == ScreenshotComparator.class;
    }

    @Override
    public ScreenshotComparator resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    {
        // We use the class declaring the test method rather than the test class because they differ when the test is
        // run from a test suite, which nests it in a class of its own, and the reference screenshots are committed
        // under the name of the class that declares the test.
        Method testMethod = extensionContext.getRequiredTestMethod();
        XWikiWebDriver driver = DockerTestUtils.getStore(extensionContext).get(XWikiWebDriver.class,
            XWikiWebDriver.class);
        return new ScreenshotComparator(driver, DockerTestUtils.getTestConfiguration(extensionContext),
            testMethod.getDeclaringClass().getSimpleName(), testMethod.getName());
    }
}
