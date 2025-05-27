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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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

    private static final String DEFAULT_GROUP_OPTIONALS_ID = "defaultOptionalGroup";

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private HTMLDisplayerManager htmlDisplayerManager;

    private final EscapeTool escapeTool = new EscapeTool();

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
        Map<String, AbstractMacroParameterUINode> parametersMap = new HashMap<>();
        Map<String, SortedSet<AbstractMacroParameterUINode>> childrenMap = new HashMap<>();
        List<AbstractMacroParameterUINode> mandatoryParameters = new ArrayList<>();
        List<AbstractMacroParameterUINode> optionalParameters = new ArrayList<>();
        Map<String, MacroParameterUINodeGroup> groupMap = new HashMap<>();
        Map<String, List<MacroParameterUINodeGroup>> featureMap = new HashMap<>();

        ContentDescriptor contentDescriptor = macroDescriptor.getContentDescriptor();
        if (contentDescriptor != null) {
            MacroParameterUINodeParameter contentNode =
                new MacroParameterUINodeParameter("$content")
                    .setDisplayType(contentDescriptor.getType().getTypeName())
                    .setEditTemplate(CONTENT_TEMPLATE.trim())
                    .setName(getParameterTranslation("rendering.macroContent", "Content"))
                    .setDescription(getParameterTranslation(macroTranslationKey + "content.description",
                        contentDescriptor.getDescription()))
                    .setMandatory(contentDescriptor.isMandatory())
                    .setOrder(contentDescriptor.getOrder());
            if (contentNode.isMandatory()) {
                mandatoryParameters.add(contentNode);
            } else {
                optionalParameters.add(contentNode);
            }
            parametersMap.put(contentNode.getKey(), contentNode);
        }

        MacroParameterUINodeGroup defaultOptionalGroup =
            createOrGetGroup(DEFAULT_GROUP_OPTIONALS_ID, null, false, groupMap, featureMap,
                childrenMap, macroTranslationKey)
            .setName(getParameterTranslation("rendering.macro.config.defaultOptionalGroup.name",
                "Optional parameters"));

        for (ParameterDescriptor parameterDescriptor : parameterDescriptors) {
            String parameterTranslationKey =
                String.format("%s.parameter.%s", macroTranslationKey, parameterDescriptor.getId());
            boolean mandatory = parameterDescriptor.isMandatory();
            MacroParameterUINodeParameter node = new MacroParameterUINodeParameter(parameterDescriptor.getId());

            parametersMap.put(node.getKey(), node);
            node.setAdvanced(parameterDescriptor.isAdvanced())
                .setDeprecated(parameterDescriptor.isDeprecated())
                .setDisplayType(parameterDescriptor.getDisplayType().getTypeName())
                .setDefaultValue(parameterDescriptor.getDefaultValue())
                .setEditTemplate(getEditTemplate(parameterDescriptor, parameterTranslationKey))
                .setCaseInsensitive(parameterDescriptor.getDisplayType() instanceof Enum)
                .setName(getParameterTranslation(parameterTranslationKey + ".name", parameterDescriptor.getName()))
                .setDescription(getParameterTranslation(parameterTranslationKey + ".description",
                    parameterDescriptor.getDescription()))
                .setMandatory(mandatory)
                .setHidden(parameterDescriptor.isDisplayHidden())
                .setOrder(parameterDescriptor.getOrder());

            PropertyGroupDescriptor groupDescriptor = parameterDescriptor.getGroupDescriptor();
            if (groupDescriptor != null
                && !(StringUtils.isEmpty(groupDescriptor.getFeature())
                && (groupDescriptor.getGroup() == null || groupDescriptor.getGroup().isEmpty()))) {
                String featureName = groupDescriptor.getFeature();
                String groupName = StringUtils.join(groupDescriptor.getGroup(), ",");
                boolean featureOnly = false;

                if (groupDescriptor.getGroup() == null || groupDescriptor.getGroup().isEmpty()) {
                    groupName = featureName;
                    featureOnly = true;
                }

                MacroParameterUINodeGroup groupNode = createOrGetGroup(groupName, featureName, featureOnly,
                    groupMap, featureMap, childrenMap, macroTranslationKey);

                if ((mandatory || groupDescriptor.isFeatureMandatory()) && !groupNode.isMandatory()) {
                    groupNode.setMandatory(true);
                }

                childrenMap.get(groupNode.getKey()).add(node);
                if (node.getOrder() > -1 && (groupNode.getOrder() == -1 || node.getOrder() < groupNode.getOrder())) {
                    groupNode.setOrder(node.getOrder());
                }
            } else if (node.isMandatory()) {
                mandatoryParameters.add(node);
            } else {
                childrenMap.get(defaultOptionalGroup.getKey()).add(node);
            }
        }

        Set<String> belongToFeature = new HashSet<>();

        for (Map.Entry<String, List<MacroParameterUINodeGroup>> featureEntry : featureMap.entrySet()) {
            String featureName = featureEntry.getKey();
            List<MacroParameterUINodeGroup> featureGroups = featureEntry.getValue();
            MacroParameterUINodeGroup featureNode = createOrGetGroup(featureName, featureName, true, groupMap,
                featureMap, childrenMap, macroTranslationKey);
            childrenMap.get(featureNode.getKey()).addAll(featureGroups);
            featureNode.setMandatory(featureGroups.stream().anyMatch(MacroParameterUINodeGroup::isMandatory));
            featureNode.setOrder(featureGroups.stream()
                .map(MacroParameterUINodeGroup::getOrder)
                .filter(i -> i != -1)
                .min(Integer::compareTo)
                .orElse(-1));

            belongToFeature.addAll(featureGroups.stream().map(MacroParameterUINodeGroup::getKey).toList());
        }

        for (MacroParameterUINodeGroup groupNode : groupMap.values()) {
            SortedSet<AbstractMacroParameterUINode> children = childrenMap.get(groupNode.getKey());
            if (!children.isEmpty() && !belongToFeature.contains(groupNode.getKey())) {
                if (groupNode.isMandatory()) {
                    mandatoryParameters.add(groupNode);
                } else {
                    optionalParameters.add(groupNode);
                }
            }
            if (!children.isEmpty()) {
                groupNode.setChildren(children.stream().map(AbstractMacroParameterUINode::getKey).toList());
                parametersMap.put(groupNode.getKey(), groupNode);
            }
        }
        // force the priority to appear first.
        defaultOptionalGroup.setOrder(0);

        MacroParameterUINodeComparator comparator = new MacroParameterUINodeComparator();
        mandatoryParameters.sort(comparator);
        optionalParameters.sort(comparator);

        result.setMandatoryNodes(mandatoryParameters.stream().map(AbstractMacroParameterUINode::getKey).toList())
            .setOptionalNodes(optionalParameters.stream().map(AbstractMacroParameterUINode::getKey).toList())
            .setParametersMap(parametersMap);

        return result;
    }

    private MacroParameterUINodeGroup createOrGetGroup(String id,
        String featureName,
        boolean featureOnly,
        Map<String, MacroParameterUINodeGroup> groupMap,
        Map<String, List<MacroParameterUINodeGroup>> featureMap,
        Map<String, SortedSet<AbstractMacroParameterUINode>> childrenMap,
        String macroTranslationKey)
    {
        String mapId = (featureOnly) ? "FEATURE:" + id : id;
        return groupMap.computeIfAbsent(mapId, key -> {
            String groupNameKey =
                String.format("%s.%s.%s.name", macroTranslationKey, "group", id);
            boolean isFeature = !StringUtils.isEmpty(featureName);
            MacroParameterUINodeGroup group = new MacroParameterUINodeGroup(id)
                .setFeatureName(featureName)
                .setFeature(isFeature)
                .setFeatureOnly(featureOnly)
                .setName(getParameterTranslation(groupNameKey, id));
            childrenMap.put(group.getKey(), new TreeSet<>(new MacroParameterUINodeComparator()));
            if (isFeature && !featureOnly) {
                featureMap.computeIfAbsent(featureName,featureKey -> new ArrayList<>()).add(group);
            }
            return group;
        });
    }

    private String getParameterTranslation(String translationKey, String fallback)
    {
        String result = this.localizationManager.getTranslationPlain(translationKey);
        return (result != null) ? result : fallback;
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
        return result.trim();
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
