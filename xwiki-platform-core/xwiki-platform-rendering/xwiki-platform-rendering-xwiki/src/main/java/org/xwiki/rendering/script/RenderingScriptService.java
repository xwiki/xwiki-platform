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
            for (Parser parser : this.componentManagerProvider.get().<Parser>getInstanceList(Parser.class)) {
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

    /**
     * Escapes a give text using the escaping method specific to the given syntax.
     * <p/>
     * One example of escaping method is using escape characters like {@code ~} for the {@link Syntax#XWIKI_2_1} syntax
     * on all or just some characters of the given text.
     * <p/>
     * The current implementation only escapes XWiki 1.0, 2.0 and 2.1 syntaxes.
     *
     * @param content the text to escape
     * @param syntax the syntax to escape the content in (e.g. {@link Syntax#XWIKI_1_0}, {@link Syntax#XWIKI_2_0},
     *            {@link Syntax#XWIKI_2_1}, etc.). This is the syntax where the output will be used and not necessarily
     *            the same syntax of the input content
     * @return the escaped text or {@code null} if the given content or the given syntax are {@code null}, or if the
     *         syntax is not supported
     * @since 7.1M1
     */
    public String escape(String content, Syntax syntax)
    {
        if (content == null || syntax == null) {
            return null;
        }
        String input = String.valueOf(content);

        // Determine the escape character for the syntax.
        char escapeChar;
        try {
            escapeChar = getEscapeCharacter(syntax);
        } catch (Exception e) {
            // We don`t know how to proceed, so we just return null.
            return null;
        }

        // Since we prefix all characters, the result size will be double the input's, so we can just use char[].
        char[] result = new char[input.length() * 2];

        // Escape the content.
        for (int i = 0; i < input.length(); i++) {
            result[2 * i] = escapeChar;
            result[2 * i + 1] = input.charAt(i);
        }

        return String.valueOf(result);
    }

    private char getEscapeCharacter(Syntax syntax) throws IllegalArgumentException
    {
        if (Syntax.XWIKI_1_0.equals(syntax)) {
            return '\\';
        } else if (Syntax.XWIKI_2_0.equals(syntax) || Syntax.XWIKI_2_1.equals(syntax)) {
            return '~';
        }

        throw new IllegalArgumentException(String.format("Escaping is not supported for Syntax [%s]", syntax));
    }
}
