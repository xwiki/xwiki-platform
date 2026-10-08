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
package org.xwiki.blocknote.internal;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.blocknote.BlockNoteMacroConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Converts the code macro calls to BlockNote code blocks, and back. The code blocks that were produced from verbatim
 * blocks are not converted back to code macro calls.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Component
@Named(CodeMacroBlockNoteConverter.CODE_MACRO)
@Singleton
public class CodeMacroBlockNoteConverter implements BlockNoteMacroConverter
{
    /**
     * The id of the code macro.
     */
    public static final String CODE_MACRO = "code";

    private static final String TYPE = "type";

    private static final String MACRO_BLOCK = "xwikiMacroBlock";

    private static final String PROPS = "props";

    private static final String CONTENT = "content";

    private static final String TEXT = "text";

    private static final String CALL = "call";

    private static final String PARAMETERS = "parameters";

    private static final String XWIKI_PARAMETERS = "xwikiParameters";

    private static final String LANGUAGE = "language";

    /**
     * The code macro parameter that specifies where to take the code from, instead of the macro content.
     */
    private static final String SOURCE = "source";

    /**
     * The default language of the BlockNote code blocks. Code blocks with this language are saved as code macro calls
     * without language, which means the code macro detects the language itself.
     */
    private static final String DEFAULT_LANGUAGE = TEXT;

    /**
     * The code block property that indicates that the code block was produced from a verbatim block.
     */
    private static final String VERBATIM = "xwikiVerbatim";

    @Override
    public String getBlockType()
    {
        return "codeBlock";
    }

    @Override
    public Optional<ObjectNode> toBlock(ObjectNode macroBlock)
    {
        if (!canConvert(macroBlock)) {
            return Optional.empty();
        }

        JsonNode call = macroBlock.path(PROPS).path(CALL);
        ObjectNode codeBlock = JsonNodeFactory.instance.objectNode();
        codeBlock.put(TYPE, getBlockType());
        codeBlock.set(PROPS, getCodeBlockProperties(call.path(PARAMETERS)));
        codeBlock.put(CONTENT, call.path(CONTENT).asText(""));

        return Optional.of(codeBlock);
    }

    private boolean canConvert(JsonNode macroBlock)
    {
        JsonNode call = macroBlock.path(PROPS).path(CALL);
        // Code blocks are block-level so we can't convert inline code macro calls. We also don't convert the code macro
        // calls that take the code from a different source than the macro content, because the code block would be
        // empty (and its content ignored on save). The code macro content is plain text, unless it is edited in-place
        // (which is not the case currently).
        return MACRO_BLOCK.equals(macroBlock.path(TYPE).asText()) && !hasParameter(call.path(PARAMETERS), SOURCE)
            && !call.path(CONTENT).isContainerNode();
    }

    private ObjectNode getCodeBlockProperties(JsonNode parameters)
    {
        ObjectNode props = JsonNodeFactory.instance.objectNode();
        ObjectNode xwikiParameters = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, JsonNode> parameter : parameters.properties()) {
            if (LANGUAGE.equalsIgnoreCase(parameter.getKey()) && parameter.getValue().isTextual()
                && !DEFAULT_LANGUAGE.equals(parameter.getValue().asText())) {
                props.put(LANGUAGE, parameter.getValue().asText());
            } else {
                // Includes the default language, which would otherwise be lost on save because BlockNote doesn't save
                // the default property values.
                xwikiParameters.set(parameter.getKey(), parameter.getValue());
            }
        }
        if (!xwikiParameters.isEmpty()) {
            props.set(XWIKI_PARAMETERS, xwikiParameters);
        }
        return props;
    }

    @Override
    public Optional<ObjectNode> toMacro(ObjectNode block)
    {
        JsonNode props = block.path(PROPS);
        if (props.path(VERBATIM).asBoolean()) {
            return Optional.empty();
        }

        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        JsonNode language = props.path(LANGUAGE);
        boolean hasLanguage = language.isTextual() && !DEFAULT_LANGUAGE.equals(language.asText());
        if (hasLanguage) {
            // The language is usually the first parameter of the code macro.
            parameters.set(LANGUAGE, language);
        }
        for (Map.Entry<String, JsonNode> parameter : props.path(XWIKI_PARAMETERS).properties()) {
            // The language selected in the code block overwrites the language parameter.
            if (!hasLanguage || !LANGUAGE.equalsIgnoreCase(parameter.getKey())) {
                parameters.set(parameter.getKey(), parameter.getValue());
            }
        }

        ObjectNode macroBlock = JsonNodeFactory.instance.objectNode();
        macroBlock.put(TYPE, MACRO_BLOCK);
        ObjectNode call = macroBlock.putObject(PROPS).putObject(CALL);
        call.put("name", CODE_MACRO);
        call.set(PARAMETERS, parameters);
        call.put(CONTENT, getTextContent(block.path(CONTENT)));

        return Optional.of(macroBlock);
    }

    private boolean hasParameter(JsonNode parameters, String name)
    {
        return parameters.properties().stream().anyMatch(parameter -> name.equalsIgnoreCase(parameter.getKey()));
    }

    private String getTextContent(JsonNode content)
    {
        if (content.isArray()) {
            StringBuilder text = new StringBuilder();
            for (JsonNode item : content) {
                text.append(item.isTextual() ? item.asText() : item.path(TEXT).asText());
            }
            return text.toString();
        }
        return content.asText("");
    }
}
