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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UseWikiDescriptorTarget;
import org.xwiki.test.docker.junit5.WikiDescriptorTarget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WikiDescriptorTargetUpdater}.
 *
 * @version $Id$
 */
class WikiDescriptorTargetUpdaterTest
{
    @Retention(RetentionPolicy.RUNTIME)
    @UseWikiDescriptorTarget(WikiDescriptorTarget.BROWSER)
    private @interface BrowserTest
    {
    }

    private static class NoAnnotation
    {
        void test()
        {
        }
    }

    @UseWikiDescriptorTarget(WikiDescriptorTarget.BROWSER)
    private static class OnClass
    {
        void test()
        {
        }

        @UseWikiDescriptorTarget(WikiDescriptorTarget.HTTP_CLIENT)
        void overridingTest()
        {
        }
    }

    @BrowserTest
    private static class OnMetaAnnotatedClass
    {
        void test()
        {
        }
    }

    @Test
    void resolveWhenNoAnnotationUsesTheTestConfiguration() throws Exception
    {
        assertEquals(WikiDescriptorTarget.HTTP_CLIENT, resolve(NoAnnotation.class, "test", null));
        assertEquals(WikiDescriptorTarget.BROWSER,
            resolve(NoAnnotation.class, "test", WikiDescriptorTarget.BROWSER));
    }

    @Test
    void resolveWhenAnnotationOnTestClassOverridesTheTestConfiguration() throws Exception
    {
        assertEquals(WikiDescriptorTarget.BROWSER, resolve(OnClass.class, "test", WikiDescriptorTarget.HTTP_CLIENT));
    }

    @Test
    void resolveWhenAnnotationOnTestMethodOverridesTheOneOnTheTestClass() throws Exception
    {
        assertEquals(WikiDescriptorTarget.HTTP_CLIENT,
            resolve(OnClass.class, "overridingTest", WikiDescriptorTarget.BROWSER));
    }

    @Test
    void resolveWhenMetaAnnotation() throws Exception
    {
        assertEquals(WikiDescriptorTarget.BROWSER, resolve(OnMetaAnnotatedClass.class, "test", null));
    }

    /**
     * Simulates the {@link ExtensionContext} hierarchy that JUnit passes when executing the passed test method: a
     * context for the method, whose parent is the context of the test class.
     */
    private WikiDescriptorTarget resolve(Class<?> testClass, String testMethodName,
        WikiDescriptorTarget configuredTarget) throws Exception
    {
        TestConfiguration testConfiguration = new TestConfiguration();
        testConfiguration.setWikiDescriptorTarget(configuredTarget);

        ExtensionContext classContext = mock(ExtensionContext.class);
        when(classContext.getElement()).thenReturn(Optional.of((AnnotatedElement) testClass));
        when(classContext.getParent()).thenReturn(Optional.empty());

        ExtensionContext methodContext = mock(ExtensionContext.class);
        when(methodContext.getElement())
            .thenReturn(Optional.of(testClass.getDeclaredMethod(testMethodName)));
        when(methodContext.getParent()).thenReturn(Optional.of(classContext));

        return WikiDescriptorTargetUpdater.resolve(methodContext, testConfiguration);
    }
}
