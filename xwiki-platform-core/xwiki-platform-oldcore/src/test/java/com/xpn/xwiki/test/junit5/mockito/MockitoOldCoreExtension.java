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
    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(MockitoOldCoreExtension.class);

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception
    {
        super.postProcessTestInstance(testInstance, context);

        // Inject the MockitoOldcore instance in all fields annotated with @InjectMockitoOldcore
        for (Field field : testInstance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(InjectMockitoOldcore.class)) {
                ReflectionUtils.setFieldValue(testInstance, field.getName(), loadMockitoOldcore(context));
            }
        }
    }

    @Override
    protected void initializeMockitoComponentManager(Object testInstance, MockitoComponentManager mcm,
        ExtensionContext context) throws Exception
    {
        // Create & save MockitoOldCore
        removeMockitoOldcore(context);
        MockitoOldcore oldcore = new MockitoOldcore(mcm);
        saveMockitoOldcore(context, oldcore);

        // Initialize the CM
        mcm.initializeTest(testInstance, mcm, oldcore);

        // Initialize MockitoOldCore
        oldcore.before(context.getRequiredTestClass());
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception
    {
        MockitoOldcore moc = loadMockitoOldcore(extensionContext);
        if (moc != null) {
            moc.after();
        }
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
        Class<?> testClass = context.getRequiredTestClass();
        store.put(testClass, oldcore);
    }

    private MockitoOldcore loadMockitoOldcore(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        Class<?> testClass = context.getRequiredTestClass();
        return store.get(testClass, MockitoOldcore.class);
    }

    private void removeMockitoOldcore(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        Class<?> testClass = context.getRequiredTestClass();
        store.remove(testClass);
    }

    private static ExtensionContext.Store getStore(ExtensionContext context)
    {
        return context.getRoot().getStore(NAMESPACE);
    }
}
