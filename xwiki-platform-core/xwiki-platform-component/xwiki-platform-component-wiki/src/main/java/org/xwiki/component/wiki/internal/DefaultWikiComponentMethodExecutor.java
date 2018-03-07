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
package org.xwiki.component.wiki.internal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.wiki.WikiComponentRuntimeException;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;

/**
 * Default {@link WikiComponentMethodExecutor}.
 *
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Singleton
public class DefaultWikiComponentMethodExecutor implements WikiComponentMethodExecutor
{
    /**
     * The key under which the context document is kept in the XWiki context.
     */
    private static final String XWIKI_CONTEXT_DOC_KEY = "doc";

    /**
     * The execution context.
     */
    @Inject
    private Execution execution;

    /**
     * Used to update the rendering context.
     */
    @Inject
    private RenderingContext renderingContext;

    /**
     * Macro transformation engine.
     */
    @Inject
    @Named("macro")
    private Transformation macroTransformation;

    /**
     * Renderer used to get the return value from the rendered content.
     */
    @Inject
    @Named("plain/1.0")
    private BlockRenderer blockRenderer;

    /**
     * Converter used to cast the return value from the rendered content (String).
     */
    @Inject
    private ConverterManager converterManager;

    /**
     * Used to retrieve the component document.
     */
    @Inject
    private DocumentAccessBridge dab;

    /**
     * Prepare the method execution context.
     *
     * @param methodContext The context to populate
     * @param args The arguments initially passed to the method
     */
    private void prepareMethodContext(Map<String, Object> methodContext, Object[] args)
    {
        methodContext.put(OUTPUT_KEY, new WikiMethodOutputHandler());

        Map<Integer, Object> inputs = new HashMap<Integer, Object>();
        if (args != null && args.length > 0) {
            // Start with "0" as first input key.
            for (int i = 0; i < args.length; i++) {
                inputs.put(i, args[i]);
            }
        }
        methodContext.put(INPUT_KEY, inputs);
    }

    /**
     * Render a XDOM and return a value converted from the rendered content. The type matches the return value of the
     * passed method.
     *
     * @param xdom The XDOM to render
     * @param method The method called
     * @return A value matching the method return type
     * @throws WikiComponentRuntimeException When the conversion fails
     */
    private Object castRenderedContent(XDOM xdom, Method method) throws WikiComponentRuntimeException
    {
        // Since no return value has been explicitly provided, we try to convert the result of the rendering
        // into the expected return type using a Converter.
        WikiPrinter printer = new DefaultWikiPrinter();
        blockRenderer.render(xdom, printer);
        String contentResult = printer.toString();

        // Do the conversion!
        try {
            return converterManager.convert(method.getGenericReturnType(), contentResult);
        } catch (ConversionException e) {
            // Surrender!
            throw new WikiComponentRuntimeException(
                String.format("Failed to convert result [%s] to type [%s] for method [%s.%s]",
                    contentResult,
                    method.getGenericReturnType(),
                    method.getDeclaringClass().getName(),
                    method.getName()), e);
        }
    }

    @Override
    public Object execute(Method method, Object[] args, DocumentReference componentDocumentReference, XDOM xdom,
        Syntax syntax, Map<String, Object> methodContext)
        throws WikiComponentRuntimeException
    {
        // Prepare and put the method context in the XWiki Context
        Map<Object, Object> xwikiContext = (Map<Object, Object>) execution.getContext().getProperty("xwikicontext");
        this.prepareMethodContext(methodContext, args);
        xwikiContext.put("method", methodContext);
        // Save current context document, to put it back after the execution.
        Object contextDoc = xwikiContext.get(XWIKI_CONTEXT_DOC_KEY);

        try {
            // Put component document in the context, so that macro transformation rights are checked against the
            // component document and not the context one.
            try {
                xwikiContext.put(XWIKI_CONTEXT_DOC_KEY, dab.getDocumentInstance(componentDocumentReference));
            } catch (Exception e) {
                throw new WikiComponentRuntimeException(String.format(
                    "Failed to load wiki component document [%s]", componentDocumentReference), e);
            }

            // We need to clone the xdom to avoid transforming the original and make it useless after the first
            // transformation
            XDOM transformedXDOM = xdom.clone();

            // Perform internal macro transformations
            try {
                TransformationContext transformationContext = new TransformationContext(transformedXDOM, syntax);
                transformationContext.setId(method.getClass().getName() + "#" + method.getName());
                ((MutableRenderingContext) renderingContext).transformInContext(macroTransformation,
                    transformationContext, transformedXDOM);
            } catch (TransformationException e) {
                throw new WikiComponentRuntimeException(String.format(
                    "Error while executing wiki component macro transformation for method [%s]", method.getName()), e);
            }

            if (!method.getReturnType().getName().equals("void")) {
                if (methodContext.get(OUTPUT_KEY) != null
                    && ((WikiMethodOutputHandler)
                    methodContext.get(OUTPUT_KEY)).getValue() != null) {
                    return method.getReturnType().cast(((WikiMethodOutputHandler)
                        methodContext.get(OUTPUT_KEY)).getValue());
                } else {
                    return this.castRenderedContent(transformedXDOM, method);
                }
            } else {
                return null;
            }
        } finally {
            if (contextDoc != null) {
                xwikiContext.put(XWIKI_CONTEXT_DOC_KEY, contextDoc);
            }
        }
    }
}
