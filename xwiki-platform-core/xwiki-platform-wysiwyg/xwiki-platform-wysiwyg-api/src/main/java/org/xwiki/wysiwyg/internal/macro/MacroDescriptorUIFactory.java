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
package org.xwiki.wysiwyg.internal.macro;

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
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.displayer.HTMLDisplayerException;
import org.xwiki.displayer.HTMLDisplayerManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.properties.PropertyGroupDescriptor;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.velocity.tools.EscapeTool;
import org.xwiki.wysiwyg.WysiwygConfiguration;
import org.xwiki.wysiwyg.macro.AbstractMacroUINode;
import org.xwiki.wysiwyg.macro.MacroDescriptorUI;
import org.xwiki.wysiwyg.macro.MacroUINodeGroup;
import org.xwiki.wysiwyg.macro.MacroUINodeParameter;

/**
 * Factory in charge of building a {@link MacroDescriptorUI} based on a {@link MacroDescriptor}.
 *
 * @version $Id$
 * @since 17.5.0
 */
@Component(roles = { MacroDescriptorUIFactory.class })
@Singleton
public class MacroDescriptorUIFactory
{
    private static final String BOOLEAN_TEMPLATE_FALLBACK = """
        <input type="checkbox" name="%1$s" value="true"/>
        <input type="hidden" name="%1$s" value="false"/>
        """;

    private static final String ENUM_SELECT_TEMPLATE_FALLBACK = "<select name=\"%s\">%s</select>";

    private static final String ENUM_OPTION_TEMPLATE_FALLBACK = "<option value=\"%s\">%s</option>";

    private static final String TEXT_TEMPLATE_FALLBACK = "<input type=\"text\" name=\"%s\" />";

    private static final String CONTENT_TEMPLATE = "<textarea name=\"$content\" rows=\"7\"></textarea>";

    private static final String DEFAULT_GROUP_OPTIONALS_ID = "defaultOptionalGroup";

    private static final String DOT_NAME = ".name";
    private static final String DOT_DESCRIPTION = ".description";

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private WysiwygConfiguration wysiwygConfiguration;

    @Inject
    private HTMLDisplayerManager htmlDisplayerManager;

    @Inject
    private Logger logger;

    private final EscapeTool escapeTool = new EscapeTool();

    // FIXME: introduce a cache?

    /**
     * Build a {@link MacroDescriptorUI} based on all information provided by the {@link MacroDescriptor}.
     *
     * @param macroDescriptor the descriptor of the macro to configure in the UI.
     * @return an instance of {@link MacroDescriptorUI} matching the given descriptor
     */
    public MacroDescriptorUI buildMacroDescriptorUI(MacroDescriptor macroDescriptor)
    {
        String macroTranslationKey = "rendering.macro." + macroDescriptor.getId();

        MacroDescriptorUI result = new MacroDescriptorUI(macroDescriptor.getId().getId())
            .setName(getParameterTranslation(macroTranslationKey + DOT_NAME, macroDescriptor.getName()))
            .setDescription(getParameterTranslation(macroTranslationKey + DOT_DESCRIPTION,
                macroDescriptor.getDescription()))
            .setSupportsInlineMode(macroDescriptor.supportsInlineMode());

        Map<String, AbstractMacroUINode> parametersMap = new LinkedHashMap<>();
        Map<String, SortedSet<AbstractMacroUINode>> childrenMap = new HashMap<>();
        List<AbstractMacroUINode> mandatoryParameters = new ArrayList<>();
        List<AbstractMacroUINode> optionalParameters = new ArrayList<>();
        Map<String, MacroUINodeGroup> groupMap = new HashMap<>();
        Map<String, List<MacroUINodeGroup>> featureMap = new HashMap<>();

        MacroUINodeGroup defaultOptionalGroup =
            createOrGetGroup(DEFAULT_GROUP_OPTIONALS_ID, null, false, groupMap, featureMap,
                childrenMap, macroTranslationKey)
                .setName(getParameterTranslation("rendering.macro.config.defaultOptionalGroup.name",
                    "Optional parameters"));

        handleContentDescriptor(macroDescriptor, macroTranslationKey, mandatoryParameters, childrenMap,
            defaultOptionalGroup, parametersMap);

        Collection<ParameterDescriptor> parameterDescriptors = macroDescriptor.getParameterDescriptorMap().values();
        for (ParameterDescriptor parameterDescriptor : parameterDescriptors) {
            String parameterTranslationKey =
                String.format("%s.parameter.%s", macroTranslationKey, parameterDescriptor.getId());
            boolean mandatory = parameterDescriptor.isMandatory();
            MacroUINodeParameter node = new MacroUINodeParameter(parameterDescriptor.getId());
            parametersMap.put(node.getKey(), node);

            node.setAdvanced(parameterDescriptor.isAdvanced())
                .setDeprecated(parameterDescriptor.isDeprecated())
                .setCaseInsensitive(isEnum(parameterDescriptor.getDisplayType()))
                .setName(getParameterTranslation(parameterTranslationKey + DOT_NAME, parameterDescriptor.getName()))
                .setDescription(getParameterTranslation(parameterTranslationKey + DOT_DESCRIPTION,
                    parameterDescriptor.getDescription()))
                .setMandatory(mandatory)
                .setHidden(parameterDescriptor.isDisplayHidden())
                .setOrder(parameterDescriptor.getOrder());

            handleTypeValueAndTemplate(parameterDescriptor, node, parameterTranslationKey);

            PropertyGroupDescriptor groupDescriptor = parameterDescriptor.getGroupDescriptor();
            if (isGroupDescriptorDefined(groupDescriptor)) {
                handleGroupDescriptor(groupDescriptor,
                    groupMap,
                    featureMap,
                    childrenMap,
                    macroTranslationKey,
                    node);
            } else if (node.isMandatory()) {
                mandatoryParameters.add(node);
            } else {
                childrenMap.get(defaultOptionalGroup.getKey()).add(node);
            }
        }

        handleFeaturesAndGroups(featureMap, groupMap, childrenMap, macroTranslationKey, mandatoryParameters,
            optionalParameters, parametersMap);

        // force the priority to appear first.
        defaultOptionalGroup.setOrder(0);

        MacroUINodeComparator comparator = new MacroUINodeComparator();
        mandatoryParameters.sort(comparator);
        optionalParameters.sort(comparator);

        result.setMandatoryNodes(mandatoryParameters.stream().map(AbstractMacroUINode::getKey).toList())
            .setOptionalNodes(optionalParameters.stream().map(AbstractMacroUINode::getKey).toList())
            .setParametersMap(parametersMap);

        return result;
    }

