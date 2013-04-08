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
package org.xwiki.extension.distribution.internal.job.step;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;

public abstract class AbstractDistributionStep implements DistributionStep
{
    protected class StringContent
    {
        public String content;

        public Syntax syntax;

        public StringContent(String content, Syntax syntax)
        {
            this.content = content;
            this.syntax = syntax;
        }
    }

    private static final Pattern FIRSTLINE = Pattern.compile("^.#syntax=(.*)$", Pattern.MULTILINE);

    @Inject
    private ComponentManager componentManager;

    @Inject
    private TransformationManager transformationManager;

    @Inject
    private Environment environment;

    @Inject
    private SyntaxFactory syntaxFactory;

    private String stepId;

    private State state;

    public AbstractDistributionStep(String stepId)
    {
        this.stepId = stepId;
    }

    @Override
    public void initialize(DistributionStep step)
    {
        this.state = step.getState();
    }

    @Override
    public String getId()
    {
        return this.stepId;
    }

    @Override
    public State getState()
    {
        return this.state;
    }

    @Override
    public void setState(State stepState)
    {
        this.state = stepState;
    }

    protected String getTemplate()
    {
        return "/templates/distribution/" + getId() + ".wiki";
    }

    protected StringContent getStringContent() throws IOException, ParseException
    {
        InputStream stream = this.environment.getResourceAsStream(getTemplate());

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
            throw new ParseException("Distribution step template [" + getTemplate() + "] does not provide its syntax");
        }

        return new StringContent(content, syntax);
    }

    protected XDOM getXDOM()
    {
        XDOM xdom;

        try {
            StringContent content = getStringContent();

            Parser parser = this.componentManager.getInstance(Parser.class, content.syntax.toIdString());
            xdom = parser.parse(new StringReader(content.content));
        } catch (Throwable e) {
            xdom = generateError(e);
        }

        return xdom;
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

    @Override
    public Block render()
    {
        XDOM content = getXDOM();

        TransformationContext txContext = new TransformationContext(content, null, false);
        try {
            this.transformationManager.performTransformations(content, txContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return content;
    }
}
