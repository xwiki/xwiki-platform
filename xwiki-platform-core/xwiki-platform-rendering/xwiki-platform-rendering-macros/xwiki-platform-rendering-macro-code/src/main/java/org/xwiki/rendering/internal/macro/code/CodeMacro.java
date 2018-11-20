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
package org.xwiki.rendering.internal.macro.code;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.box.AbstractBoxMacro;
import org.xwiki.rendering.macro.code.CodeMacroParameters;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.parser.HighlightParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Highlight provided content depending of the content syntax.
 * 
 * @version $Id$
 * @since 1.7RC1
 */
@Component
@Named("code")
@Singleton
public class CodeMacro extends AbstractBoxMacro<CodeMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Highlights code snippets of various programming languages";

    /**
     * Used to indicate that content should not be highlighted.
     */
    private static final String LANGUAGE_NONE = "none";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "the content to highlight";

    /**
     * Used to parse content when language="none".
     */
    @Inject
    @Named("plain/1.0")
    private Parser plainTextParser;

    /**
     * Used to lookup highlight parsers.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public CodeMacro()
    {
        super("Code", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION, false), CodeMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_FORMATTING);
    }

    @Override
    protected List<Block> parseContent(CodeMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        List<Block> result;
        try {
            if (LANGUAGE_NONE.equalsIgnoreCase(parameters.getLanguage())) {
                if (StringUtils.isEmpty(content)) {
                    result = Collections.emptyList();
                } else {
                    result = this.plainTextParser.parse(new StringReader(content)).getChildren().get(0).getChildren();
                }
            } else {
                result = highlight(parameters, content);
            }
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to highlight content", e);
        }

        Map<String, String> formatParameters = new LinkedHashMap<String, String>();
        formatParameters.put("class", "code");

        if (context.isInline()) {
            result = Arrays.<Block> asList(new FormatBlock(result, Format.NONE, formatParameters));
        } else {
            result = Arrays.<Block> asList(new GroupBlock(result, formatParameters));
        }

        return result;
    }

    /**
     * Return a highlighted version of the provided content.
     * 
     * @param parameters the code macro parameters.
     * @param content the content to highlight.
     * @return the highlighted version of the provided content.
     * @throws ParseException the highlight parser failed.
     * @throws ComponentLookupException failed to find highlight parser for provided language.
     */
    protected List<Block> highlight(CodeMacroParameters parameters, String content) throws ParseException,
        ComponentLookupException
    {
        HighlightParser parser;

        if (parameters.getLanguage() != null) {
            if (this.componentManager.hasComponent(HighlightParser.class, parameters.getLanguage())) {
                try {
                    parser = this.componentManager.getInstance(HighlightParser.class, parameters.getLanguage());
                    return parser.highlight(parameters.getLanguage(), new StringReader(content));
                } catch (ComponentLookupException e) {
                    this.logger.error("Faild to load highlighting parser for language [{}]", parameters.getLanguage(),
                        e);
                }
            }
        }

        this.logger.debug(
            "Can't find any specific highlighting parser for language [{}]. Trying the default highlighting parser.",
            parameters.getLanguage());

        parser = this.componentManager.getInstance(HighlightParser.class, "default");

        return parser.highlight(parameters.getLanguage(), new StringReader(content));
    }
}
