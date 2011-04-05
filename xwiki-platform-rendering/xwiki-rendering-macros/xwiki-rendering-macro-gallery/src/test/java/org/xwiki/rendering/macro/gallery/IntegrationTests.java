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
package org.xwiki.rendering.macro.gallery;

import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.test.integration.RenderingTestSuite;
import org.xwiki.skinx.SkinExtension;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 3.0RC1
 */
@RunWith(RenderingTestSuite.class)
public class IntegrationTests
{
    @RenderingTestSuite.Initialized
    public void initialize(ComponentManager componentManager) throws Exception
    {
        Mockery mockery = new JUnit4Mockery();
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
    private void registerMockSkinExtension(ComponentManager componentManager, Mockery mockery, String hint)
        throws Exception
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
