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
package org.xwiki.platform.security.requiredrights.internal;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.DefinitionDescriptionBlock;
import org.xwiki.rendering.block.DefinitionListBlock;
import org.xwiki.rendering.block.DefinitionTermBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.RenderingContext;

/**
 * Provider that produces a displayer for a macro.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component(roles = MacroDisplayerProvider.class)
@Singleton
public class MacroDisplayerProvider
{
    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    @Named("plain/1.0")
    private Parser plainParser;

    @Inject
    private MacroManager macroManager;

    @Inject
    private RenderingContext renderingContext;

    @Inject
    private MacroContentParser macroContentParser;

    /**
     * @param macroBlock the macro block to display
     * @return a supplier that displays the macro block
     */
    public Supplier<Block> get(MacroBlock macroBlock)
    {
        String macroId = macroBlock.getId();
        String translationKeyPrefix = "rendering.macro." + macroId;

        // Generated structure: dl > dt > parameter name, dd > parameter value
        List<Block> properties = new ArrayList<>();

        Map<String, ParameterDescriptor> parameterDescriptors = Map.of();
        try {
            MacroId macroIdWithSyntax = new MacroId(macroId,
                this.macroContentParser.getCurrentSyntax(getTransformationContext(macroBlock)));
            parameterDescriptors =
                this.macroManager.getMacro(macroIdWithSyntax).getDescriptor().getParameterDescriptorMap();
        } catch (MacroLookupException e) {
            // Ignore, just work without the macro descriptor.
        }

        for (Map.Entry<String, String> entry : macroBlock.getParameters().entrySet()) {
            String parameterName = entry.getKey();
            String parameterValue = entry.getValue();

            String fallbackName;
            if (parameterDescriptors.containsKey(parameterName)) {
                fallbackName = parameterDescriptors.get(parameterName).getName();
            } else {
                fallbackName = parameterName;
            }

            String translationKey = translationKeyPrefix + ".parameter." + parameterName + ".name";
            Block parameterNameBlock = maybeTranslate(translationKey, fallbackName);
            Block parameterValueBlock = getCodeBlock(parameterValue);
            properties.add(new DefinitionTermBlock(List.of(parameterNameBlock)));
            properties.add(new DefinitionDescriptionBlock(List.of(parameterValueBlock)));
        }

        Block contentName = maybeTranslate("rendering.macroContent", "Content");
        Block contentValue = getCodeBlock(macroBlock.getContent());
        properties.add(new DefinitionTermBlock(List.of(contentName)));
        properties.add(new DefinitionDescriptionBlock(List.of(contentValue)));

        return () -> new DefinitionListBlock(properties);
    }

    private Block maybeTranslate(String key, String fallback)
    {
        Translation translation = this.localizationManager.getTranslation(key);

        if (translation != null) {
            return translation.render();
        } else {
            return new WordBlock(fallback);
        }
    }

    private Block getCodeBlock(String value)
    {
        if (StringUtils.isNotBlank(value)) {
            try {
                return new GroupBlock(
                    List.of(this.plainParser.parse(new StringReader(value))),
                    Map.of("class", "code box")
                );
            } catch (ParseException e) {
                // Ignore, shouldn't happen
            }
        }
        return new CompositeBlock();
    }

    private MacroTransformationContext getTransformationContext(MacroBlock macroBlock)
    {
        MacroTransformationContext macroTransformationContext = new MacroTransformationContext();
        macroTransformationContext.setId("RequiredRightAnalyzer_" + macroBlock.getId());
        macroTransformationContext.setCurrentMacroBlock(macroBlock);
        // fallback syntax: macro content parser search by default for the XDOM syntax.
        macroTransformationContext.setSyntax(this.renderingContext.getDefaultSyntax());
        macroTransformationContext.setInline(macroBlock.isInline());
        return macroTransformationContext;
    }

}
