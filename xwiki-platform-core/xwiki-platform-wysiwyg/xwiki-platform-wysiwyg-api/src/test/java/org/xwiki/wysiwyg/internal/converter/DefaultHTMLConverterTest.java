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
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wysiwyg.cleaner.HTMLCleaner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultHTMLConverter}.
 * 
 * @version $Id$
 */
@ComponentTest
public class DefaultHTMLConverterTest
{
    @InjectMockComponents
    private DefaultHTMLConverter converter;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @AfterComponent
    public void overrideComponent() throws Exception
    {
        this.componentManager.registerComponent(RenderingContext.class, mock(MutableRenderingContext.class));
    }

    @BeforeComponent
    public void configure() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);
    }

    /**
     * Unit test for {@link DefaultHTMLConverter#fromHTML(String, String)}.
     */
    @Test
    public void fromHTML() throws Exception
    {
        String html = "some HTML";
        String syntaxId = "syntax/x.y";

        // Verify the HTML is cleaned.
        HTMLCleaner cleaner = this.componentManager.getInstance(HTMLCleaner.class);
        when(cleaner.clean(html)).thenReturn(html);

        PrintRendererFactory printRendererFactory =
            this.componentManager.registerMockComponent(PrintRendererFactory.class, syntaxId);

        PrintRenderer printRenderer = mock(PrintRenderer.class);
        when(printRendererFactory.createRenderer(any(WikiPrinter.class))).thenReturn(printRenderer);

        assertEquals("", this.converter.fromHTML(html, syntaxId));

        // Verify the HTML is converted to the specified syntax.
        StreamParser xhtmlStreamParser = this.componentManager.getInstance(StreamParser.class, "xhtml/5");
        verify(xhtmlStreamParser).parse(any(StringReader.class), same(printRenderer));
    }

    /**
     * Unit test for {@link DefaultHTMLConverter#toHTML(String, String)}.
     */
    @Test
    public void toHTML() throws Exception
    {
        String source = "wiki syntax";
        Syntax syntax = new Syntax(new SyntaxType("syntax", "Syntax"), "x.y");

        // The source should be parsed.
        ContentParser contentParser = this.componentManager.getInstance(ContentParser.class);
        XDOM xdom = new XDOM(Collections.emptyList());
        when(contentParser.parse(source, syntax, null)).thenReturn(xdom);

        assertEquals("", this.converter.toHTML(source, syntax.toIdString()));

        // Verify that the macro transformations have been executed.
        Transformation macroTransformation = this.componentManager.getInstance(Transformation.class, "macro");
        RenderingContext renderingContext = this.componentManager.getInstance(RenderingContext.class);

        // It's very important to verify that a transformation context id is set as otherwise if the content being
        // edited has different velocity macros executing, they'll be executed in isolation and thus what's defined in
        // one won't be visible from the other ones (For example see https://jira.xwiki.org/browse/XWIKI-11695).
        ArgumentCaptor<TransformationContext> txContextArgument = ArgumentCaptor.forClass(TransformationContext.class);
        verify((MutableRenderingContext) renderingContext).transformInContext(same(macroTransformation),
            txContextArgument.capture(), same(xdom));
        assertEquals("wysiwygtxid", txContextArgument.getValue().getId());

        // Verify the XDOM is rendered to Annotated XHTML.
        BlockRenderer xhtmlRenderer = this.componentManager.getInstance(BlockRenderer.class, "annotatedhtml/5.0");
        verify(xhtmlRenderer).render(same(xdom), any(WikiPrinter.class));
    }

    /**
     * Unit test for {@link DefaultHTMLConverter#parseAndRender(String, String)}.
     */
    @Test
    public void parseAndRender() throws Exception
    {
        String html = "some HTML";
        String syntaxId = "syntax/x.y";

        // Verify the HTML is cleaned.
        HTMLCleaner cleaner = this.componentManager.getInstance(HTMLCleaner.class);
        when(cleaner.clean(html)).thenReturn(html);

        // Verify the HTML is parsed into XDOM.
        XDOM xdom = new XDOM(Collections.emptyList());
        Parser xhtmlParser = this.componentManager.getInstance(Parser.class, "xhtml/5");
        when(xhtmlParser.parse(any(StringReader.class))).thenReturn(xdom);

        assertEquals("", this.converter.parseAndRender(html, syntaxId));

        // Verify that the macro transformations have been executed.
        Transformation macroTransformation = this.componentManager.getInstance(Transformation.class, "macro");
        RenderingContext renderingContext = this.componentManager.getInstance(RenderingContext.class);

        // It's very important to verify that a transformation context id is set as otherwise if the content being
        // edited has different velocity macros executing, they'll be executed in isolation and thus what's defined in
        // one won't be visible from the other ones (For example see https://jira.xwiki.org/browse/XWIKI-11695).
        ArgumentCaptor<TransformationContext> txContextArgument = ArgumentCaptor.forClass(TransformationContext.class);
        verify((MutableRenderingContext) renderingContext).transformInContext(same(macroTransformation),
            txContextArgument.capture(), same(xdom));
        assertEquals("wysiwygtxid", txContextArgument.getValue().getId());

        // Verify the XDOM is rendered to Annotated XHTML.
        BlockRenderer xhtmlRenderer = this.componentManager.getInstance(BlockRenderer.class, "annotatedhtml/5.0");
        verify(xhtmlRenderer).render(same(xdom), any(WikiPrinter.class));

        // Verify that the syntax meta data has been set.
        Syntax syntax = Syntax.valueOf(syntaxId);
        assertEquals(syntax, xdom.getMetaData().getMetaData(MetaData.SYNTAX));

        // Verify that the syntax has been set on the rendering context.
        verify(((MutableRenderingContext) renderingContext)).push(null, null, syntax, null, false, syntax);
    }
}
