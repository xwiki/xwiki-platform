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
package org.xwiki.rendering.macro.code;

import org.xwiki.rendering.macro.source.MacroContentSourceReference;
import org.xwiki.rendering.macro.source.MacroContentWikiSource;
import org.xwiki.rendering.macro.source.MacroContentWikiSourceFactory;
import org.xwiki.rendering.test.integration.Initialized;
import org.xwiki.rendering.test.integration.junit5.RenderingTest;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 3.0RC1
 */
@AllComponents
// @Scope(pattern = "macrocode1.test")
public class IntegrationTests extends RenderingTest
{
    @Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        MacroContentWikiSourceFactory factory =
            componentManager.registerMockComponent(MacroContentWikiSourceFactory.class, "script");
        MacroContentSourceReference reference = new MacroContentSourceReference("script", "myvar");
        when(factory.getContent(eq(reference), any()))
            .thenReturn(new MacroContentWikiSource(reference, "<i>script</i>", null));
    }
}
