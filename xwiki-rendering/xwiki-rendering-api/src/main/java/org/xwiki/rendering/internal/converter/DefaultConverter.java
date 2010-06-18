package org.xwiki.rendering.internal.converter;

import java.io.Reader;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.converter.ConversionException;
import org.xwiki.rendering.converter.Converter;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.TransformationManager;

/**
 * Default implementation for {@link Converter}.
 * 
 * @version $Id$
 */
@Component
public class DefaultConverter implements Converter
{
    /**
     * Used to lookup parser and renderer.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Used to execute transformations.
     */
    @Requirement
    private TransformationManager transformationManager;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.converter.Converter#convert(java.io.Reader, org.xwiki.rendering.syntax.Syntax, org.xwiki.rendering.syntax.Syntax, org.xwiki.rendering.renderer.printer.WikiPrinter)
     */
    public void convert(Reader source, Syntax sourceSyntax, Syntax targetSyntax, WikiPrinter printer)
        throws ConversionException
    {
        // Step 1: Find the parser and generate a XDOM
        XDOM xdom;
        try {
            Parser parser = this.componentManager.lookup(Parser.class, sourceSyntax.toIdString());
            xdom = parser.parse(source);
        } catch (ComponentLookupException e) {
            throw new ConversionException("Failed to locate Parser for syntax [" + sourceSyntax + "]", e);
        } catch (ParseException e) {
            throw new ConversionException("Failed to parse input source", e);
        }

        // Step 2: Run transformations
        try {
            this.transformationManager.performTransformations(xdom, new TransformationContext(xdom, sourceSyntax));
        } catch (TransformationException e) {
            throw new ConversionException("Failed to execute some transformations", e);
        }

        // Step 3: Locate the Renderer and render the content in the passed printer
        BlockRenderer renderer;
        try {
            renderer = this.componentManager.lookup(BlockRenderer.class, targetSyntax.toIdString());
        } catch (ComponentLookupException e) {
            throw new ConversionException("Failed to locate Renderer for syntax [" + targetSyntax + "]", e);
        }
        renderer.render(xdom, printer);
    }
}
