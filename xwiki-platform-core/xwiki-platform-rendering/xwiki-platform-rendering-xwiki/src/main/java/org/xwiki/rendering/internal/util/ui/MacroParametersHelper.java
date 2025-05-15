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
package org.xwiki.rendering.internal.util.ui;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.displayer.HTMLDisplayerException;
import org.xwiki.displayer.HTMLDisplayerManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.properties.PropertyGroupDescriptor;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.velocity.tools.EscapeTool;

@Component(roles = { MacroParametersHelper.class })
@Singleton
public class MacroParametersHelper
{
    private static final String BOOLEAN_TEMPLATE_FALLBACK = """
            <input type="checkbox" name="%1$s" value="true"/>
            <input type="hidden" name="%1$s" value="false"/>
        """;

    private static final String ENUM_SELECT_TEMPLATE_FALLBACK = """
            <select name="%s">%s</select>
        """;

    private static final String ENUM_OPTION_TEMPLATE_FALLBACK = """
            <option value="%s">%s</option>
        """;

    private static final String TEXT_TEMPLATE_FALLBACK = """
        <input type="text" name="%s" />
        """;

    private static final String CONTENT_TEMPLATE = """
        <textarea name="$content" rows="7"></textarea>
        """;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private HTMLDisplayerManager htmlDisplayerManager;

    private EscapeTool escapeTool = new EscapeTool();

    // FIXME: introduce a cache?
    public MacroDescriptorUI buildMacroDescriptorUI(MacroDescriptor macroDescriptor)
    {
        String macroTranslationKey = "rendering.macro." + macroDescriptor.getId();
        MacroDescriptorUI result = new MacroDescriptorUI(macroDescriptor.getId().getId())
            .setName(getParameterTranslation(macroTranslationKey + ".name", macroDescriptor.getName()))
            .setDescription(getParameterTranslation(macroTranslationKey + ".description",
                macroDescriptor.getDescription()))
            .setSupportsInlineMode(macroDescriptor.supportsInlineMode());

        Collection<ParameterDescriptor> parameterDescriptors = macroDescriptor.getParameterDescriptorMap().values();
        Map<String, MacroParameterUINode> parametersMap = new HashMap<>();
        List<MacroParameterUINode> mandatoryParameters = new ArrayList<>();
        List<MacroParameterUINode> optionalParameters = new ArrayList<>();

        ContentDescriptor contentDescriptor = macroDescriptor.getContentDescriptor();
        if (contentDescriptor != null) {
            MacroParameterUINode contentNode =
                new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "$content")
                    .setName(getParameterTranslation("rendering.macroContent", "Content"))
                    .setDescription(getParameterTranslation(macroTranslationKey + "content.description",
                        contentDescriptor.getDescription()))
                    .setMandatory(contentDescriptor.isMandatory())
                    .setOrder(contentDescriptor.getOrder())
                    .setDisplayType(contentDescriptor.getType().getTypeName())
                    .setEditTemplate(CONTENT_TEMPLATE);
            if (contentNode.isMandatory()) {
                mandatoryParameters.add(contentNode);
            } else {
                optionalParameters.add(contentNode);
            }
            parametersMap.put(contentNode.getKey(), contentNode);
        }

        MacroParameterUINode defaultOptionalGroup = new MacroParameterUINode(MacroParameterUINodeType.GROUP, "default")
            .setName(this.localizationManager.getTranslationPlain("rendering.macro.config.defaultOptionalGroup"));
        optionalParameters.add(defaultOptionalGroup);

        Map<String, MacroParameterUINode> groupMap = new HashMap<>();
        Map<String, MacroParameterUINode> featureMap = new HashMap<>();

        for (ParameterDescriptor parameterDescriptor : parameterDescriptors) {
            String parameterTranslationKey =
                String.format("%s.parameter.%s", macroTranslationKey, parameterDescriptor.getId());
            boolean mandatory = parameterDescriptor.isMandatory();
            MacroParameterUINode node = new MacroParameterUINode(MacroParameterUINodeType.PARAMETER,
                parameterDescriptor.getId());

            parametersMap.put(node.getKey(), node);
            node.setName(getParameterTranslation(parameterTranslationKey + ".name", parameterDescriptor.getName()))
                .setDescription(getParameterTranslation(parameterTranslationKey + ".description",
                    parameterDescriptor.getDescription()))
                .setMandatory(mandatory)
                .setHidden(parameterDescriptor.isDisplayHidden())
                .setAdvanced(parameterDescriptor.isAdvanced())
                .setDeprecated(parameterDescriptor.isDeprecated())
                .setOrder(parameterDescriptor.getOrder())
                .setDisplayType(parameterDescriptor.getDisplayType().getTypeName())
                .setDefaultValue(parameterDescriptor.getDefaultValue())
                .setEditTemplate(getEditTemplate(parameterDescriptor, parameterTranslationKey))
                .setCaseInsensitive(parameterDescriptor.getDisplayType() instanceof Enum);

            PropertyGroupDescriptor groupDescriptor = parameterDescriptor.getGroupDescriptor();
            if (groupDescriptor != null
                && !(StringUtils.isEmpty(groupDescriptor.getFeature())
                && (groupDescriptor.getGroup() == null || groupDescriptor.getGroup().isEmpty()))) {
                boolean mandatoryGroup = mandatory || groupDescriptor.isFeatureMandatory();
                MacroParameterUINode groupNode;
                String feature = groupDescriptor.getFeature();
                if (!StringUtils.isEmpty(feature)) {
                    groupNode = featureMap.computeIfAbsent(feature, key ->
                        createGroupNode(true, key, mandatoryGroup, mandatoryParameters, optionalParameters));
                } else {
                    String groupName = StringUtils.join(groupDescriptor.getGroup(), ",");
                    groupNode = groupMap.computeIfAbsent(groupName, key ->
                        createGroupNode(false, key, mandatoryGroup, mandatoryParameters, optionalParameters));
                }
                parametersMap.put(groupNode.getKey(), groupNode);
                groupNode.addChild(node);
                if (node.getOrder() > -1 && (groupNode.getOrder() == -1 || node.getOrder() < groupNode.getOrder())) {
                    groupNode.setOrder(node.getOrder());
                }
            } else if (node.isMandatory()) {
                mandatoryParameters.add(node);
            } else {
                defaultOptionalGroup.addChild(node);
            }
        }

