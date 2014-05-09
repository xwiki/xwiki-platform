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
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;

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
    private static final Pattern FIRSTLINE = Pattern.compile("^.#syntax=(.*)$", Pattern.MULTILINE);

    @Inject
    private Environment environment;

    @Inject
    private ContentParser parser;

    @Inject
    private SyntaxFactory syntaxFactory;

    /**
     * Used to execute transformations.
     */
    @Inject
    private transient TransformationManager transformationManager;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    private class StringContent
    {
        public String content;

        public Syntax syntax;

        public StringContent(String content, Syntax syntax)
        {
            this.content = content;
            this.syntax = syntax;
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

        Syntax syntax;
        if (matcher.find()) {
            String syntaxString = matcher.group(1);
            syntax = this.syntaxFactory.createSyntaxFromIdString(syntaxString);
            content = content.substring(matcher.end());
        } else {
            syntax = Syntax.PLAIN_1_0;
        }

        return new StringContent(content, syntax);
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

    /**
     * @param template the template to parse
     * @return the result of the template parsing
     */
    public XDOM getXDOM(String template)
    {
        XDOM xdom;

        try {
            StringContent content = getStringContent(template);

            xdom = this.parser.parse(content.content, content.syntax);
        } catch (Throwable e) {
            xdom = generateError(e);
        }

        return xdom;
    }

    public String render(String template, String transformationId, Syntax targetSyntax) throws ComponentLookupException
    {
        Block block = getXDOM(template);

        transform(block, transformationId);

        WikiPrinter printer = new DefaultWikiPrinter();

        BlockRenderer blockRenderer =
            this.componentManagerProvider.get().getInstance(BlockRenderer.class, targetSyntax.toIdString());
        blockRenderer.render(block, printer);

        return printer.toString();
    }
}
