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
package org.xwiki.platform.security.requiredrights.internal.display;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.DefinitionDescriptionBlock;
import org.xwiki.rendering.block.DefinitionListBlock;
import org.xwiki.rendering.block.DefinitionTermBlock;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.util.ParserUtils;

/**
 * Abstract supplier provider that provides some common functionality.
 *
 * @param <T> the type of the object
 * @version $Id$
 * @since 15.9RC1
 */
public abstract class AbstractBlockSupplierProvider<T> implements BlockSupplierProvider<T>
{
    protected static final String CLASS_ATTRIBUTE = "class";

    @Inject
    @Named("plain/1.0")
    private Parser plainTextParser;

    private final ParserUtils parserUtils = new ParserUtils();

    protected Block renderProperties(List<PropertyDisplay> propertyNamesHintsValues)
    {
        List<Block> propertyBlocks = propertyNamesHintsValues.stream()
            .flatMap(this::renderProperty)
            .collect(Collectors.toList());
        // Add the xform class such that hints work.
        return new DefinitionListBlock(propertyBlocks, Map.of(CLASS_ATTRIBUTE, "xform"));
    }

    protected Block getCodeBlock(String value)
    {
        if (StringUtils.isNotBlank(value)) {
            return new GroupBlock(List.of(getStringBlock(value)), Map.of(CLASS_ATTRIBUTE, "code box"));
        } else {
            return new CompositeBlock();
        }
    }

    private Stream<Block> renderProperty(PropertyDisplay nameHintValue)
    {
        Block nameBlock;

        Block nameStringBlock = getStringBlock(nameHintValue.getName());

        if (StringUtils.isNotBlank(nameHintValue.getHint())) {
            FormatBlock hintBlock = new FormatBlock(List.of(getStringBlock(nameHintValue.getHint())),
                Format.NONE, Map.of(CLASS_ATTRIBUTE, "xHint"));
            nameBlock = new DefinitionTermBlock(List.of(nameStringBlock, hintBlock));
        } else {
            nameBlock = new DefinitionTermBlock(List.of(nameStringBlock));
        }

        Block valueBlock;
        if (nameHintValue.isHtmlValue()) {
            valueBlock = new DefinitionDescriptionBlock(List.of(new RawBlock(nameHintValue.getValue(),
                Syntax.HTML_5_0)));
        } else {
            valueBlock = new DefinitionDescriptionBlock(List.of(getCodeBlock(nameHintValue.getValue())));
        }

        return Stream.of(nameBlock, valueBlock);
    }

    protected Block getStringBlock(String value)
    {
        if (StringUtils.isNotBlank(value)) {
            try {
                return this.parserUtils.convertToInline(this.plainTextParser.parse(new StringReader(value)),
                    false);
            } catch (ParseException e) {
                // Ignore, shouldn't happen
            }
        }
        return new CompositeBlock();
    }

    protected static class PropertyDisplay
    {
        private final String name;

        private final String hint;

        private final String value;

        private final boolean htmlValue;

        PropertyDisplay(String name, String hint, String value, boolean htmlValue)
        {
            this.name = name;
            this.hint = hint;
            this.value = value;
            this.htmlValue = htmlValue;
        }

        public String getName()
        {
            return this.name;
        }

        public String getHint()
        {
            return this.hint;
        }

        public String getValue()
        {
            return this.value;
        }

        public boolean isHtmlValue()
        {
            return this.htmlValue;
        }
    }
}
