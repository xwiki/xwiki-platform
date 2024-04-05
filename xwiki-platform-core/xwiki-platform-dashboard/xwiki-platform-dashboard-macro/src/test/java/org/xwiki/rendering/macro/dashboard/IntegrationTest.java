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

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.test.integration.junit5.RenderingTests;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.velocity.internal.DefaultVelocityManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 * 
 * @version $Id$
 * @since 3.0RC1
 */
@AllComponents
public class IntegrationTest implements RenderingTests
{
    @RenderingTests.Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        // Since we have a dependency on XWiki Platform Oldcore the Context Component Manager will be found and the
        // test will try to look up the Dashboard macro in the User and Wiki Component Manager and thus need a Current
        // User and a Current Wiki. It's easier for this test to simply unregister the Context Component Manager rather
        // than have to provide mocks for them.
        componentManager.unregisterComponent(ComponentManager.class, "context");

        // For performance reasons we mock some components to avoid having to draw all oldcore components

        // Some components we don't really use and which trigger a lot of dependencies or fill the log with errors
        componentManager.unregisterComponent(org.xwiki.observation.EventListener.class, "refactoring.automaticRedirectCreator");
        componentManager.unregisterComponent(org.xwiki.observation.EventListener.class, "observation.remote");
        componentManager.unregisterComponent(org.xwiki.observation.EventListener.class, "refactoring.backLinksUpdater");
        componentManager.unregisterComponent(org.xwiki.observation.EventListener.class, "XClassMigratorListener");
        componentManager.unregisterComponent(org.xwiki.observation.EventListener.class, "ExtensionJobHistoryRecorder");
        componentManager.unregisterComponent(org.xwiki.observation.EventListener.class, "refactoring.legacyParentFieldUpdater");

        SkinExtension mockSsfx = componentManager.registerMockComponent(SkinExtension.class, "ssfx");
        SkinExtension mockJsfx = componentManager.registerMockComponent(SkinExtension.class, "jsfx");

        GadgetSource mockGadgetSource = componentManager.registerMockComponent(GadgetSource.class);
        // Mock gadget for macrodashboard_nested_velocity.test
        when(mockGadgetSource.getGadgets(eq("nested_velocity"), any(MacroTransformationContext.class)))
            .thenReturn(Arrays.asList(new Gadget("0", Arrays.asList(new WordBlock("title")),
                Arrays.asList(new MacroMarkerBlock("velocity", Collections.emptyMap(), "someVelocityCodeHere",
                    Collections.singletonList(new WordBlock("someVelocityOutputHere")), true)),
                "1,1")));

        // Mock gadget for macrodashboard1.test
        when(mockGadgetSource.getGadgets(isNull(String.class), any(MacroTransformationContext.class)))
            .thenReturn(Arrays.asList(new Gadget("0", Arrays.<Block>asList(new WordBlock("title")),
                Arrays.<Block>asList(new WordBlock("content")), "1,1")));

        when(mockGadgetSource.getDashboardSourceMetadata(any(), any(MacroTransformationContext.class)))
            .thenReturn(Collections.<Block>emptyList());

        // return true on is editing, to take as many paths possible
        when(mockGadgetSource.isEditing()).thenReturn(true);

        // Use the much lighter xwiki-commons velocity manager
        componentManager.registerComponent(DefaultVelocityManager.class);

        ContextualAuthorizationManager authorization = componentManager.registerMockComponent(ContextualAuthorizationManager.class);
        when(authorization.hasAccess(Right.SCRIPT)).thenReturn(true);
    }
}
