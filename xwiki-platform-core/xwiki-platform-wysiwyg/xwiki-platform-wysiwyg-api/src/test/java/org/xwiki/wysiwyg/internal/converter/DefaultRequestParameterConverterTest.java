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

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Named;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxRegistry;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wysiwyg.converter.JakartaRequestParameterConversionResult;
import org.xwiki.wysiwyg.converter.RequestParameterConverter;
import org.xwiki.wysiwyg.filter.MutableJakartaServletRequest;
import org.xwiki.wysiwyg.internal.filter.http.MutableHttpServletRequestFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultRequestParameterConverter}.
 */
@ComponentTest
@ComponentList({MutableHttpServletRequestFactory.class})
class DefaultRequestParameterConverterTest
{
    @InjectMockComponents
    private DefaultRequestParameterConverter converter;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private SyntaxRegistry syntaxRegistry;

    @MockComponent(classToMock = MutableRenderingContext.class)
    private RenderingContext renderingContext;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @Mock
    private PrintRendererFactory printRendererFactory;

    @Mock
    private StreamParser streamParser;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private Syntax inputSyntax = new Syntax(new SyntaxType("input", "Input Syntax"), "1.0");

    private Syntax outputSyntax = new Syntax(new SyntaxType("output", "Output Syntax"), "1.0");

    @BeforeEach
    void configure() throws Exception
    {
        when(this.syntaxRegistry.getSyntax("input/1.0")).thenReturn(Optional.of(this.inputSyntax));
        when(this.syntaxRegistry.getSyntax("output/1.0")).thenReturn(Optional.of(this.outputSyntax));

        when(this.contextComponentManager.getInstance(PrintRendererFactory.class, "output/1.0"))
            .thenReturn(this.printRendererFactory);
        when(this.contextComponentManager.getInstance(StreamParser.class, "input/1.0")).thenReturn(this.streamParser);
    }

    @Test
    void convert() throws Exception
    {
        when(this.request.getParameterValues("RequiresConversion")).thenReturn(new String[] {"test"});
        when(this.request.getParameterValues("RequiresConversionHtml")).thenReturn(new String[] {"testHtml"});
        when(this.request.getParameterMap()).thenReturn(Map.of(
            "test", new String[] {"content"},
            "testHtml", new String[] {"Other content"},
            "test_inputSyntax", new String[] {"input/1.0"},
            "test_outputSyntax", new String[] {"output/1.0"}
        ));

        PrintRenderer printRenderer = mock();
        ArgumentCaptor<WikiPrinter> wikiPrinterCaptor = ArgumentCaptor.forClass(WikiPrinter.class);
        when(this.printRendererFactory.createRenderer(wikiPrinterCaptor.capture())).thenReturn(printRenderer);
        doAnswer(invocation -> {
            Reader stringReader = invocation.getArgument(0);
            WikiPrinter wikiPrinter = wikiPrinterCaptor.getValue();
            wikiPrinter.print("converted " + IOUtils.toString(stringReader));
            return null;
        }).when(this.streamParser).parse(any(Reader.class), eq(printRenderer));

        Transformation transformation = mock(Transformation.class);
        when(this.renderingContext.getTransformation()).thenReturn(transformation);
        XDOM xdom = mock(XDOM.class);
        when(this.renderingContext.getXDOM()).thenReturn(xdom);
        when(this.renderingContext.getTransformationId()).thenReturn("transformationId");
        when(this.renderingContext.isRestricted()).thenReturn(false);

        RequestParameterConverter converter1 = mock(RequestParameterConverter.class);
        RequestParameterConverter converter2 = mock(RequestParameterConverter.class);
        RequestParameterConverter defaultConverter = mock(RequestParameterConverter.class);
        Map<String, Object> instanceMap = new LinkedHashMap<>();
        instanceMap.put("converter1", converter1);
        instanceMap.put("default", defaultConverter);
        instanceMap.put("converter2", converter2);
        when(this.contextComponentManager.getInstanceMap(RequestParameterConverter.class)).thenReturn(instanceMap);

        when(converter1.convert(any(ServletRequest.class))).then(invocationOnMock -> {
            ServletRequest request = invocationOnMock.getArgument(0);
            assertEquals("converted content", request.getParameter("test"));
            assertEquals("Other content", request.getParameter("testHtml"));
            assertEquals(2, request.getParameterMap().size());

            JakartaRequestParameterConversionResult result = mock(JakartaRequestParameterConversionResult.class);
            when(result.getRequest()).thenReturn((MutableJakartaServletRequest) request);
            when(result.getErrors()).thenReturn(Map.of());
            when(result.getOutput()).thenReturn(Map.of(
                "testHtml", "modified html content",
                "test", "converted content"
            ));
            return result;
        });
        when(converter2.convert(any(ServletRequest.class))).then(invocationOnMock -> {
            ServletRequest request = invocationOnMock.getArgument(0);
            assertEquals("converted content", request.getParameter("test"));
            assertEquals("Other content", request.getParameter("testHtml"));
            assertEquals(2, request.getParameterMap().size());

            JakartaRequestParameterConversionResult result = mock(JakartaRequestParameterConversionResult.class);
            when(result.getRequest()).thenReturn((MutableJakartaServletRequest) request);
            when(result.getErrors()).thenReturn(Map.of());
            when(result.getOutput()).thenReturn(Map.of());
            return result;
        });

        Optional<ServletRequest> filteredRequestOpt = this.converter.convert(this.request, this.response);
        assertTrue(filteredRequestOpt.isPresent());
        ServletRequest filteredRequest = filteredRequestOpt.get();
        assertEquals("converted content", filteredRequest.getParameter("test"));
        assertEquals("Other content", filteredRequest.getParameter("testHtml"));
        verify((MutableRenderingContext) this.renderingContext).push(transformation, xdom, outputSyntax,
            "transformationId", false, outputSyntax);
        verify((MutableRenderingContext) this.renderingContext).pop();
    }
}
