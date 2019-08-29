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
package com.xpn.xwiki.internal.template;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.internal.block.AbstractBlockAsyncRenderer;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererResult;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.printer.WriterWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
import org.xwiki.velocity.VelocityManager;

/**
 * Actually execute the template.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@Component(roles = TemplateAsyncRenderer.class)
public class TemplateAsyncRenderer extends AbstractBlockAsyncRenderer
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private AuthorizationManager authorization;

    @Inject
    private JobProgressManager progress;

    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainRenderer;

    @Inject
    private ContentParser parser;

    @Inject
    private VelocityManager velocityManager;

    @Inject
    private Logger logger;

    private Template template;

    private List<String> id;

    private boolean inline;

    private Syntax targetSyntax;

    void initialize(Template template)
    {
        this.template = template;

        this.inline = syncContext.isInline();
        this.targetSyntax = syncContext.getTransformationContext().getTargetSyntax();

        this.id = Arrays.asList("template", template.getId());
    }

    @Override
    public List<String> getId()
    {
        return this.id;
    }

    @Override
    public boolean isAsyncAllowed()
    {
        return this.template.getContent().isAsyncAllowed();
    }

    @Override
    public boolean isCacheAllowed()
    {
        return this.template.getContent().isCacheAllowed();
    }

    @Override
    public boolean isInline()
    {
        return this.inline;
    }

    @Override
    public BlockAsyncRendererResult render(boolean async, boolean cached) throws RenderingException
    {
        // TODO: Register the known involved references and components

        // Execute the template
        XDOM result = execute(this.template, this.template.getContent());

        ///////////////////////////////////////
        // Rendering

        String resultString = null;

        if (async || cached) {
            BlockRenderer renderer;
            try {
                renderer = this.componentManager.get().getInstance(BlockRenderer.class, this.targetSyntax.toIdString());
            } catch (ComponentLookupException e) {
                throw new RenderingException("Failed to lookup renderer for syntax [" + this.targetSyntax + "]", e);
            }

            WikiPrinter printer = new DefaultWikiPrinter();
            renderer.render(result, printer);

            resultString = printer.toString();
        }

        return new BlockAsyncRendererResult(resultString, result);
    }

    private void render(Template template, TemplateContent content, Writer writer) throws Exception
    {
        if (content.getSourceSyntax() != null) {
            XDOM xdom = execute(template, content);

            render(xdom, writer);
        } else {
            evaluateContent(template, content, writer);
        }
    }

    private void render(XDOM xdom, Writer writer)
    {
        WikiPrinter printer = new WriterWikiPrinter(writer);

        BlockRenderer blockRenderer;
        try {
            blockRenderer =
                this.componentManagerProvider.get().getInstance(BlockRenderer.class, getTargetSyntax().toIdString());
        } catch (ComponentLookupException e) {
            blockRenderer = this.plainRenderer;
        }

        blockRenderer.render(xdom, printer);
    }

    private XDOM execute(Template template, TemplateContent content) throws Exception
    {
        XDOM xdom = getXDOM(template, content);

        transform(xdom);

        return xdom;
    }

    public XDOM getXDOM(Template template) throws Exception
    {
        XDOM xdom;

        if (template != null) {
            xdom = getXDOM(template, template.getContent());
        } else {
            xdom = new XDOM(Collections.<Block>emptyList());
        }

        return xdom;
    }

    private XDOM getXDOM(Template template, TemplateContent content) throws Exception
    {
        XDOM xdom;

        if (content.getSourceSyntax() != null) {
            xdom = this.parser.parse(content.getContent(), content.getSourceSyntax());
        } else {
            String result = evaluateContent(template, content);
            if (StringUtils.isEmpty(result)) {
                xdom = new XDOM(Collections.emptyList());
            } else {
                xdom = new XDOM(Arrays.asList(new RawBlock(result,
                    content.getRawSyntax() != null ? content.getRawSyntax() : renderingContext.getTargetSyntax())));
            }
        }

        return xdom;
    }

    private void transform(Block block)
    {
        TransformationContext transformationContext =
            new TransformationContext(block instanceof XDOM ? (XDOM) block : new XDOM(Arrays.asList(block)),
                this.renderingContext.getDefaultSyntax(), this.renderingContext.isRestricted());

        transformationContext.setId(this.renderingContext.getTransformationId());
        transformationContext.setTargetSyntax(getTargetSyntax());

        try {
            transform(block, transformationContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Syntax getTargetSyntax()
    {
        Syntax targetSyntax = this.renderingContext.getTargetSyntax();

        return targetSyntax != null ? targetSyntax : Syntax.PLAIN_1_0;
    }

    private String evaluateContent(Template template, TemplateContent content) throws Exception
    {
        Writer writer = new StringWriter();

        evaluateContent(template, content, writer);

        return writer.toString();
    }

    private void evaluateContent(Template template, TemplateContent content, Writer writer) throws Exception
    {
        // Make sure the author of the template has script right (required to execute Velocity)
        if (content.isAuthorProvided()) {
            this.authorization.checkAccess(Right.SCRIPT, content.getAuthorReference(), content.getDocumentReference());
        }

        // Use the Transformation id as the name passed to the Velocity Engine. This name is used internally
        // by Velocity as a cache index key for caching macros.
        String namespace = this.renderingContext.getTransformationId();

        boolean renderingContextPushed = false;
        if (namespace == null) {
            namespace = template.getId() != null ? template.getId() : "unknown namespace";

            if (this.renderingContext instanceof MutableRenderingContext) {
                // Make the current velocity template id available
                ((MutableRenderingContext) this.renderingContext).push(this.renderingContext.getTransformation(),
                    this.renderingContext.getXDOM(), this.renderingContext.getDefaultSyntax(), namespace,
                    this.renderingContext.isRestricted(), this.renderingContext.getTargetSyntax());

                renderingContextPushed = true;
            }
        }

        this.progress.startStep(template, "template.evaluateContent.message",
            "Evaluate content of template with id [{}]", template.getId());

        try {
            this.velocityManager.evaluate(writer, namespace, new StringReader(content.getContent()));
        } finally {
            // Get rid of temporary rendering context
            if (renderingContextPushed) {
                ((MutableRenderingContext) this.renderingContext).pop();
            }

            this.progress.endStep(template);
        }
    }
}
