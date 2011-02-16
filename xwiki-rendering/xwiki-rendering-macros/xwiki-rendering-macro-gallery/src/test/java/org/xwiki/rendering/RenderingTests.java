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
package org.xwiki.rendering;

import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.ComponentManagerTestSetup;

/**
 * All rendering integration tests defined in text files using a special format.
 * 
 * @version $Id$
 * @since 3.0M3
 */
public class RenderingTests extends TestCase
{
    /**
     * Creates the test suite.
     * 
     * @return the test suite
     * @throws Exception is creating the test suite fails
     */
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test Gallery Macro");

        ComponentManagerTestSetup testSetup = new ComponentManagerTestSetup(suite);
        setUpMocks(testSetup.getComponentManager());

        return testSetup;
    }

    /**
     * Registers mock components to the given component manager.
     * 
     * @param componentManager the component manager were to register the mock components
     */
    private static void setUpMocks(EmbeddableComponentManager componentManager)
    {
        Mockery mockery = new Mockery();
        registerMockSkinExtension(componentManager, mockery, "jsfx");
        registerMockSkinExtension(componentManager, mockery, "ssfx");
    }

    /**
     * Registers a mock skin extension component with the given role hint.
     * 
     * @param componentManager the component manager where to register the mock skin extension component
     * @param mockery the object used to mock the skin extension component
     * @param hint the role hint to use for the skin extension component
     */
    @SuppressWarnings("unchecked")
    private static void registerMockSkinExtension(EmbeddableComponentManager componentManager, Mockery mockery,
        String hint)
    {
        final SkinExtension skinExtension = mockery.mock(SkinExtension.class, hint);
        DefaultComponentDescriptor<SkinExtension> descriptor = new DefaultComponentDescriptor<SkinExtension>();
        descriptor.setRole(SkinExtension.class);
        descriptor.setRoleHint(hint);
        componentManager.registerComponent(descriptor, skinExtension);

        mockery.checking(new Expectations()
        {
            {
                allowing(skinExtension).use(with(aNonNull(String.class)), with(aNonNull(Map.class)));
            }
        });
    }
}
