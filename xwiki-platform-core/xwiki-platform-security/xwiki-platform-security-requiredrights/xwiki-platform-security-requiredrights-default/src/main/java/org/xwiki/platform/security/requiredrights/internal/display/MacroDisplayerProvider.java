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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.RenderingContext;

/**
 * Provider that produces a displayer for a macro.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
public class MacroDisplayerProvider extends AbstractBlockSupplierProvider<MacroBlock>
{
    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private MacroManager macroManager;

    @Inject
    private RenderingContext renderingContext;

    @Inject
    private MacroContentParser macroContentParser;

    @Override
    public Supplier<Block> get(MacroBlock macroBlock, Object... parameters)
    {
        String macroId = macroBlock.getId();
        String translationKeyPrefix = "rendering.macro." + macroId;

        Map<String, ParameterDescriptor> parameterDescriptors = Map.of();
        ContentDescriptor contentDescriptor = null;
        try {
            MacroId macroIdWithSyntax = new MacroId(macroId,
                this.macroContentParser.getCurrentSyntax(getTransformationContext(macroBlock)));
            MacroDescriptor macroDescriptor = this.macroManager.getMacro(macroIdWithSyntax).getDescriptor();
            parameterDescriptors = macroDescriptor.getParameterDescriptorMap();
            contentDescriptor = macroDescriptor.getContentDescriptor();
        } catch (MacroLookupException e) {
            // Ignore, just work without the macro descriptor.
        }

        List<PropertyDisplay> properties = new ArrayList<>();

        for (Map.Entry<String, String> entry : macroBlock.getParameters().entrySet()) {
            String parameterName = entry.getKey();
            String parameterValue = entry.getValue();

            String parameterTranslationKey = translationKeyPrefix + ".parameter." + parameterName;
            ParameterDescriptor parameterDescriptor = parameterDescriptors.get(parameterName);
            String displayName = getParameterDisplayName(parameterName, parameterTranslationKey, parameterDescriptor);
            String description = getParameterDescription(parameterTranslationKey, parameterDescriptor);
            properties.add(new PropertyDisplay(displayName, description, parameterValue, false));
        }

        String contentLabel = maybeTranslate("rendering.macroContent", "Content");
        String fallbackContentDescription = contentDescriptor != null ? contentDescriptor.getDescription() : null;
        String contentDescriptionKey = translationKeyPrefix + ".content.description";
        String contentDescription = maybeTranslate(contentDescriptionKey, fallbackContentDescription);
        properties.add(new PropertyDisplay(contentLabel, contentDescription, macroBlock.getContent(), false));

        return () -> renderProperties(properties);
    }

    private String getParameterDescription(String parameterTranslationKey, ParameterDescriptor parameterDescriptor)
    {
        String fallbackDescription;
        if (parameterDescriptor != null) {
            fallbackDescription = parameterDescriptor.getDescription();
        } else {
            fallbackDescription = null;
        }

        return maybeTranslate(parameterTranslationKey + ".description", fallbackDescription);
    }

    private String getParameterDisplayName(String parameterName, String parameterTranslationKey,
        ParameterDescriptor parameterDescriptor)
    {
        String fallbackName;

        if (parameterDescriptor != null) {
            fallbackName = parameterDescriptor.getName();
        } else {
            fallbackName = parameterName;
        }

        return maybeTranslate(parameterTranslationKey + ".name", fallbackName);
    }

    private String maybeTranslate(String key, String fallback)
    {
        String translation = this.localizationManager.getTranslationPlain(key);

        if (translation != null) {
            return translation;
        } else {
            return fallback;
        }
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
