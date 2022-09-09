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
package org.xwiki.rendering.internal.macro.context;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.properties.BeanDescriptor;
import org.xwiki.properties.BeanManager;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.context.ContextMacroParameters;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContextMacro}.
 *
 * @version $Id$
 * @since 8.3RC1
 */
@ComponentTest
@ComponentList(TestEnvironment.class)
class ContextMacroTest
{
    private static final DocumentReference AUTHOR = new DocumentReference("wiki", "XWiki", "author");

    private static final DocumentReference TARGET_REFERENCE = new DocumentReference("wiki", "space", "target");

    private static final DocumentReference SOURCE_REFERENCE = new DocumentReference("wiki", "space", "source");

    @Inject
    protected BlockAsyncRendererExecutor executor;

    @MockComponent
    private DocumentAccessBridge dab;

    @MockComponent
    private BeanManager beanManager;

    @MockComponent
    private MacroContentParser parser;

    @MockComponent
    private AuthorizationManager authorization;

    @MockComponent
    @Named("macro")
    private DocumentReferenceResolver<String> macroReferenceResolver;

    @MockComponent
    protected DocumentReferenceResolver<String> resolver;

    @InjectMockComponents
    private ContextMacro macro;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        // Macro Descriptor set up
        BeanDescriptor descriptor = mock(BeanDescriptor.class);
        when(descriptor.getProperties()).thenReturn(Collections.emptyList());
        when(this.beanManager.getBeanDescriptor(any())).thenReturn(descriptor);

        when(this.dab.getCurrentAuthorReference()).thenReturn(AUTHOR);

        when(this.macroReferenceResolver.resolve(eq("target"), any())).thenReturn(TARGET_REFERENCE);
        when(this.resolver.resolve("source")).thenReturn(SOURCE_REFERENCE);
    }

    @Test
    void executeWhenNoDocumentSpecified() throws Exception
    {
        ContextMacroParameters parameters = new ContextMacroParameters();

        try {
            this.macro.execute(parameters, "", new MacroTransformationContext());
            fail("Should have thrown an exception");
        } catch (MacroExecutionException expected) {
            assertEquals("You must specify a 'document' parameter pointing to the document to set in the "
                + "context as the current document.", expected.getMessage());
        }
    }

    @Test
    void executeWithReferencedDocumentNotViewableByTheAuthor() throws Exception
    {
        MacroTransformationContext macroContext = new MacroTransformationContext();
        MacroBlock macroBlock = new MacroBlock("context", Collections.emptyMap(), false);
        macroContext.setCurrentMacroBlock(macroBlock);

        doThrow(AccessDeniedException.class).when(this.authorization).checkAccess(Right.VIEW, AUTHOR, TARGET_REFERENCE);

        ContextMacroParameters parameters = new ContextMacroParameters();
        parameters.setDocument("target");

        try {
            this.macro.execute(parameters, "", macroContext);
            fail("Should have thrown an exception");
        } catch (MacroExecutionException expected) {
            assertEquals("Author [wiki:XWiki.author] is not allowed to access target document [wiki:space.target]",
                expected.getMessage());
        }
    }

    @Test
    void executeOk() throws Exception
    {
        MacroBlock macroBlock = new MacroBlock("context", Collections.<String, String>emptyMap(), false);
        MetaData metadata = new MetaData();
        metadata.addMetaData(MetaData.SOURCE, "source");
        XDOM xdom = new XDOM(Arrays.asList(macroBlock), metadata);
        MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setSyntax(Syntax.XWIKI_2_0);
        macroContext.setCurrentMacroBlock(macroBlock);

        DocumentModelBridge dmb = mock(DocumentModelBridge.class);
        when(this.dab.getTranslatedDocumentInstance(TARGET_REFERENCE)).thenReturn(dmb);

        when(this.parser.parse(eq(""), same(macroContext), eq(false), any(MetaData.class), eq(false))).thenReturn(xdom);

        ContextMacroParameters parameters = new ContextMacroParameters();
        parameters.setDocument("target");

        when(this.executor.execute(any())).thenReturn(new WordBlock("result"));

        this.macro.execute(parameters, "", macroContext);

        ArgumentCaptor<BlockAsyncRendererConfiguration> configurationCaptor =
            ArgumentCaptor.forClass(BlockAsyncRendererConfiguration.class);
        verify(this.executor).execute(configurationCaptor.capture());

        BlockAsyncRendererConfiguration configuration = configurationCaptor.getValue();
        assertEquals(AUTHOR, configuration.getSecureAuthorReference());
        assertEquals(SOURCE_REFERENCE, configuration.getSecureDocumentReference());
    }
}
