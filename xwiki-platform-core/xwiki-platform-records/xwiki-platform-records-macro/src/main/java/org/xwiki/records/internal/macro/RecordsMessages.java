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
package org.xwiki.records.internal.macro;

import java.io.StringReader;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Builds the messages the Records macro shows to authors and readers.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Component(roles = RecordsMessages.class)
@Singleton
public class RecordsMessages
{
    /**
     * The translation key of the message shown when the table cannot be rendered.
     */
    static final String RENDER_FAILED = "error.renderFailed";

    /**
     * The prefix of the translation keys of the messages the macro displays.
     */
    private static final String MESSAGE_PREFIX = "rendering.macro.records.";

    /**
     * Translates the messages shown to authors and readers.
     */
    @Inject
    private ContextualLocalizationManager localization;

    /**
     * Parses a translated message as plain text, so that nothing in it, such as a field name taken from the wiki, is
     * read as wiki syntax.
     */
    @Inject
    @Named("plain/1.0")
    private Parser plainParser;

    /**
     * Renders the plain text blocks back to XWiki syntax, which escapes whatever the message macros would otherwise
     * interpret when they parse their content.
     */
    @Inject
    @Named("xwiki/2.1")
    private BlockRenderer xwikiRenderer;

    /**
     * @param key the translation key of the message, without the macro prefix
     * @param arguments the arguments of the message
     * @return the message, as plain text
     */
    public String translate(String key, Object... arguments)
    {
        return this.localization.getTranslationPlain(MESSAGE_PREFIX + key, arguments);
    }

    /**
     * @param key the translation key of the message, without the macro prefix
     * @param arguments the arguments of the message
     * @return a call to the {@code warning} macro showing the message
     * @throws MacroExecutionException when the message cannot be escaped
     */
    public Block warning(String key, Object... arguments) throws MacroExecutionException
    {
        return message("warning", key, arguments);
    }

    /**
     * @param key the translation key of the message, without the macro prefix
     * @param arguments the arguments of the message
     * @return a call to the {@code error} macro showing the message
     * @throws MacroExecutionException when the message cannot be escaped
     */
    public Block error(String key, Object... arguments) throws MacroExecutionException
    {
        return message("error", key, arguments);
    }

    /**
     * Builds a call to a message macro, so that the message looks and is announced like a {@code {{warning}}} or an
     * {@code {{error}}}, icon and accessible name included. The call is left for the macro transformation to execute,
     * which it does for the blocks a macro returns.
     *
     * @param macroId the identifier of the message macro, {@code warning} or {@code error}
     * @param key the translation key of the message
     * @param arguments the arguments of the message
     * @return the macro call
     * @throws MacroExecutionException when the message cannot be escaped
     */
    private Block message(String macroId, String key, Object... arguments) throws MacroExecutionException
    {
        return new MacroBlock(macroId, Map.of(), escape(translate(key, arguments)), false);
    }

    /**
     * The message macros parse their content as wiki syntax, while the messages embed names that come from the wiki, so
     * the text goes through a plain text parser and back out as XWiki syntax, which escapes it.
     *
     * @param text the plain text to escape
     * @return the text as XWiki syntax content that renders as the text itself
     * @throws MacroExecutionException when the text cannot be parsed
     */
    private String escape(String text) throws MacroExecutionException
    {
        try {
            WikiPrinter printer = new DefaultWikiPrinter();
            this.xwikiRenderer.render(this.plainParser.parse(new StringReader(text)), printer);
            return printer.toString();
        } catch (ParseException e) {
            throw new MacroExecutionException(translate(RENDER_FAILED), e);
        }
    }
}
