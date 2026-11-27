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
package org.xwiki.wysiwyg.internal.converter;

import java.io.StringReader;
import java.util.List;

import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EditedContentRefresher}.
 *
 * @version $Id$
 */
@ComponentTest
class EditedContentRefresherTest
{
    private static final String EDITOR_SYNTAX_ID = "editor/1.0";

    private static final Syntax EDITOR_SYNTAX = new Syntax(new SyntaxType("editor", "Editor"), "1.0");

    private static final DocumentReference SOURCE_REFERENCE = new DocumentReference("wiki", "Space", "Page");

    @InjectMockComponents
    private EditedContentRefresher refresher;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent(classToMock = MutableRenderingContext.class)
    private RenderingContext renderingContext;

    @MockComponent
    @Named("macro")
    private Transformation macroTransformation;

    @MockComponent
    @Named(EDITOR_SYNTAX_ID)
    private Parser parser;

    @MockComponent
    @Named(EDITOR_SYNTAX_ID)
    private BlockRenderer renderer;

    @BeforeComponent
    void configure() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);
    }

    @Test
    void refresh() throws Exception
    {
        String content = "edited content";
        XDOM xdom = new XDOM(List.of());
        when(this.parser.parse(any(StringReader.class))).thenReturn(xdom);
        doAnswer(invocation -> {
            invocation.<WikiPrinter>getArgument(1).print("refreshed content");
            return null;
        }).when(this.renderer).render(same(xdom), any(WikiPrinter.class));
        when(this.serializer.serialize(SOURCE_REFERENCE)).thenReturn("wiki:Space.Page");

        assertEquals("refreshed content",
            this.refresher.refresh(content, EDITOR_SYNTAX, Syntax.XWIKI_2_1, SOURCE_REFERENCE, true));

        // The content edited in-place is serialized, while parsing, in the syntax used to save the content.
        MutableRenderingContext mutableRenderingContext = (MutableRenderingContext) this.renderingContext;
        InOrder parsing = inOrder(mutableRenderingContext, this.parser);
        parsing.verify(mutableRenderingContext).push(null, null, Syntax.XWIKI_2_1, null, false, Syntax.XWIKI_2_1);
        parsing.verify(this.parser).parse(any(StringReader.class));
        parsing.verify(mutableRenderingContext).pop();

        // The macro content is parsed by default with the syntax used to save the content.
        assertEquals(Syntax.XWIKI_2_1, xdom.getMetaData().getMetaData(MetaData.SYNTAX));
        assertEquals("wiki:Space.Page", xdom.getMetaData().getMetaData(MetaData.SOURCE));

        // All the macros are executed in the same transformation context, for the editor syntax.
        ArgumentCaptor<TransformationContext> transformationContext =
            ArgumentCaptor.forClass(TransformationContext.class);
        verify(mutableRenderingContext).transformInContext(same(this.macroTransformation),
            transformationContext.capture(), same(xdom));
        assertEquals("wysiwygRefresh", transformationContext.getValue().getId());
        assertEquals(Syntax.XWIKI_2_1, transformationContext.getValue().getSyntax());
        assertEquals(EDITOR_SYNTAX, transformationContext.getValue().getTargetSyntax());
        assertTrue(transformationContext.getValue().isRestricted());
    }
}
