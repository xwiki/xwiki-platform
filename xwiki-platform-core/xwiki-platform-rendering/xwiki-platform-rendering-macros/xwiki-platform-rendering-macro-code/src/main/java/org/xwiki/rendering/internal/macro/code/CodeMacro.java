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
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.internal.code.layout.CodeLayoutHandler;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.box.AbstractBoxMacro;
import org.xwiki.rendering.macro.code.CodeMacroParameters;
import org.xwiki.rendering.macro.code.source.CodeMacroSource;
import org.xwiki.rendering.macro.code.source.CodeMacroSourceFactory;
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
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private CodeMacroSourceFactory sourceFactory;

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
        setDefaultCategories(Set.of(DEFAULT_CATEGORY_FORMATTING));
    }

    @Override
    protected boolean isContentChecked()
    {
        return false;
    }

    private CodeMacroSource getContent(CodeMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        if (parameters.getSource() != null) {
            return this.sourceFactory.getContent(parameters.getSource(), context);
        }

        return content != null ? new CodeMacroSource(null, content, null) : null;
    }

    @Override
    protected List<Block> parseContent(CodeMacroParameters parameters, String inputContent,
        MacroTransformationContext context) throws MacroExecutionException
    {
        CodeMacroSource source = getContent(parameters, inputContent, context);

        if (source == null) {
            return null;
        }

        List<Block> result;
        try {
            if (LANGUAGE_NONE.equalsIgnoreCase(parameters.getLanguage())) {
                if (StringUtils.isEmpty(source.getContent())) {
                    result = Collections.emptyList();
                } else {
                    result = this.plainTextParser.parse(new StringReader(source.getContent())).getChildren().get(0)
                        .getChildren();
                }
            } else {
                result = highlight(parameters, source);
            }
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to highlight content", e);
        }

        Map<String, String> formatParameters = new LinkedHashMap<>();
        formatParameters.put("class", "code");

        if (context.isInline()) {
            result = Arrays.asList(new FormatBlock(result, Format.NONE, formatParameters));
        } else {
            try {
                CodeLayoutHandler layoutHandler = this.componentManagerProvider.get()
                    .getInstance(CodeLayoutHandler.class, parameters.getLayout().getHint());
                result = layoutHandler.layout(result, source.getContent());
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to load code layout handler for layout type [{}], no layout will be applied",
                    parameters.getLayout().name(), e);
            }
            result = Arrays.asList(new GroupBlock(result, formatParameters));
        }

        return result;
    }

    /**
     * Return a highlighted version of the provided content.
     * 
     * @param parameters the code macro parameters.
     * @param source the source to highlight.
     * @return the highlighted version of the provided content.
     * @throws ParseException the highlight parser failed.
     * @throws ComponentLookupException failed to find highlight parser for provided language.
     */
    private List<Block> highlight(CodeMacroParameters parameters, CodeMacroSource source)
        throws ParseException, ComponentLookupException
    {
        HighlightParser parser;

        String language = parameters.getLanguage();
        if (language == null) {
            language = source.getLanguage();
        }

        ComponentManager componentManager = this.componentManagerProvider.get();

        if (language != null) {
            if (componentManager.hasComponent(HighlightParser.class, language)) {
                try {
                    parser = componentManager.getInstance(HighlightParser.class, language);
                    return parser.highlight(language, new StringReader(source.getContent()));
                } catch (ComponentLookupException e) {
                    this.logger.error("Faild to load highlighting parser for language [{}]", language, e);
                }
            }
        }

        this.logger.debug(
            "Can't find any specific highlighting parser for language [{}]. Trying the default highlighting parser.",
            language);

        parser = componentManager.getInstance(HighlightParser.class, "default");

        return parser.highlight(language, new StringReader(source.getContent()));
    }
}
