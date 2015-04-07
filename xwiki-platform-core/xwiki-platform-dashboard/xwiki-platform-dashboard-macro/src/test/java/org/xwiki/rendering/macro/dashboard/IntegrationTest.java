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
package org.xwiki.rendering.macro.dashboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.action.CustomAction;
import org.junit.runner.RunWith;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.test.integration.RenderingTestSuite;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.jmock.MockingComponentManager;
import org.xwiki.velocity.VelocityManager;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 * 
 * @version $Id$
 * @since 3.0RC1
 */
@RunWith(RenderingTestSuite.class)
public class IntegrationTest
{
    @RenderingTestSuite.Initialized
    @SuppressWarnings("unchecked")
    public void initialize(final MockingComponentManager componentManager) throws Exception
    {
        Mockery mockery = new JUnit4Mockery();

        // Since we have a dependency on XWiki Platform Oldcore the Context Component Manager will be found and the
        // test will try to look up the Dashboard macro in the User and Wiki Component Manager and thus need a Current
        // User and a Current Wiki. It's easier for this test to simply unregister the Context Component Manager rather
        // than have to provide mocks for them.
        componentManager.unregisterComponent(ComponentManager.class, "context");

        final SkinExtension mockSsfx = componentManager.registerMockComponent(mockery, SkinExtension.class, "ssfx",
            "ssfxMock");
        final SkinExtension mockJsfx = componentManager.registerMockComponent(mockery, SkinExtension.class, "jsfx",
            "jsfxMock");
        mockery.checking(new Expectations()
        {
            {
                allowing(mockSsfx).use(with("uicomponents/container/columns.css"), with(any(Map.class)));
                allowing(mockSsfx).use(with("uicomponents/dashboard/dashboard.css"), with(any(Map.class)));
                allowing(mockJsfx).use(with("js/scriptaculous/dragdrop.js"));
                allowing(mockJsfx).use(with("js/scriptaculous/effects.js"));
                allowing(mockJsfx).use(with("js/xwiki/wysiwyg/xwe/XWikiWysiwyg.js"), with(any(Map.class)));
                allowing(mockJsfx).use(with("uicomponents/dashboard/dashboard.js"), with(any(Map.class)));
            }
        });

        final GadgetSource mockGadgetSource = componentManager.registerMockComponent(mockery, GadgetSource.class);
        mockery.checking(new Expectations()
        {
            {
                // Mock gadget for macrodashboard_nested_velocity.test
                allowing(mockGadgetSource).getGadgets(with("nested_velocity"),
                    with(any(MacroTransformationContext.class)));
                will(returnValue(Arrays.asList(new Gadget("0", Arrays.<Block> asList(new WordBlock("title")), Arrays
                    .<Block> asList(new MacroBlock("velocity", Collections.<String, String> emptyMap(),
                        "someVelocityCodeHere", true)), "1,1"))));

                // Mock gadget for macrodashboard1.test
                allowing(mockGadgetSource).getGadgets((String) with(anything()),
                    with(any(MacroTransformationContext.class)));
                will(returnValue(Arrays.asList(new Gadget("0", Arrays.<Block> asList(new WordBlock("title")), Arrays
                    .<Block> asList(new WordBlock("content")), "1,1"))));

                allowing(mockGadgetSource).getDashboardSourceMetadata((String) with(anything()),
                    with(any(MacroTransformationContext.class)));
                will(returnValue(Collections.<Block> emptyList()));

                allowing(mockGadgetSource).isEditing();
                // return true on is editing, to take as many paths possible
                will(returnValue(true));
            }
        });

        // Mock VelocityManager used in macrodashboard_nested_velocity.test because we do not have an XWikiContext
        // instance in the ExecutionContext.
        final VelocityManager mockVelocityManager =
            componentManager.registerMockComponentWithId(mockery, VelocityManager.class, "velocityManagerMock");
        mockery.checking(new Expectations()
        {
            {
                allowing(mockVelocityManager).getVelocityContext();
                will(returnValue(new VelocityContext()));
                allowing(mockVelocityManager).getVelocityEngine();
                will(doAll(new CustomAction("mockGetVelocityEngine")
                {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable
                    {
                        org.xwiki.velocity.VelocityEngine velocityEngine =
                            componentManager.getInstance(org.xwiki.velocity.VelocityEngine.class);
                        Properties properties = new Properties();
                        properties.setProperty("resource.loader", "file");
                        velocityEngine.initialize(properties);

                        return velocityEngine;
                    }

                }));
            }
        });

        componentManager.registerMockComponent(mockery, ContextualAuthorizationManager.class);
    }
}
