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
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxRegistry;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.wysiwyg.converter.JakartaRequestParameterConversionResult;
import org.xwiki.wysiwyg.converter.RequestParameterConverter;
import org.xwiki.wysiwyg.filter.MutableJakartaServletRequest;

/**
 * The default {@link RequestParameterConverter} implementation, converting parameter values, where needed, from a
 * specified input syntax to a specified output syntax (without execution any rendering transformations).
 * For backward compatibility reasons this converter also perform a call to any other converter registered.
 *
 * @version $Id$
 * @since 13.5RC1
 */
@Component
@Singleton
public class DefaultRequestParameterConverter extends AbstractRequestParameterConverter
{
    @Inject
    private SyntaxRegistry syntaxRegistry;

    @Inject
    private RenderingContext renderingContext;

    @Inject
    @Named("context")
    private ComponentManager contextComponentManager;

    /**
     * @return the name of the request parameter whose multiple values indicate the request parameters that require
     *         conversion; for instance, if this parameter's value is {@code [description, content]} then the request
     *         has two parameters, {@code description} and {@code content}, requiring conversion; the input syntax is
     *         specified with another request parameter, e.g. {@code description_inputSyntax} and
     *         {@code content_inputSyntax}; the output syntax is specified with another request parameter as well, e.g.
     *         {@code description_outputSyntax} and {@code content_outputSyntax}
     */
    @Override
    protected String getConverterParameterName()
    {
        return "RequiresConversion";
    }

    @Override
    public JakartaRequestParameterConversionResult convert(ServletRequest request)
    {
        JakartaRequestParameterConversionResult result = super.convert(request);
        // and then loop over the other converters to ensure they're also properly called.
        try {
            for (Map.Entry<String, Object> entry : contextComponentManager.getInstanceMap(
                RequestParameterConverter.class).entrySet()) {
                // We want to ensure to not call again this current instance.
                if (!"default".equals(entry.getKey())) {
                    RequestParameterConverter converter = (RequestParameterConverter) entry.getValue();
                    JakartaRequestParameterConversionResult intermediateResult = converter.convert(result.getRequest());
                    result.getErrors().putAll(intermediateResult.getErrors());
                    result.getOutput().putAll(intermediateResult.getOutput());

                }
            }
        } catch (ComponentLookupException e) {
            this.logger.error("Error while getting RequestParameterConverter", e);
        }
        return result;
    }

    @Override
    protected void convert(String[] parametersRequiringConversion,
        JakartaRequestParameterConversionResult conversionResult)
    {
        MutableJakartaServletRequest request = conversionResult.getRequest();
        for (String parameterName : parametersRequiringConversion) {
            String content = request.getParameter(parameterName);
            // Remove the syntax parameters from the request to avoid interference with further request processing.
            Optional<Syntax> inputSyntax =
                this.syntaxRegistry.getSyntax(request.removeParameter(parameterName + "_inputSyntax"));
            Optional<Syntax> outputSyntax =
                this.syntaxRegistry.getSyntax(request.removeParameter(parameterName + "_outputSyntax"));
            if (content == null || !inputSyntax.isPresent() || !outputSyntax.isPresent()) {
                continue;
            }
            try {
                request.setParameter(parameterName, convert(content, inputSyntax.get(), outputSyntax.get()));
            } catch (Exception e) {
                this.logger.error(e.getLocalizedMessage(), e);
                conversionResult.getErrors().put(parameterName, e);
            }
            // If the conversion fails the output contains the value before the conversion.
            conversionResult.getOutput().put(parameterName, request.getParameter(parameterName));
        }
    }

    private String convert(String content, Syntax inputSyntax, Syntax outputSyntax)
        throws ComponentLookupException, ParseException
    {
        boolean renderingContextPushed = false;
        try {
            renderingContextPushed = maybeSetRenderingContextSyntax(outputSyntax);

            // Parse and render, without executing any rendering transformations.
            WikiPrinter printer = new DefaultWikiPrinter();
            PrintRendererFactory printRendererFactory =
                this.contextComponentManager.getInstance(PrintRendererFactory.class, outputSyntax.toIdString());
            StreamParser parser =
                this.contextComponentManager.getInstance(StreamParser.class, inputSyntax.toIdString());
            parser.parse(new StringReader(content), printRendererFactory.createRenderer(printer));

            return printer.toString();
        } finally {
            if (renderingContextPushed) {
                ((MutableRenderingContext) this.renderingContext).pop();
            }
        }
    }

    private boolean maybeSetRenderingContextSyntax(Syntax syntax)
    {
        if (this.renderingContext instanceof MutableRenderingContext mutableRenderingContext) {
            // Make sure we set the default syntax and the target syntax on the rendering context. This is needed
            // for instance when the content of a macro that was edited in-line is converted to wiki syntax.
            mutableRenderingContext.push(this.renderingContext.getTransformation(), this.renderingContext.getXDOM(),
                syntax, this.renderingContext.getTransformationId(), this.renderingContext.isRestricted(), syntax);
            return true;
        }
        return false;
    }
}
