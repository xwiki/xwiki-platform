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
package org.xwiki.uiextension.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.plain.Plain10SyntaxProvider;
import org.xwiki.rendering.internal.renderer.plain.PlainTextBlockRenderer;
import org.xwiki.rendering.internal.renderer.plain.PlainTextRenderer;
import org.xwiki.rendering.internal.renderer.plain.PlainTextRendererFactory;
import org.xwiki.rendering.internal.transformation.DefaultRenderingContext;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.reference.link.LinkLabelGenerator;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.uiextension.UIExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UIExtensionRenderer}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({
    DefaultRenderingContext.class,
    // Needed to render in the plain/1.0 syntax.
    PlainTextBlockRenderer.class,
    PlainTextRendererFactory.class,
    PlainTextRenderer.class,
    Plain10SyntaxProvider.class
})
class UIExtensionRendererTest
{
    @InjectMockComponents
    private UIExtensionRenderer renderer;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @MockComponent
    private Execution execution;

    @MockComponent
    private LinkLabelGenerator linkLabelGenerator;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private MutableRenderingContext renderingContext;

    @BeforeEach
    void beforeEach() throws Exception
    {
        when(this.execution.getContext()).thenReturn(new ExecutionContext());
        when(this.contextComponentManagerProvider.get()).thenReturn(this.componentManager);
        when(this.linkLabelGenerator.generate(any(ResourceReference.class))).thenReturn("label");

        this.renderingContext = this.componentManager.getInstance(RenderingContext.class);
    }

    @Test
    void renderExecutesTheUIExtensionWithTheOutputSyntaxAsTargetSyntax() throws Exception
    {
        // Simulate a page being rendered in HTML, e.g. a normal page view.
        pushTargetSyntax(Syntax.HTML_5_0);

        UIExtension extension = mockTemplateUIExtension("uix", "Hello");

        // The UI extension is executed with plain/1.0 as target syntax, so the raw block it produces is tagged
        // plain/1.0 and is thus kept by the plain/1.0 renderer.
        assertEquals("Hello", this.renderer.render(List.of(extension), Syntax.PLAIN_1_0, false));
    }

    @Test
    void renderWhenTheUIExtensionIgnoresTheTargetSyntax() throws Exception
    {
        pushTargetSyntax(Syntax.HTML_5_0);

        // A UI extension that produces HTML no matter what the target syntax is. This is the behavior we can't fix
        // from the outside: the plain/1.0 renderer drops a raw block that is not tagged plain/1.0.
        UIExtension extension = mock(UIExtension.class);
        when(extension.execute(anyBoolean())).thenReturn(new RawBlock("Hello", Syntax.HTML_5_0));

        assertEquals("", this.renderer.render(List.of(extension), Syntax.PLAIN_1_0, false));
    }

    @Test
    void renderPassesTheOutputSyntaxWhenTheContextTargetSyntaxDiffers() throws Exception
    {
        // Simulate a page loaded with ?outputSyntax=plain, e.g. the PDF export sheet.
        pushTargetSyntax(Syntax.PLAIN_1_0);

        BlockRenderer htmlRenderer = this.componentManager.registerMockComponent(BlockRenderer.class, "html/5.0");
        doAnswer(invocation -> {
            Collection<Block> blocks = invocation.getArgument(0);
            WikiPrinter printer = invocation.getArgument(1);
            blocks.forEach(block -> printer.print(((RawBlock) block).getSyntax().toIdString()));
            return null;
        }).when(htmlRenderer).render(anyCollection(), any(WikiPrinter.class));

        UIExtension extension = mockTemplateUIExtension("uix", "Hello");

        // The raw block is tagged with the forced output syntax and not with the plain/1.0 syntax of the context.
        assertEquals("html/5.0", this.renderer.render(List.of(extension), Syntax.HTML_5_0, false));
    }

    @Test
    void renderRestoresThePreviousTargetSyntax() throws Exception
    {
        pushTargetSyntax(Syntax.HTML_5_0);

        this.renderer.render(List.of(mockTemplateUIExtension("uix", "Hello")), Syntax.PLAIN_1_0, false);

        assertEquals(Syntax.HTML_5_0, this.renderingContext.getTargetSyntax());
    }

    @Test
    void renderRestoresThePreviousTargetSyntaxWhenRenderingFails()
    {
        pushTargetSyntax(Syntax.HTML_5_0);

        UIExtension extension = mock(UIExtension.class);
        when(extension.execute(anyBoolean())).thenThrow(new RuntimeException("Boom!"));

        assertThrows(RenderingException.class,
            () -> this.renderer.render(List.of(extension), Syntax.ANNOTATED_HTML_5_0, false));

        assertEquals(Syntax.HTML_5_0, this.renderingContext.getTargetSyntax());
    }

