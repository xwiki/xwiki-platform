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
package org.xwiki.rendering.script;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.script.service.ScriptService;

/**
 * Provides Rendering-specific Scripting APIs.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Named("rendering")
@Singleton
public class RenderingScriptService implements ScriptService
{
    /**
     * Used to lookup parsers and renderers to discover available syntaxes.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    /**
     * @see #getDefaultTransformationNames()
     */
    @Inject
    private RenderingConfiguration configuration;

    /**
     * @see #resolveSyntax(String)
     */
    @Inject
    private SyntaxFactory syntaxFactory;

    @Inject
    private Logger logger;

    /**
     * @return the list of syntaxes for which a Parser is available
     */
    public List<Syntax> getAvailableParserSyntaxes()
    {
        List<Syntax> syntaxes = new ArrayList<Syntax>();
        try {
            for (Parser parser : this.componentManagerProvider.get().<Parser> getInstanceList(Parser.class)) {
                syntaxes.add(parser.getSyntax());
            }
        } catch (ComponentLookupException e) {
            // Failed to lookup parsers, consider there are no syntaxes available
            this.logger.error("Failed to lookup parsers", e);
        }

        return syntaxes;
    }

    /**
     * @return the list of syntaxes for which a Renderer is available
     */
    public List<Syntax> getAvailableRendererSyntaxes()
    {
        List<Syntax> syntaxes = new ArrayList<Syntax>();
        try {
            List<PrintRendererFactory> factories =
                this.componentManagerProvider.get().getInstanceList(PrintRendererFactory.class);
            for (PrintRendererFactory factory : factories) {
                syntaxes.add(factory.getSyntax());
            }
        } catch (ComponentLookupException e) {
            // Failed to lookup renderers, consider there are no syntaxes available
            this.logger.error("Failed to lookup renderers", e);
        }

        return syntaxes;
    }

    /**
     * @return the names of Transformations that are configured in the Rendering Configuration and which are used by the
     *         Transformation Manager when running all transformations
     */
    public List<String> getDefaultTransformationNames()
    {
        return this.configuration.getTransformationNames();
    }

    /**
     * Parses a text written in the passed syntax.
     * 
     * @param text the text to parse
     * @param syntaxId the id of the syntax in which the text is written in
     * @return the XDOM representing the AST of the parsed text or null if an error occurred
     * @since 3.2M3
     */
    public XDOM parse(String text, String syntaxId)
    {
        XDOM result;
        try {
            Parser parser = this.componentManagerProvider.get().getInstance(Parser.class, syntaxId);
            result = parser.parse(new StringReader(text));
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    /**
     * Render a list of Blocks into the passed syntax.
     * 
     * @param block the block to render
     * @param outputSyntaxId the syntax in which to render the blocks
     * @return the string representing the passed blocks in the passed syntax or null if an error occurred
     * @since 3.2M3
     */
    public String render(Block block, String outputSyntaxId)
    {
        String result;
        WikiPrinter printer = new DefaultWikiPrinter();
        try {
            BlockRenderer renderer =
                this.componentManagerProvider.get().getInstance(BlockRenderer.class, outputSyntaxId);
            renderer.render(block, printer);
            result = printer.toString();
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    /**
     * Converts a Syntax specified as a String into a proper Syntax object.
     * 
     * @param syntaxId the syntax as a string (eg "xwiki/2.0", "html/4.01", etc)
     * @return the proper Syntax object representing the passed syntax
     */
    public Syntax resolveSyntax(String syntaxId)
    {
        Syntax syntax;
        try {
            syntax = this.syntaxFactory.createSyntaxFromIdString(syntaxId);
        } catch (ParseException exception) {
            syntax = null;
        }
        return syntax;
    }
}
