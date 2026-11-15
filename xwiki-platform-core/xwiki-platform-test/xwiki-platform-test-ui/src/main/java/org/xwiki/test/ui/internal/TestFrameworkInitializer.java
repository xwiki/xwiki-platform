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
package org.xwiki.test.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.annotation.ComponentDeclaration;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.test.ui.PersistentTestContext;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Initializes the functional test framework (page objects, test utilities and the minimal set of components they
 * need) for a given {@link PersistentTestContext}, independently of the test engine (JUnit 4 or JUnit 5) used to
 * execute the tests.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public final class TestFrameworkInitializer
{
    private TestFrameworkInitializer()
    {
        // Utility class
    }

    /**
     * @param context the test context to use for the page objects and test utilities
     * @return the component manager containing the components required by the test framework
     * @throws Exception if the components fail to be registered
     */
    public static EmbeddableComponentManager initialize(PersistentTestContext context) throws Exception
    {
        BaseElement.setContext(context);
        TestUtils.setContext(context);

        EmbeddableComponentManager componentManager = new EmbeddableComponentManager();

        // Only load the minimal number of components required for the test framework, for both performance reasons
        // and for avoiding having to declare dependencies such as HttpServletRequest.
        ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
        List<ComponentDeclaration> componentDeclarations = new ArrayList<>();
        componentDeclarations.add(new ComponentDeclaration(DefaultStringEntityReferenceResolver.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultStringEntityReferenceSerializer.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(RelativeStringEntityReferenceResolver.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultEntityReferenceProvider.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultModelConfiguration.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(DefaultSymbolScheme.class.getName()));
        componentDeclarations.add(new ComponentDeclaration(LocalStringEntityReferenceSerializer.class.getName()));
        loader.initialize(componentManager, TestFrameworkInitializer.class.getClassLoader(), componentDeclarations);

        TestUtils.initializeComponent(componentManager);

        return componentManager;
    }
}
