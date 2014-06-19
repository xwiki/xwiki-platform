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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.MissingParserException;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * Internal toolkit to experiment on wiki bases templates.
 * 
 * @version $Id$
 * @since 6.1M1
 */
@Component(roles = WikiTemplateRenderer.class)
@Singleton
public class WikiTemplateRenderer
{
    private static final Pattern FIRSTLINE = Pattern.compile("^##(target|raw)\\.syntax=(.*)$\r?\n?", Pattern.MULTILINE);

    @Inject
    private Environment environment;

    @Inject
    private ContentParser parser;

    @Inject
    private SyntaxFactory syntaxFactory;

    @Inject
    private VelocityManager velocityManager;

    /**
     * Used to execute transformations.
     */
    @Inject
    private TransformationManager transformationManager;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private RenderingContext renderingContext;

    @Inject
    private Logger logger;

    private class StringContent
    {
        public String content;

        public Syntax sourceSyntax;

        public Syntax rawSyntax;

        public StringContent(String content, Syntax sourceSyntax, Syntax rawSyntax)
        {
            this.content = content;
            this.sourceSyntax = sourceSyntax;
            this.rawSyntax = rawSyntax;
        }
    }

    private StringContent getStringContent(String template) throws IOException, ParseException
    {
        InputStream stream = this.environment.getResourceAsStream(template);

        String content;
        try {
            content = IOUtils.toString(stream, "UTF-8");
        } finally {
            IOUtils.closeQuietly(stream);
        }

        Matcher matcher = FIRSTLINE.matcher(content);

        if (matcher.find()) {
            content = content.substring(matcher.end());

            String syntaxString = matcher.group(2);
            Syntax syntax = this.syntaxFactory.createSyntaxFromIdString(syntaxString);

            String mode = matcher.group(1);
            switch (mode) {
                case "source":
                    return new StringContent(content, syntax, null);
                case "raw":
                    return new StringContent(content, null, syntax);
                default:
                    break;
            }
        }

        return new StringContent(content, Syntax.PLAIN_1_0, null);
    }

    protected XDOM generateError(Throwable throwable)
    {
        List<Block> errorBlocks = new ArrayList<Block>();

        // Add short message
        Map<String, String> errorBlockParams = Collections.singletonMap("class", "xwikirenderingerror");
        errorBlocks.add(new GroupBlock(Arrays.<Block> asList(new WordBlock("Failed to render step content")),
            errorBlockParams));

        // Add complete error
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        Block descriptionBlock = new VerbatimBlock(writer.toString(), false);
        Map<String, String> errorDescriptionBlockParams =
            Collections.singletonMap("class", "xwikirenderingerrordescription hidden");
        errorBlocks.add(new GroupBlock(Arrays.asList(descriptionBlock), errorDescriptionBlockParams));

        return new XDOM(errorBlocks);
    }

    private void transform(Block block, String transformationId)
    {
        TransformationContext txContext =
            new TransformationContext(block instanceof XDOM ? (XDOM) block : new XDOM(Arrays.asList(block)), null,
                false);

        txContext.setId(transformationId);

        try {
            this.transformationManager.performTransformations(block, txContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public XDOM getXDOM(String template)
    {
        return getXDOM(template, this.renderingContext.getTransformationId());
    }

    /**
     * @param template the template to parse
     * @return the result of the template parsing
     */
    public XDOM getXDOM(String template, String transformationId)
    {
        XDOM xdom;

        try {
            StringContent content = getStringContent(template);

            if (content.sourceSyntax != null) {
                xdom = this.parser.parse(content.content, content.sourceSyntax);
            } else if (content.rawSyntax != null) {
                String result = evaluateString(content.content, null);
                xdom = new XDOM(Arrays.asList(new RawBlock(result, content.rawSyntax)));
            } else {
                xdom = new XDOM(Collections.<Block> emptyList());
            }
        } catch (Throwable e) {
            xdom = generateError(e);
        }

        return xdom;
    }

    public String render(String template, Syntax targetSyntax) throws ComponentLookupException, ParseException,
        MissingParserException, IOException, XWikiVelocityException
    {
        return render(template, targetSyntax, this.renderingContext.getTransformationId());
    }

    public String render(String template, Syntax targetSyntax, String transformationId)
        throws ComponentLookupException, ParseException, MissingParserException, IOException, XWikiVelocityException
    {
        XDOM xdom = getXDOM(template, transformationId);

        transform(xdom, transformationId);

        WikiPrinter printer = new DefaultWikiPrinter();

        BlockRenderer blockRenderer =
            this.componentManagerProvider.get().getInstance(BlockRenderer.class, targetSyntax.toIdString());
        blockRenderer.render(xdom, printer);

        return printer.toString();
    }

    protected String evaluateString(String content, String transformationId) throws XWikiVelocityException
    {
        VelocityContext velocityContext = this.velocityManager.getVelocityContext();

        StringWriter writer = new StringWriter();

        // Use the Transformation id as the name passed to the Velocity Engine. This name is used internally
        // by Velocity as a cache index key for caching macros.
        String namespace = transformationId;
        if (namespace == null) {
            namespace = "unknown namespace";
        }

        VelocityEngine velocityEngine = this.velocityManager.getVelocityEngine();

        velocityEngine.startedUsingMacroNamespace(namespace);
        try {
            velocityEngine.evaluate(velocityContext, writer, namespace, content);
        } finally {
            velocityEngine.stoppedUsingMacroNamespace(namespace);
        }

        return writer.toString();
    }
}
