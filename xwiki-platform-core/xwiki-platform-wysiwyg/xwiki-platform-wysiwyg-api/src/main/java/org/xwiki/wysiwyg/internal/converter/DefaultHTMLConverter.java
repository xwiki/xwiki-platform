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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.wysiwyg.cleaner.HTMLCleaner;
import org.xwiki.wysiwyg.converter.HTMLConverter;

/**
 * Converts HTML into/from markup syntax.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultHTMLConverter implements HTMLConverter
{
    private static final String TRANSFORMATION_ID = "wysiwygtxid";

    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    /**
     * The component used to clean the HTML before the conversion.
     */
    @Inject
    private HTMLCleaner htmlCleaner;

    /**
     * The component used to parse the XHTML obtained after cleaning.
     */
    @Inject
    @Named("xhtml/5")
    private Parser xhtmlParser;

    /**
     * The component used to parse the XHTML obtained after cleaning, when transformations are not executed.
     */
    @Inject
    @Named("xhtml/5")
    private StreamParser xhtmlStreamParser;

    /**
     * Used to update the rendering context.
     */
    @Inject
    private RenderingContext renderingContext;

    @Inject
    private ContentParser contentParser;

    /**
     * The component used to execute the XDOM macro transformations before rendering to XHTML.
     * <p>
     * NOTE: We execute only macro transformations because they are the only transformations protected by the WYSIWYG
     * editor. We should use the transformation manager once generic transformation markers are implemented in the
     * rendering module and the WYSIWYG editor supports them.
     * 
     * @see <a href="https://jira.xwiki.org/browse/XRENDERING-78">XWIKI-3260: Add markers to modified XDOM by
     *      Transformations/Macros</a>
     */
    @Inject
    @Named("macro")
    private Transformation macroTransformation;

    /**
     * The component used to render a XDOM to XHTML.
     */
    @Inject
    @Named("annotatedhtml/5.0")
    private BlockRenderer xhtmlRenderer;

    /**
     * The component manager. We need it because we have to access some components dynamically based on the input
     * syntax.
     */
    @Inject
    @Named("context")
    private ComponentManager contextComponentManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public String fromHTML(String dirtyHTML, String syntaxId)
    {
        boolean renderingContextPushed = false;
        try {
            // Clean
            String html = this.htmlCleaner.clean(dirtyHTML);

            renderingContextPushed = maybeSetRenderingContextSyntax(Syntax.valueOf(syntaxId));

            // Parse & Render
            // Note that transformations are not executed when converting XHTML to source syntax.
            WikiPrinter printer = new DefaultWikiPrinter();
            PrintRendererFactory printRendererFactory =
                this.contextComponentManager.getInstance(PrintRendererFactory.class, syntaxId);
            this.xhtmlStreamParser.parse(new StringReader(html), printRendererFactory.createRenderer(printer));

            return printer.toString();
        } catch (Exception e) {
            this.logger.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Exception while parsing HTML", e);
        } finally {
            if (renderingContextPushed) {
                ((MutableRenderingContext) this.renderingContext).pop();
            }
        }
    }

    @Override
    public String toHTML(String source, String syntaxId)
    {
        return toHTML(source, getSyntax(syntaxId), null);
    }

    @Override
    public String toHTML(String source, Syntax syntax, EntityReference sourceReference)
    {
        return toHTML(source, syntax, sourceReference, false);
    }

    @Override
    public String toHTML(String source, Syntax syntax, EntityReference sourceReference, boolean restricted)
    {
        try {
            // Parse
            XDOM xdom = this.contentParser.parse(source, syntax, sourceReference);

            // Execute the macro transformation
            executeMacroTransformation(xdom, syntax, restricted);

            // Render
            WikiPrinter printer = new DefaultWikiPrinter();
            this.xhtmlRenderer.render(xdom, printer);

            return printer.toString();
        } catch (Exception e) {
            this.logger.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Exception while rendering HTML", e);
        }
    }

    @Override
    public String parseAndRender(String dirtyHTML, String syntaxId)
    {
        return parseAndRender(dirtyHTML, getSyntax(syntaxId), null);
    }

    @Override
    public String parseAndRender(String dirtyHTML, Syntax syntax, EntityReference sourceReference)
    {
        return parseAndRender(dirtyHTML, syntax, sourceReference, false);
    }

    @Override
    public String parseAndRender(String dirtyHTML, Syntax syntax, EntityReference sourceReference, boolean restricted)
    {
        boolean renderingContextPushed = false;
        try {
            // Clean
            String html = this.htmlCleaner.clean(dirtyHTML);

            renderingContextPushed = maybeSetRenderingContextSyntax(syntax);

            // Parse
            XDOM xdom = this.xhtmlParser.parse(new StringReader(html));

            // Add the source reference to be a good citizen
            if (sourceReference != null) {
                xdom.getMetaData().addMetaData(MetaData.SOURCE, this.serializer.serialize(sourceReference));
            }

            // The XHTML parser sets the "syntax" meta data property of the created XDOM to "xhtml/1.0". The syntax meta
            // data is used as the default syntax for macro content. We have to change this to the specified syntax
            // because HTML is used only to be able to edit the source syntax in the WYSIWYG editor.
            xdom.getMetaData().addMetaData(MetaData.SYNTAX, syntax);

            // Execute the macro transformation
            executeMacroTransformation(xdom, syntax, restricted);

            // Render
            WikiPrinter printer = new DefaultWikiPrinter();
            this.xhtmlRenderer.render(xdom, printer);

            return printer.toString();
        } catch (Exception e) {
            this.logger.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Exception while refreshing HTML", e);
        } finally {
            if (renderingContextPushed) {
                ((MutableRenderingContext) this.renderingContext).pop();
            }
        }
    }

    private Syntax getSyntax(String syntaxId)
    {
        try {
            return Syntax.valueOf(syntaxId);
        } catch (ParseException e) {
            throw new RuntimeException(String.format("Invalid syntax [%s]", syntaxId), e);
        }
    }

    private boolean maybeSetRenderingContextSyntax(Syntax syntax)
    {
        if (this.renderingContext instanceof MutableRenderingContext) {
            // Make sure we set the default syntax and the target syntax on the rendering context. This is needed
            // for instance when the content of a macro that was edited in-line is converted to wiki syntax.
            ((MutableRenderingContext) this.renderingContext).push(this.renderingContext.getTransformation(),
                this.renderingContext.getXDOM(), syntax, this.renderingContext.getTransformationId(),
                this.renderingContext.isRestricted(), syntax);
            return true;
        }
        return false;
    }

    private void executeMacroTransformation(XDOM xdom, Syntax syntax, boolean restricted) throws TransformationException
    {
        TransformationContext txContext = new TransformationContext();
        txContext.setXDOM(xdom);
        txContext.setSyntax(syntax);
        txContext.setTargetSyntax(Syntax.ANNOTATED_HTML_5_0);
        txContext.setRestricted(restricted);

        // It's very important to set a Transformation id as otherwise if any Velocity Macro is executed it'll be
        // executed in isolation (and if you have, say, 2 velocity macros, the second one will not 'see' what's defined
        // in the first one...
        txContext.setId(TRANSFORMATION_ID);

        if (this.renderingContext instanceof MutableRenderingContext) {
            ((MutableRenderingContext) this.renderingContext).transformInContext(this.macroTransformation, txContext,
                xdom);
        }
    }
}
