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
package org.xwiki.uiextension.internal.macro;

import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.test.integration.junit5.RenderingTests;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionManager;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 */
@AllComponents
@ComponentList(TestEnvironment.class)
public class IntegrationTests implements RenderingTests
{
    @RenderingTests.Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        // Replace the environment by a test compatible one
        componentManager.registerComponent(TestEnvironment.class);

        // For performance reasons we mock some components to avoid having to draw all oldcore components

        // Some components we don't really use and which trigger a lot of dependencies
        componentManager.registerMockComponent(AuthorizationManager.class);
        componentManager.registerMockComponent(ContextualAuthorizationManager.class);

        // Inject ui extensions
        UIExtension testextension1 = componentManager.registerMockComponent(UIExtension.class, "testextension1hint");
        when(testextension1.getId()).thenReturn("testextension1");
        when(testextension1.getExtensionPointId()).thenReturn("extensionpoint");
        when(testextension1.getParameters()).thenReturn(Map.of());
        when(testextension1.execute(true)).thenReturn(new WordBlock("extension1"));
        when(testextension1.execute(false)).thenReturn(new ParagraphBlock(List.of(new WordBlock("extension1"))));
        when(testextension1.execute()).thenReturn(new ParagraphBlock(List.of(new WordBlock("extension1"))));
        UIExtension testextension2 = componentManager.registerMockComponent(UIExtension.class, "testextension2hint");
        when(testextension2.getId()).thenReturn("testextension2");
        when(testextension2.getExtensionPointId()).thenReturn("extensionpoint");
        when(testextension2.getParameters()).thenReturn(Map.of());
        when(testextension2.execute(true)).thenReturn(new WordBlock("extension2"));
        when(testextension2.execute(false)).thenReturn(new ParagraphBlock(List.of(new WordBlock("extension2"))));
        when(testextension2.execute()).thenReturn(new ParagraphBlock(List.of(new WordBlock("extension2"))));

        // Provider a custom UIExtensionManager to have a stable list
        UIExtensionManager manager = componentManager.registerMockComponent(UIExtensionManager.class, "extensionpoint");
        when(manager.get("extensionpoint")).thenReturn(List.of(testextension1, testextension2));
    }
}