        if (defaultOptionalGroup.getChildren().isEmpty()) {
            optionalParameters.remove(defaultOptionalGroup);
        } else {
            // default group should always have priority.
            defaultOptionalGroup.setOrder(0);
            parametersMap.put(defaultOptionalGroup.getKey(), defaultOptionalGroup);
        }
        MacroParameterUINodeComparator comparator = new MacroParameterUINodeComparator();
        mandatoryParameters.sort(comparator);
        optionalParameters.sort(comparator);

        result.setMandatoryNodes(mandatoryParameters.stream().map(MacroParameterUINode::getKey).toList())
            .setOptionalNodes(optionalParameters.stream().map(MacroParameterUINode::getKey).toList())
            .setParametersMap(parametersMap);

        return result;
    }

    private String getParameterTranslation(String translationKey, String fallback)
    {
        String translationPlain = this.localizationManager.getTranslationPlain(translationKey);
        if (translationKey.equals(translationPlain)) {
            translationPlain = fallback;
        }
        return translationPlain;
    }

    private MacroParameterUINode createGroupNode(boolean isFeature, String key, boolean mandatoryGroup,
        List<MacroParameterUINode> mandatoryParameters, List<MacroParameterUINode> optionalParameters)
    {
        MacroParameterUINodeType type = (isFeature) ? MacroParameterUINodeType.FEATURE :
            MacroParameterUINodeType.GROUP;
        MacroParameterUINode groupNode = new MacroParameterUINode(type, key);
        groupNode.setName(this.localizationManager.getTranslationPlain(key));
        if (mandatoryGroup) {
            groupNode.setMandatory(true);
            mandatoryParameters.add(groupNode);
        } else {
            optionalParameters.add(groupNode);
        }
        return groupNode;
    }

    private String getEditTemplate(ParameterDescriptor parameterDescriptor, String parameterTranslationKey)
    {
        String result = "";
        try {
            Map<String, String> displayerMap = new LinkedHashMap<>();
            displayerMap.put("name", parameterDescriptor.getId());
            PropertyGroupDescriptor groupDescriptor = parameterDescriptor.getGroupDescriptor();
            if (groupDescriptor != null
                && groupDescriptor.getGroup() != null
                && !groupDescriptor.getGroup().isEmpty()) {
                // TODO: remove once a decision is taken in
                //  https://forum.xwiki.org/t/removal-of-data-property-group-mechansim/16976
                displayerMap.put("data-property-group", StringUtils.join(groupDescriptor.getGroup(), "/"));
            }
            result = this.htmlDisplayerManager.display(parameterDescriptor.getDisplayType(),
                parameterDescriptor.getDefaultValue(), displayerMap, "edit");
        } catch (HTMLDisplayerException e) {
            throw new RuntimeException(e);
        }
        if (StringUtils.isEmpty(result)) {
            result = getEditTemplateFallback(parameterDescriptor, parameterTranslationKey);
        }
        return result;
    }

    private String getEditTemplateFallback(ParameterDescriptor parameterDescriptor, String parameterTranslationKey)
    {
        Type parameterType = parameterDescriptor.getDisplayType();
        String result;
        if (parameterType == Boolean.class) {
            result = String.format(BOOLEAN_TEMPLATE_FALLBACK, escapeTool.xml(parameterDescriptor.getId()));
        } else if (parameterType instanceof Enum enumType) {
            StringBuilder options = new StringBuilder();
            for (Object enumConstant : enumType.getDeclaringClass().getEnumConstants()) {
                String name = ((Enum) enumConstant).name();
                String label = getParameterTranslation(parameterTranslationKey + ".value." + name, name);
                options.append(String.format(ENUM_OPTION_TEMPLATE_FALLBACK, escapeTool.xml(name),
                    escapeTool.xml(label)));
            }
            result = String.format(ENUM_SELECT_TEMPLATE_FALLBACK,
                escapeTool.xml(parameterDescriptor.getId()),
                options);
        } else {
            result = String.format(TEXT_TEMPLATE_FALLBACK, escapeTool.xml(parameterDescriptor.getId()));
        }
        return result;
    }
}
