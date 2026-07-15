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
package org.xwiki.rendering.macro.velocity;

import java.util.Collections;
import java.util.HashMap;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.Mockito.when;
import static org.xwiki.rendering.test.integration.junit5.BlockAssert.assertBlocks;

/**
 * Verify that a Velocity macro defined in one page is not visible from another page.
 *
 * @version $Id$
 * @since 2.4M2
 */
@ComponentTest
@AllComponents(excludes = { org.xwiki.environment.internal.StandardEnvironment.class })
class VelocityMacroIsolationTest
{
    @MockComponent
    private Environment environment;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @MockComponent
    private WikiModel wikiModel;

    @MockComponent
    @Named("current")
    private AttachmentReferenceResolver<String> attachmentReferenceResolver;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    @Named("current")
    private SpaceReferenceResolver<String> spaceReferenceResolver;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private Macro<VelocityMacroParameters> velocityMacro;

    @BeforeEach
    void setUp() throws Exception
    {
        MemoryConfigurationSource configurationSource = new MemoryConfigurationSource();
        this.componentManager.registerComponent(ConfigurationSource.class, configurationSource);
        this.componentManager.registerComponent(ConfigurationSource.class, "xwikicfg", configurationSource);

        when(this.authorizationManager.hasAccess(Right.SCRIPT)).thenReturn(true);

        this.velocityMacro = this.componentManager.getInstance(Macro.class, "velocity");

        // Put a fake execution context to avoid NPE in ScriptClassLoaderHandlerListener.
        Execution execution = this.componentManager.getInstance(Execution.class);
        ExecutionContextManager ecm = this.componentManager.getInstance(ExecutionContextManager.class);
        ecm.initialize(new ExecutionContext());
        execution.getContext().setProperty("xwikicontext", new HashMap<>());
    }

    @Test
    void velocityMacroIsolation() throws Exception
    {
        String expected = """
            beginDocument
            beginParagraph
            onSpecialSymbol [#]
            onWord [testMacrosAreLocal]
            onSpecialSymbol [(]
            onSpecialSymbol [)]
            endParagraph
            endDocument""";

        VelocityMacroParameters params = new VelocityMacroParameters();
        MacroTransformationContext context = new MacroTransformationContext();
        context.setSyntax(Syntax.XWIKI_2_0);
        context.setCurrentMacroBlock(new MacroBlock("velocity", Collections.emptyMap(), false));

        // Execute the velocity macro in the context of a first page
        context.setId("page1");
        this.velocityMacro.execute(params, "#macro(testMacrosAreLocal)mymacro#end", context);

        // And then in the context of a second independent page
        context.setId("page2");
        PrintRendererFactory eventRendererFactory =
            this.componentManager.getInstance(PrintRendererFactory.class, "event/1.0");
        assertBlocks(expected,
            this.velocityMacro.execute(params, "#testMacrosAreLocal()", context), eventRendererFactory);
    }
}