    private void handleTypeValueAndTemplate(ParameterDescriptor parameterDescriptor, MacroUINodeParameter node,
        String parameterTranslationKey)
    {
        Type displayType = parameterDescriptor.getDisplayType();
        Type parameterType = displayType;
        Object defaultValue = parameterDescriptor.getDefaultValue();

        if (wysiwygConfiguration.inferMacroParameterTypeBasedOnDefaultValue()
            && displayType.equals(String.class)
            && ("true".equals(defaultValue) || "false".equals(defaultValue))) {
            parameterType = Boolean.class;
            defaultValue = Boolean.valueOf(String.valueOf(defaultValue));
        }

        String editTemplate = getEditTemplate(parameterDescriptor, parameterType, parameterTranslationKey);
        node.setDisplayType(parameterType.getTypeName())
            .setDefaultValue(defaultValue)
            .setEditTemplate(editTemplate);
    }

    private void handleFeaturesAndGroups(Map<String, List<MacroUINodeGroup>> featureMap,
        Map<String, MacroUINodeGroup> groupMap,
        Map<String, SortedSet<AbstractMacroUINode>> childrenMap, String macroTranslationKey,
        List<AbstractMacroUINode> mandatoryParameters, List<AbstractMacroUINode> optionalParameters,
        Map<String, AbstractMacroUINode> parametersMap)
    {
        Set<String> belongToFeature = new HashSet<>();
        for (Map.Entry<String, List<MacroUINodeGroup>> featureEntry : featureMap.entrySet()) {
            String featureName = featureEntry.getKey();
            List<MacroUINodeGroup> featureGroups = featureEntry.getValue();
            MacroUINodeGroup featureNode = createOrGetGroup(featureName, featureName, true, groupMap,
                featureMap, childrenMap, macroTranslationKey);
            childrenMap.get(featureNode.getKey()).addAll(featureGroups);
            featureNode.setMandatory(featureGroups.stream().anyMatch(MacroUINodeGroup::isMandatory));
            featureNode.setOrder(featureGroups.stream()
                .map(MacroUINodeGroup::getOrder)
                .filter(i -> i != -1)
                .min(Integer::compareTo)
                .orElse(-1));

            belongToFeature.addAll(featureGroups.stream().map(MacroUINodeGroup::getKey).toList());
        }

        for (MacroUINodeGroup groupNode : groupMap.values()) {
            SortedSet<AbstractMacroUINode> children = childrenMap.get(groupNode.getKey());
            if (!children.isEmpty() && !belongToFeature.contains(groupNode.getKey())) {
                if (groupNode.isMandatory()) {
                    mandatoryParameters.add(groupNode);
                } else {
                    optionalParameters.add(groupNode);
                }
            }
            if (!children.isEmpty()) {
                groupNode.setChildren(children.stream().map(AbstractMacroUINode::getKey).toList());
                children.forEach(child -> child.setParent(groupNode.getKey()));
                parametersMap.put(groupNode.getKey(), groupNode);
            }
        }
    }

