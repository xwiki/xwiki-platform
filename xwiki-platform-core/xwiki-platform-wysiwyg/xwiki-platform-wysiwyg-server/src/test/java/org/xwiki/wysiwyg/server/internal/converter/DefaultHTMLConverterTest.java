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
package org.xwiki.wysiwyg.server.internal.converter;

import java.io.StringReader;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.gwt.wysiwyg.client.cleaner.HTMLCleaner;
import org.xwiki.gwt.wysiwyg.client.converter.HTMLConverter;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultHTMLConverter}.
 * 
 * @version $Id$
 */
public class DefaultHTMLConverterTest
{
    /**
     * A component manager that automatically mocks all dependencies of {@link DefaultHTMLConverter}.
     */
    @Rule
    public MockitoComponentMockingRule<HTMLConverter> mocker = new MockitoComponentMockingRule<HTMLConverter>(
        DefaultHTMLConverter.class);

    @AfterComponent
    public void overideComponent() throws Exception
    {
        mocker.registerComponent(RenderingContext.class, mock(MutableRenderingContext.class));
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
        HTMLCleaner cleaner = mocker.getInstance(HTMLCleaner.class);
        when(cleaner.clean(html)).thenReturn(html);

        PrintRendererFactory printRendererFactory = this.mocker.registerMockComponent(PrintRendererFactory.class, syntaxId);

        PrintRenderer printRenderer = mock(PrintRenderer.class);
        when(printRendererFactory.createRenderer(any(WikiPrinter.class))).thenReturn(printRenderer);

        Assert.assertEquals("", mocker.getComponentUnderTest().fromHTML(html, syntaxId));

        // Verify the HTML is converted to the specified syntax.
        StreamParser xhtmlStreamParser = mocker.getInstance(StreamParser.class, "xhtml/1.0");
        verify(xhtmlStreamParser).parse(any(StringReader.class), same(printRenderer));
    }

    /**
     * Unit test for {@link DefaultHTMLConverter#toHTML(String, String)}.
     */
    @Test
    public void toHTML() throws Exception
    {
        String source = "wiki syntax";
        String syntaxId = "syntax/x.y";

        // The source should be parsed.
        Parser parser = this.mocker.registerMockComponent(Parser.class, syntaxId);

        XDOM xdom = new XDOM(Collections.<Block> emptyList());
        when(parser.parse(any(StringReader.class))).thenReturn(xdom);

        Assert.assertEquals("", mocker.getComponentUnderTest().toHTML(source, syntaxId));

        // Verify that the macro transformations have been executed.
        Transformation macroTransformation = mocker.getInstance(Transformation.class, "macro");
        RenderingContext renderingContext = mocker.getInstance(RenderingContext.class);

        // It's very important to verify that a transformation context id is set as otherwise if the content being
        // edited has different velocity macros executing, they'll be executed in isolation and thus what's defined in
        // one won't be visible from the other ones (For example see http://jira.xwiki.org/browse/XWIKI-11695).
        ArgumentCaptor<TransformationContext> txContextArgument = ArgumentCaptor.forClass(TransformationContext.class);
        verify((MutableRenderingContext) renderingContext).transformInContext(same(macroTransformation),
            txContextArgument.capture(), same(xdom));
        assertEquals("wysiwygtxid", txContextArgument.getValue().getId());

        // Verify the XDOM is rendered to Annotated XHTML.
        BlockRenderer xhtmlRenderer = mocker.getInstance(BlockRenderer.class, "annotatedxhtml/1.0");
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
        HTMLCleaner cleaner = mocker.getInstance(HTMLCleaner.class);
        when(cleaner.clean(html)).thenReturn(html);

        // Verify the HTML is parsed into XDOM.
        XDOM xdom = new XDOM(Collections.<Block> emptyList());
        Parser xhtmlParser = mocker.getInstance(Parser.class, "xhtml/1.0");
        when(xhtmlParser.parse(any(StringReader.class))).thenReturn(xdom);

        // Verify the specified syntax is used.
        SyntaxFactory syntaxFactory = mocker.getInstance(SyntaxFactory.class);
        Syntax syntax = mock(Syntax.class);
        when(syntaxFactory.createSyntaxFromIdString(syntaxId)).thenReturn(syntax);

        Assert.assertEquals("", mocker.getComponentUnderTest().parseAndRender(html, syntaxId));

        // Verify that the macro transformations have been executed.
        Transformation macroTransformation = mocker.getInstance(Transformation.class, "macro");
        RenderingContext renderingContext = mocker.getInstance(RenderingContext.class);

        // It's very important to verify that a transformation context id is set as otherwise if the content being
        // edited has different velocity macros executing, they'll be executed in isolation and thus what's defined in
        // one won't be visible from the other ones (For example see http://jira.xwiki.org/browse/XWIKI-11695).
        ArgumentCaptor<TransformationContext> txContextArgument = ArgumentCaptor.forClass(TransformationContext.class);
        verify((MutableRenderingContext) renderingContext).transformInContext(same(macroTransformation),
            txContextArgument.capture(), same(xdom));
        assertEquals("wysiwygtxid", txContextArgument.getValue().getId());

        // Verify the XDOM is rendered to Annotated XHTML.
        BlockRenderer xhtmlRenderer = mocker.getInstance(BlockRenderer.class, "annotatedxhtml/1.0");
        verify(xhtmlRenderer).render(same(xdom), any(WikiPrinter.class));

        // Verify that the syntax meta data has been set.
        Assert.assertSame(syntax, xdom.getMetaData().getMetaData(MetaData.SYNTAX));
    }
}
