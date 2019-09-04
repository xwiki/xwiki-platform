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

import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.internal.block.AbstractBlockAsyncRenderer;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererResult;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;

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
    private ContentParser parser;

    @Inject
    private VelocityTemplateEvaluator evaluator;

    @Inject
    private AsyncContext asyncContext;

    private Template template;

    private List<String> id;

    private boolean inline;

    private Syntax targetSyntax;

    private String transformationId;

    private TemplateContent content;

    private boolean xdomMode;

    Set<String> initialize(Template template, boolean inline, boolean xdomMode) throws Exception
    {
        this.template = template;
        this.content = template.getContent();
        this.xdomMode = xdomMode;

        this.inline = inline;
        this.transformationId = this.renderingContext.getTransformationId();

        Syntax contextTargetSyntax = this.renderingContext.getTargetSyntax();
        this.targetSyntax = contextTargetSyntax != null ? contextTargetSyntax : Syntax.PLAIN_1_0;

        this.id = Arrays.asList("template", template.getId(), this.targetSyntax.toIdString(), String.valueOf(inline));

        return this.content.getContextEntries();
    }

    @Override
    public List<String> getId()
    {
        return this.id;
    }

    @Override
    public boolean isAsyncAllowed()
    {
        return this.content.isAsyncAllowed();
    }

    @Override
    public boolean isCacheAllowed()
    {
        return this.content.isCacheAllowed();
    }

    @Override
    public boolean isInline()
    {
        return this.inline;
    }

    @Override
    public Syntax getTargetSyntax()
    {
        return this.targetSyntax;
    }

    @Override
    public BlockAsyncRendererResult render(boolean async, boolean cached) throws RenderingException
    {
        // Register the known involved references
        if (this.content.getDocumentReference() != null) {
            this.asyncContext.useEntity(this.content.getDocumentReference());
        }

        if (this.content.getSourceSyntax() != null) {
            return renderWiki(async, cached);
        } else {
            return renderVelocity(async, cached);
        }
    }

    private BlockAsyncRendererResult renderWiki(boolean async, boolean cached) throws RenderingException
    {
        ///////////////////////////////////////
        // Parsing and execution

        Block xdom;
        try {
            xdom = this.parser.parse(this.content.getContent(), this.content.getSourceSyntax());

            transform(xdom);
        } catch (Exception e) {
            throw new RenderingException("Failed to execute template", e);
        }

        ///////////////////////////////////////
        // Inline

        // If in inline mode remove any top level paragraph.
        if (isInline()) {
            xdom = removeTopLevelParagraph(xdom);
        }

        ///////////////////////////////////////
        // Rendering

        String resultString;
        if (cached || async || !this.xdomMode) {
            BlockRenderer renderer;
            try {
                renderer = this.componentManager.get().getInstance(BlockRenderer.class, this.targetSyntax.toIdString());
            } catch (ComponentLookupException e) {
                throw new RenderingException("Failed to lookup renderer for syntax [" + this.targetSyntax + "]", e);
            }

            WikiPrinter printer = new DefaultWikiPrinter();
            renderer.render(xdom, printer);

            resultString = printer.toString();
        } else {
            resultString = null;
        }

        return new BlockAsyncRendererResult(resultString, xdom);
    }

    private BlockAsyncRendererResult renderVelocity(boolean async, boolean cached) throws RenderingException
    {
        ///////////////////////////////////////
        // Velocity

        String result = evaluateContent(this.template, this.content);

        ///////////////////////////////////////
        // XDOM

        XDOM xdom;
        if (cached || this.xdomMode) {
            if (StringUtils.isEmpty(result)) {
                xdom = new XDOM(Collections.emptyList());
            } else {
                xdom = new XDOM(Arrays.asList(new RawBlock(result,
                    this.content.getRawSyntax() != null ? this.content.getRawSyntax() : this.targetSyntax)));
            }
        } else {
            xdom = null;
        }

        return new BlockAsyncRendererResult(result, xdom);
    }

    private void transform(Block block) throws TransformationException
    {
        TransformationContext transformationContext =
            new TransformationContext(block instanceof XDOM ? (XDOM) block : new XDOM(Arrays.asList(block)),
                this.renderingContext.getDefaultSyntax(), this.renderingContext.isRestricted());

        transformationContext.setId(this.transformationId);
        transformationContext.setTargetSyntax(this.targetSyntax);

        transform(block, transformationContext);
    }

    private String evaluateContent(Template template, TemplateContent content) throws RenderingException
    {
        Writer writer = new StringWriter();

        try {
            this.evaluator.evaluateContent(template, content, writer);
        } catch (Exception e) {
            throw new RenderingException("Failed to evaluate template", e);
        }

        return writer.toString();
    }
}