    private void handleContentDescriptor(MacroDescriptor macroDescriptor, String macroTranslationKey,
        List<AbstractMacroUINode> mandatoryParameters, Map<String, SortedSet<AbstractMacroUINode>> childrenMap,
        MacroUINodeGroup defaultOptionalGroup, Map<String, AbstractMacroUINode> parametersMap)
    {
        ContentDescriptor contentDescriptor = macroDescriptor.getContentDescriptor();
        if (contentDescriptor != null) {
            MacroUINodeParameter contentNode =
                new MacroUINodeParameter("$content")
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
                childrenMap.get(defaultOptionalGroup.getKey()).add(contentNode);
            }
            parametersMap.put(contentNode.getKey(), contentNode);
        }
    }

    private void handleGroupDescriptor(PropertyGroupDescriptor groupDescriptor, Map<String, MacroUINodeGroup> groupMap,
        Map<String, List<MacroUINodeGroup>> featureMap, Map<String, SortedSet<AbstractMacroUINode>> childrenMap,
        String macroTranslationKey, MacroUINodeParameter node)
    {
        String featureName = groupDescriptor.getFeature();
        String groupName = StringUtils.join(groupDescriptor.getGroup(), ",");
        boolean featureOnly = false;

        if (groupDescriptor.getGroup() == null || groupDescriptor.getGroup().isEmpty()) {
            groupName = featureName;
            featureOnly = true;
        }

        MacroUINodeGroup groupNode = createOrGetGroup(groupName, featureName, featureOnly,
            groupMap, featureMap, childrenMap, macroTranslationKey);

        if ((node.isMandatory() || groupDescriptor.isFeatureMandatory()) && !groupNode.isMandatory()) {
            groupNode.setMandatory(true);
        }

        childrenMap.get(groupNode.getKey()).add(node);
        if (node.getOrder() > -1 && (groupNode.getOrder() == -1 || node.getOrder() < groupNode.getOrder())) {
            groupNode.setOrder(node.getOrder());
        }
    }

    private boolean isGroupDescriptorDefined(PropertyGroupDescriptor groupDescriptor)
    {
        return groupDescriptor != null
            && !(StringUtils.isEmpty(groupDescriptor.getFeature())
            && (groupDescriptor.getGroup() == null || groupDescriptor.getGroup().isEmpty()));
    }

    private MacroUINodeGroup createOrGetGroup(String id,
        String featureName,
        boolean featureOnly,
        Map<String, MacroUINodeGroup> groupMap,
        Map<String, List<MacroUINodeGroup>> featureMap,
        Map<String, SortedSet<AbstractMacroUINode>> childrenMap,
        String macroTranslationKey)
    {
        String mapId = (featureOnly) ? "FEATURE:" + id : id;
        return groupMap.computeIfAbsent(mapId, key -> {
            String groupNameKey =
                String.format("%s.%s.%s.name", macroTranslationKey, "group", id);
            boolean isFeature = !StringUtils.isEmpty(featureName);
            MacroUINodeGroup group = new MacroUINodeGroup(id)
                .setFeatureName(featureName)
                .setFeature(isFeature)
                .setFeatureOnly(featureOnly)
                .setName(getParameterTranslation(groupNameKey, id));
            childrenMap.put(group.getKey(), new TreeSet<>(new MacroUINodeComparator()));
            if (isFeature && !featureOnly) {
                featureMap.computeIfAbsent(featureName, featureKey -> new ArrayList<>()).add(group);
            }
            return group;
        });
    }

    private String getParameterTranslation(String translationKey, String fallback)
    {
        String result = this.localizationManager.getTranslationPlain(translationKey);
        return (result != null) ? result : fallback;
    }

    // TODO: double check there's no regression with https://jira.xwiki.org/browse/XWIKI-20491 and maybe remove
    //  sourceDocument in macroEditor calls
    private String getEditTemplate(ParameterDescriptor parameterDescriptor, Type parameterType,
        String parameterTranslationKey)
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
            result = this.htmlDisplayerManager
                .display(parameterType, parameterDescriptor.getDefaultValue(), displayerMap, "edit");
        } catch (HTMLDisplayerException e) {
            this.logger.error("Error when computing edit template for parameter [{}] of type [{}]. "
                + "Falling back on default template.", parameterDescriptor.getId(),
                parameterType, e);
        }
        if (StringUtils.isEmpty(result)) {
            result = getEditTemplateFallback(parameterDescriptor, parameterType, parameterTranslationKey);
        }
        return result.trim();
    }

    private boolean isEnum(Type displayType)
    {
        return (displayType instanceof Class classType && classType.isEnum());
    }

    private String getEditTemplateFallback(ParameterDescriptor parameterDescriptor, Type parameterType,
        String parameterTranslationKey)
    {
        String result;
        if (parameterType == Boolean.class) {
            result = String.format(BOOLEAN_TEMPLATE_FALLBACK, escapeTool.xml(parameterDescriptor.getId()));
        } else if (isEnum(parameterType)) {
            StringBuilder options = new StringBuilder();
            for (Object enumConstant : ((Class) parameterType).getEnumConstants()) {
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
