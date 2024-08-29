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
package org.xwiki.localization.macro.internal;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.localization.macro.TranslationMacroParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroPreparationException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.util.ParserUtils;

/**
 * Display a translation message.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("translation")
@Singleton
public class TranslationMacro extends AbstractMacro<TranslationMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Display a translation message.";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "the default translation message";

    /**
     * Used to make blocks inline.
     */
    private static final ParserUtils PARSERUTILS = new ParserUtils();

    /**
     * Used to find the translation.
     */
    @Inject
    private LocalizationManager localization;

    /**
     * Used to get the current Locale.
     */
    @Inject
    private LocalizationContext localizationContext;

    /**
     * Used to parse the translation macro content.
     */
    @Inject
    private MacroContentParser macroContentParser;

    /**
     * Used to parse text.
     */
    @Inject
    @Named("plain/1.0")
    private Parser plainParser;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public TranslationMacro()
    {
        super("Translation", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION),
            TranslationMacroParameters.class);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_CONTENT));
    }

    @Override
    public List<Block> execute(TranslationMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        Locale locale =
            parameters.getLocale() != null ? parameters.getLocale() : this.localizationContext.getCurrentLocale();

        Translation translation = this.localization.getTranslation(parameters.getKey(), locale);

        List<Block> blocks;
        if (translation != null) {
            Block block =
                parameters.getParameters() != null ? translation.render(locale, (Object[]) parameters.getParameters())
                    : translation.render(locale);

            if (block instanceof CompositeBlock) {
                blocks = block.getChildren();
            } else {
                blocks = Arrays.asList(block);
            }

            if (!context.getCurrentMacroBlock().isInline()) {
                // Make the content standalone
                blocks = Arrays.<Block> asList(new GroupBlock(blocks));
            }
        } else if (content != null) {
            blocks =
                this.macroContentParser.parse(content, context, false, context.getCurrentMacroBlock().isInline())
                    .getChildren();
        } else {
            try {
                blocks = this.plainParser.parse(new StringReader(parameters.getKey())).getChildren();

                if (context.getCurrentMacroBlock().isInline()) {
                    PARSERUTILS.removeTopLevelParagraph(blocks);
                }
            } catch (ParseException e) {
                throw new MacroExecutionException("Failed to parse key [" + parameters.getKey() + "]", e);
            }
        }

        return blocks;
    }

    @Override
    public void prepare(MacroBlock macroBlock) throws MacroPreparationException
    {
        this.macroContentParser.prepareContentWiki(macroBlock);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }
}
