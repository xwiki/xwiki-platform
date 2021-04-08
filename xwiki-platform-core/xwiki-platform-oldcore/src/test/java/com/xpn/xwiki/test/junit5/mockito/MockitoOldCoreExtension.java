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
package com.xpn.xwiki.test.junit5.mockito;

import java.lang.reflect.Field;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.test.junit5.mockito.MockitoComponentManagerExtension;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.test.MockitoOldcore;

/**
 * Initialize a {@link MockitoOldcore} object, which is useful to write oldcore tests. In addition behaves the same as
 * {@link MockitoComponentManagerExtension}. Tests can get access to the {@link MockitoOldcore} object by accepting it
 * as a parameter.
 *
 * For example:
 *
 * <pre>
 * {@code
 * &#64;OldcoreTest
 * &#64;ComponentList({
 *     Component3Impl.class
 * })
 * public class MyComponentTest
 * {
 *     &#64;Mock
 *     private List<String> list;
 *
 *     &#64;MockComponent
 *     private Component1Role component1;
 *
 *     &#64;InjectMocks
 *     &#64;InjectMockComponents
 *     private Component4Impl component4;
 *
 *     &#64;InjectMockitoOldcore
 *     private MockitoOldcore mockitoOldcore;
 *
 *     &#64;BeforeEach
 *     public void before(MockitoComponentManager componentManager, MockitoOldcore oldcore)
 *     {
 *         ...
 *     }
 *
 *     &#64;Test
 *     public void test1(MockitoOldcore oldcore)
 *     {
 *         ...
 *     }
 *
 *     &#64;Test
 *     public void test2(ComponentManager componentManager)
 *     {
 *         ...
 *     }
 * ...
 * }
 * }
 * </pre>
 *
 * @version $Id$
 * @since 10.4RC1
 */
public class MockitoOldCoreExtension extends MockitoComponentManagerExtension
{
    private static final String MOCKITO_OLDCORE = "mockitoOldcore";

    @Override
    protected void initializeMockitoComponentManager(Object testInstance, MockitoComponentManager mcm,
        ExtensionContext context) throws Exception
    {
        OldcoreTest annotation = testInstance.getClass().getAnnotation(OldcoreTest.class);
        if (annotation == null) {
            super.initializeMockitoComponentManager(testInstance, mcm, context);
            return;
        }
        
        // Create & save MockitoOldCore
        removeMockitoOldcore(context);

        MockitoOldcore oldcore = new MockitoOldcore(mcm);
        oldcore.mockXWiki(annotation.mockXWiki());

        saveMockitoOldcore(context, oldcore);

        // Inject the MockitoOldcore instance in all fields annotated with @InjectMockitoOldcore
        // Note: we inject the field before the CM init in case there's a @BeforeComponent method that wants to use
        // the @InjectMockitoOldcore field.
        for (Field field : ReflectionUtils.getAllFields(testInstance.getClass())) {
            if (field.isAnnotationPresent(InjectMockitoOldcore.class)) {
                ReflectionUtils.setFieldValue(testInstance, field.getName(), loadMockitoOldcore(context));
            }
        }

        // Initialize the CM
        mcm.initializeTest(testInstance, mcm, oldcore);

        // Initialize MockitoOldCore
        oldcore.before(context.getRequiredTestClass());
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception
    {
        MockitoOldcore moc = loadMockitoOldcore(extensionContext);
        if (moc != null) {
            moc.after();
        }

        super.afterEach(extensionContext);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException
    {
        // Support both ComponentManager & MockitoOldCore parameters
        return super.supportsParameter(parameterContext, extensionContext)
            || MockitoOldcore.class.isAssignableFrom(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException
    {
        // Support both ComponentManager & MockitoOldCore parameters
        if (MockitoOldcore.class.isAssignableFrom(parameterContext.getParameter().getType())) {
            return loadMockitoOldcore(extensionContext);
        } else {
            return super.resolveParameter(parameterContext, extensionContext);
        }
    }

    private void saveMockitoOldcore(ExtensionContext context, MockitoOldcore oldcore)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(MOCKITO_OLDCORE, oldcore);
    }

    private MockitoOldcore loadMockitoOldcore(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        return store.get(MOCKITO_OLDCORE, MockitoOldcore.class);
    }

    private void removeMockitoOldcore(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        Class<?> testClass = context.getRequiredTestClass();
        store.remove(testClass);
    }
}
