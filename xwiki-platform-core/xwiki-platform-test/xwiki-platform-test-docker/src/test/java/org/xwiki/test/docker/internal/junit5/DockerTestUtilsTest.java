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

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.browser.Browser;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DockerTestUtils}.
 *
 * @version $Id$
 */
public class DockerTestUtilsTest
{
    @UITest(servletEngine = ServletEngine.TOMCAT, browser = Browser.CHROME, database = Database.MYSQL)
    public class MyClass
    {
    }

    @Test
    public void getResultFileLocation() throws Exception
    {
        UITest uiTest = DockerTestUtilsTest.MyClass.class.getAnnotation(UITest.class);
        UITestTestConfigurationResolver resolver = new UITestTestConfigurationResolver();
        TestConfiguration configuration = resolver.resolve(uiTest);
        ExtensionContext extensionContext = mock(ExtensionContext.class);
        when(extensionContext.getRequiredTestClass()).thenReturn((Class) DockerTestUtilsTest.class);
        when(extensionContext.getRequiredTestMethod()).thenReturn(
            DockerTestUtilsTest.class.getDeclaredMethod("getResultFileLocation"));
        when(extensionContext.getUniqueId()).thenReturn("...[test-template-invocation:#25]");

        assertEquals("./target/screenshots/mysql-default-default-tomcat-default-chrome-"
            + "org.xwiki.test.docker.internal.junit5.DockerTestUtilsTest-getResultFileLocation25.test",
            DockerTestUtils.getResultFileLocation("test", configuration, extensionContext).toString());
    }

    @Test
    void getStoreReturnsTheStoreOfTheTopLevelTestClass()
    {
        ExtensionContext rootContext = mockContext(null);
        ExtensionContext firstClassContext = mockContext(rootContext);
        ExtensionContext nestedClassContext = mockContext(firstClassContext);
        ExtensionContext methodContext = mockContext(nestedClassContext);
        ExtensionContext secondClassContext = mockContext(rootContext);

        ExtensionContext.Store firstClassStore = DockerTestUtils.getStore(firstClassContext);
        ExtensionContext.Store secondClassStore = DockerTestUtils.getStore(secondClassContext);

        // The nested test classes and the test methods share the store of their top level test class.
        assertSame(firstClassStore, DockerTestUtils.getStore(nestedClassContext));
        assertSame(firstClassStore, DockerTestUtils.getStore(methodContext));
        // Another top level test class, executed in the same JVM, has its own store.
        assertNotSame(firstClassStore, secondClassStore);
        assertNotSame(DockerTestUtils.getStore(rootContext), firstClassStore);
        assertNotSame(DockerTestUtils.getStore(rootContext), secondClassStore);
    }

    private ExtensionContext mockContext(ExtensionContext parent)
    {
        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getParent()).thenReturn(Optional.ofNullable(parent));
        ExtensionContext.Store store = mock(ExtensionContext.Store.class);
        when(context.getStore(any())).thenReturn(store);
        return context;
    }
}
