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
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.xwiki.test.docker.junit5.MultiUserTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.ui.PersistentTestContext;
import org.xwiki.test.ui.TestUtils;

/**
 * Add support for injecting {@link MultiUserTestUtils} as a parameter in JUnit 5 tests.
 *
 * @version $Id$
 * @since 15.10.12
 * @since 16.4.1
 * @since 16.6.0RC1
 */
public class MultiUserTestUtilsParameterResolver implements ParameterResolver
{
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    {
        return parameterContext.getParameter().getType() == MultiUserTestUtils.class;
    }

    @Override
    public MultiUserTestUtils resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    {
        TestConfiguration testConfiguration = DockerTestUtils.getTestConfiguration(extensionContext);

        ExtensionContext.Store store = DockerTestUtils.getStore(extensionContext);
        PersistentTestContext testContext = store.get(PersistentTestContext.class, PersistentTestContext.class);
        TestUtils testUtils = testContext.getUtil();

        return new MultiUserTestUtils(testUtils, testConfiguration);
    }
}
