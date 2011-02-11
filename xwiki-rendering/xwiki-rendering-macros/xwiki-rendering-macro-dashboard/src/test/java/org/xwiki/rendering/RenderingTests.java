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

import java.util.Arrays;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.macro.dashboard.Gadget;
import org.xwiki.rendering.macro.dashboard.GadgetReader;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.ComponentManagerTestSetup;

import junit.framework.Test;
import junit.framework.TestCase;

/**
 * All Rendering integration tests defined in text files using a special format.
 * 
 * @version $Id$
 * @since 3.0M3
 */
public class RenderingTests extends TestCase
{
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test Dashboard Macro");

        ComponentManagerTestSetup testSetup = new ComponentManagerTestSetup(suite);
        setUpMocks(testSetup.getComponentManager());

        return testSetup;
    }

    public static void setUpMocks(EmbeddableComponentManager componentManager) throws Exception
    {
        Mockery mockery = new Mockery();

        // Since we have a dependency on xwiki-core the Context Component Manager will be found and the test will try
        // to look up the Dashboard macro in the User and Wiki Component Manager and thus need a Current User and a
        // Current Wiki. It's easier for this test to simply unregister the Context Component Manager rather than
        // have to provide mocks for them.
        componentManager.unregisterComponent(ComponentManager.class, "context");

        final SkinExtension mockSsfx = mockery.mock(SkinExtension.class, "ssfxMock");
        mockery.checking(new Expectations()
        {
            {
                allowing(mockSsfx).use(with("uicomponents/container/columns.css"), with(any(Map.class)));
                allowing(mockSsfx).use(with("uicomponents/dashboard/dashboard.css"), with(any(Map.class)));
            }
        });
        DefaultComponentDescriptor<SkinExtension> ssfxDesc = new DefaultComponentDescriptor<SkinExtension>();
        ssfxDesc.setRole(SkinExtension.class);
        ssfxDesc.setRoleHint("ssfx");
        componentManager.registerComponent(ssfxDesc, mockSsfx);

        final GadgetReader mockGadgetReader = mockery.mock(GadgetReader.class);
        mockery.checking(new Expectations()
        {
            {
                allowing(mockGadgetReader).getGadgets(with(any(MacroTransformationContext.class)));
                will(returnValue(Arrays.asList(new Gadget("0", Arrays.<Block> asList(new WordBlock("title")), Arrays
                    .<Block> asList(new WordBlock("content")), "1,1"))));
            }
        });
        DefaultComponentDescriptor<GadgetReader> descriptorGR = new DefaultComponentDescriptor<GadgetReader>();
        descriptorGR.setRole(GadgetReader.class);
        componentManager.registerComponent(descriptorGR, mockGadgetReader);
    }
}