    @Test
    void renderPreservesTheOtherRenderingContextProperties() throws Exception
    {
        XDOM xdom = new XDOM(List.of(new WordBlock("test")));
        this.renderingContext.push(null, xdom, Syntax.XWIKI_2_1, "transformationId", true, Syntax.HTML_5_0);

        UIExtension extension = mock(UIExtension.class);
        when(extension.execute(anyBoolean())).thenAnswer(invocation -> {
            // Only the target syntax must be overwritten.
            assertEquals(Syntax.PLAIN_1_0, this.renderingContext.getTargetSyntax());
            assertSame(xdom, this.renderingContext.getXDOM());
            assertEquals(Syntax.XWIKI_2_1, this.renderingContext.getDefaultSyntax());
            assertEquals("transformationId", this.renderingContext.getTransformationId());
            assertTrue(this.renderingContext.isRestricted());
            return new RawBlock("Hello", Syntax.PLAIN_1_0);
        });

        assertEquals("Hello", this.renderer.render(List.of(extension), Syntax.PLAIN_1_0, false));
    }

    @Test
    void renderWhenNoRendererForTheOutputSyntax()
    {
        RenderingException exception = assertThrows(RenderingException.class,
            () -> this.renderer.render(List.of(), Syntax.ANNOTATED_HTML_5_0, false));

        assertEquals("Failed to find a renderer for syntax [annotatedhtml/5.0]", exception.getMessage());
    }

    @Test
    void renderSkipsTheUIExtensionsThatFailToExecute() throws Exception
    {
        pushTargetSyntax(Syntax.PLAIN_1_0);

        UIExtension failing = mock(UIExtension.class, "failing");
        when(failing.getId()).thenReturn("failingId");
        when(failing.execute(anyBoolean())).thenThrow(new RuntimeException("Boom!"));

        assertEquals("Hello",
            this.renderer.render(Arrays.asList(failing, mockTemplateUIExtension("uix", "Hello")), Syntax.PLAIN_1_0,
                false));

        assertEquals("Failed to execute the UI extension [failingId]. Root cause is [RuntimeException: Boom!]",
            this.logCapture.getMessage(0));
    }

    @Test
    void renderIgnoresNullUIExtensions() throws Exception
    {
        pushTargetSyntax(Syntax.PLAIN_1_0);

        assertEquals("Hello",
            this.renderer.render(Arrays.asList(null, mockTemplateUIExtension("uix", "Hello")), Syntax.PLAIN_1_0,
                false));

        assertEquals("Ignoring a null UI extension.", this.logCapture.getMessage(0));
    }

    @Test
    void renderWhenNoUIExtension() throws Exception
    {
        assertEquals("", this.renderer.render(List.of(), Syntax.PLAIN_1_0, false));
    }

    @Test
    void renderWhenInline() throws Exception
    {
        pushTargetSyntax(Syntax.PLAIN_1_0);

        UIExtension extension = mockTemplateUIExtension("uix", "Hello");

        assertEquals("Hello", this.renderer.render(List.of(extension), Syntax.PLAIN_1_0, true));

        verify(extension).execute(true);
    }

    @Test
    void renderWhenNoTargetSyntaxInTheRenderingContext() throws Exception
    {
        // Nothing was pushed in the rendering context, so it has no target syntax to start with.
        assertNull(this.renderingContext.getTargetSyntax());

        assertEquals("Hello",
            this.renderer.render(List.of(mockTemplateUIExtension("uix", "Hello")), Syntax.PLAIN_1_0, false));
    }

    private void pushTargetSyntax(Syntax targetSyntax)
    {
        this.renderingContext.push(null, null, Syntax.XWIKI_2_1, "test", false, targetSyntax);
    }

    /**
     * @return a UI extension that behaves like a template based one: it tags the raw block it produces with the target
     *     syntax found in the rendering context. See {@code InternalTemplateManager}.
     */
    private UIExtension mockTemplateUIExtension(String id, String content)
    {
        UIExtension extension = mock(UIExtension.class, id);
        when(extension.getId()).thenReturn(id);
        when(extension.execute(anyBoolean()))
            .thenAnswer(invocation -> new RawBlock(content, this.renderingContext.getTargetSyntax()));
        return extension;
    }
}
