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
import java.util.List;
import java.util.Map;

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
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroPreparationException;
import org.xwiki.rendering.macro.context.ContextMacroParameters;
import org.xwiki.rendering.macro.context.TransformationContextMode;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContextMacro}.
 *
 * @version $Id$
 * @since 8.3RC1
 */
@ComponentTest
@ComponentList({TestEnvironment.class, ContextMacroDocument.class})
class ContextMacroTest
{
    private static final DocumentReference AUTHOR = new DocumentReference("wiki", "XWiki", "author");

    private static final DocumentReference TARGET_REFERENCE = new DocumentReference("wiki", "space", "target");

    private static final DocumentReference SOURCE_REFERENCE = new DocumentReference("wiki", "space", "source");

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
    private DocumentReferenceResolver<String> resolver;

    @InjectMockComponents
    private ContextMacro macro;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private BlockAsyncRendererExecutor executor;

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

        this.executor = this.componentManager.getInstance(BlockAsyncRendererExecutor.class);
    }

    @Test
    void executeWhenNoDocumentSpecified() throws Exception
    {
        MacroBlock macroBlock = new MacroBlock("context", Collections.<String, String>emptyMap(), false);
        MetaData metadata = new MetaData();
        metadata.addMetaData(MetaData.SOURCE, "source");
        XDOM pageXDOM = new XDOM(Arrays.asList(macroBlock), metadata);
        MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setSyntax(Syntax.XWIKI_2_0);
        macroContext.setCurrentMacroBlock(macroBlock);
        macroContext.setXDOM(pageXDOM);

        DocumentModelBridge dmb = mock(DocumentModelBridge.class);
        when(this.dab.getTranslatedDocumentInstance(TARGET_REFERENCE)).thenReturn(dmb);

        XDOM contentXDOM = new XDOM(Arrays.asList(new WordBlock("test")));
        when(this.parser.parse(eq(""), same(null), same(macroContext), eq(false), eq(null), eq(false)))
            .thenReturn(contentXDOM);

        ContextMacroParameters parameters = new ContextMacroParameters();

        when(this.executor.execute(any())).thenReturn(new WordBlock("result"));

        List<Block> result = this.macro.execute(parameters, "", macroContext);

        verifyNoInteractions(this.executor);

        assertEquals(Arrays.asList(new MetaDataBlock(contentXDOM.getChildren())), result);
    }

    @Test
    void executeWithReferencedDocumentNotViewableByTheAuthor() throws Exception
    {
        doThrow(AccessDeniedException.class).when(this.authorization).checkAccess(Right.VIEW, AUTHOR, TARGET_REFERENCE);

        try {
            executeInDOCUMENTContext();

            fail("Should have thrown an exception");
        } catch (MacroExecutionException expected) {
            assertEquals("Author [wiki:XWiki.author] is not allowed to access target document [wiki:space.target]",
                expected.getMessage());
        }
    }

    @Test
    void executeInCURRENTContext() throws Exception
    {
        MacroBlock macroBlock = new MacroBlock("context", Collections.<String, String>emptyMap(), false);
        MetaData metadata = new MetaData();
        metadata.addMetaData(MetaData.SOURCE, "source");
        XDOM pageXDOM = new XDOM(Arrays.asList(macroBlock), metadata);
        MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setSyntax(Syntax.XWIKI_2_0);
        macroContext.setCurrentMacroBlock(macroBlock);
        macroContext.setXDOM(pageXDOM);

        DocumentModelBridge dmb = mock(DocumentModelBridge.class);
        when(this.dab.getTranslatedDocumentInstance(TARGET_REFERENCE)).thenReturn(dmb);

        MetaData parserMetadata = new MetaData();
        parserMetadata.addMetaData(MetaData.SOURCE, "target");
        parserMetadata.addMetaData(MetaData.BASE, "target");

        XDOM contentXDOM = new XDOM(Arrays.asList(new WordBlock("test")), parserMetadata);
        when(this.parser.parse(eq(""), same(null), same(macroContext), eq(false), eq(parserMetadata), eq(false)))
            .thenReturn(contentXDOM);

        ContextMacroParameters parameters = new ContextMacroParameters();
        parameters.setDocument("target");
        parameters.setTransformationContext(TransformationContextMode.CURRENT);

        when(this.executor.execute(any())).thenReturn(new WordBlock("result"));

        List<Block> result = this.macro.execute(parameters, "", macroContext);

        verifyNoInteractions(this.executor);

        assertEquals(Arrays.asList(new MetaDataBlock(contentXDOM.getChildren(), parserMetadata)), result);
    }

    @Test
    void executeInDOCUMENTContext() throws Exception
    {
        execute(false, TransformationContextMode.DOCUMENT, false);
    }

    @Test
    void executeInDOCUMENTContextInRestrictedMode() throws Exception
    {
        execute(true, TransformationContextMode.DOCUMENT, false);
    }

    @Test
    void executeWithRestricted() throws Exception
    {
        execute(false, null, true);
    }

    private void execute(boolean restrictedContext, TransformationContextMode mode, boolean restricted) throws Exception
    {
        MacroBlock macroBlock = new MacroBlock("context", Collections.<String, String>emptyMap(), false);
        MetaData metadata = new MetaData();
        metadata.addMetaData(MetaData.SOURCE, "source");
        XDOM pageXDOM = new XDOM(Arrays.asList(macroBlock), metadata);
        MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setSyntax(Syntax.XWIKI_2_0);
        macroContext.setCurrentMacroBlock(macroBlock);
        macroContext.setXDOM(pageXDOM);
        macroContext.getTransformationContext().setRestricted(restrictedContext);

        XDOM targetXDOM;
        MetaData parserMetadata;
        ContextMacroParameters parameters = new ContextMacroParameters();
        if (mode != null) {
            DocumentModelBridge dmb = mock(DocumentModelBridge.class);
            when(this.dab.getTranslatedDocumentInstance(TARGET_REFERENCE)).thenReturn(dmb);
            targetXDOM = new XDOM(Arrays.asList(new WordBlock("word")), metadata);
            when(dmb.getPreparedXDOM()).thenReturn(targetXDOM);

            parameters.setDocument("target");
            parameters.setTransformationContext(mode);

            parserMetadata = new MetaData();
            parserMetadata.addMetaData(MetaData.SOURCE, "target");
            parserMetadata.addMetaData(MetaData.BASE, "target");
        } else {
            targetXDOM = null;
            parserMetadata = null;
        }
        parameters.setRestricted(restricted);

        XDOM contentXDOM = new XDOM(Arrays.asList(new WordBlock("test")), parserMetadata);
        when(this.parser.parse(eq(""), same(null), same(macroContext), eq(false), eq(parserMetadata), eq(false)))
            .thenReturn(contentXDOM);


        when(this.executor.execute(any())).thenReturn(new WordBlock("result"));

        this.macro.execute(parameters, "", macroContext);

        ArgumentCaptor<BlockAsyncRendererConfiguration> configurationCaptor =
            ArgumentCaptor.forClass(BlockAsyncRendererConfiguration.class);
        verify(this.executor).execute(configurationCaptor.capture());

        BlockAsyncRendererConfiguration configuration = configurationCaptor.getValue();
        assertEquals(AUTHOR, configuration.getSecureAuthorReference());
        assertEquals(SOURCE_REFERENCE, configuration.getSecureDocumentReference());
        if (targetXDOM != null) {
            assertSame(targetXDOM, configuration.getXDOM());
        }
        assertEquals(restrictedContext ? true : restricted, configuration.isResricted());
    }

    @Test
    void prepare() throws MacroPreparationException
    {
        MacroBlock macroBlock = new MacroBlock("cotext", Map.of(), false);

        this.macro.prepare(macroBlock);

        verify(this.parser).prepareContentWiki(macroBlock);
    }
}
