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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceString;
import org.xwiki.model.reference.PageReference;
import org.xwiki.properties.PropertyGroupDescriptor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wysiwyg.macro.AbstractMacroUINode;
import org.xwiki.wysiwyg.macro.MacroDescriptorUI;
import org.xwiki.wysiwyg.macro.MacroUINodeGroup;
import org.xwiki.wysiwyg.macro.MacroUINodeParameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MacroDescriptorUIFactory}.
 *
 * @version $Id$
 * @since 17.5.0
 */
@ComponentTest
class MacroDescriptorUIFactoryTest
{
    @InjectMockComponents
    private MacroDescriptorUIFactory helper;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    private enum MyEnum {
        FOO,
        BAR,
        THING
    }

    @Test
    void buildMacroDescriptorUI()
    {
        ParameterDescriptor param1 = mock(ParameterDescriptor.class);
        when(param1.getId()).thenReturn("param1");
        when(param1.getName()).thenReturn("param1Name");
        when(param1.isMandatory()).thenReturn(true);
        when(param1.getOrder()).thenReturn(1);
        when(param1.getDisplayType()).thenReturn(String.class);

        ParameterDescriptor param2 = mock(ParameterDescriptor.class);
        when(param2.getOrder()).thenReturn(2);
        when(param2.getId()).thenReturn("param2");
        when(param2.getName()).thenReturn("param2Name");
        when(param2.getDisplayType()).thenReturn(Boolean.class);

        ParameterDescriptor param3 = mock(ParameterDescriptor.class);
        when(param3.isDeprecated()).thenReturn(true);
        when(param3.getOrder()).thenReturn(3);
        when(param3.getId()).thenReturn("param3");
        when(param3.getName()).thenReturn("param3Name");
        when(param3.getDisplayType()).thenReturn(String.class);

        PropertyGroupDescriptor group1 = mock(PropertyGroupDescriptor.class);
        when(group1.getFeature()).thenReturn("myFeature");
        when(group1.isFeatureMandatory()).thenReturn(true);

        ParameterDescriptor param4 = mock(ParameterDescriptor.class);
        when(param4.getId()).thenReturn("param4");
        when(param4.getName()).thenReturn("param4Name");
        when(param4.getGroupDescriptor()).thenReturn(group1);
        when(param4.getOrder()).thenReturn(4);
        when(param4.getDisplayType()).thenReturn(String.class);

        ParameterDescriptor param5 = mock(ParameterDescriptor.class);
        when(param5.getId()).thenReturn("param5");
        when(param5.getName()).thenReturn("param5Name");
        when(param5.getOrder()).thenReturn(5);
        when(param5.isMandatory()).thenReturn(true);
        when(param5.getDisplayType()).thenReturn(MyEnum.class);

        ParameterDescriptor param6 = mock(ParameterDescriptor.class);
        when(param6.getId()).thenReturn("param6");
        when(param6.getName()).thenReturn("param6Name");
        when(param6.getOrder()).thenReturn(6);
        when(param6.getGroupDescriptor()).thenReturn(group1);
        when(param6.getDisplayType()).thenReturn(String.class);

        ParameterDescriptor param7 = mock(ParameterDescriptor.class);
        when(param7.getId()).thenReturn("param7");
        when(param7.getName()).thenReturn("param7Name");
        when(param7.getOrder()).thenReturn(-1);
        when(param7.getDisplayType()).thenReturn(String.class);

        ParameterDescriptor param8 = mock(ParameterDescriptor.class);
        when(param8.getId()).thenReturn("param8");
        when(param8.getName()).thenReturn("param8Name");
        when(param8.isMandatory()).thenReturn(true);
        when(param8.getOrder()).thenReturn(-1);
        when(param8.getDisplayType()).thenReturn(String.class);

        PropertyGroupDescriptor group2 = mock(PropertyGroupDescriptor.class);
        when(group2.getGroup()).thenReturn(List.of("someGroup"));

        ParameterDescriptor param9 = mock(ParameterDescriptor.class);
        when(param9.getId()).thenReturn("param9");
        when(param9.getName()).thenReturn("param9Name");
        when(param9.getGroupDescriptor()).thenReturn(group2);
        when(param9.getOrder()).thenReturn(-1);
        when(param9.getDisplayType()).thenReturn(String.class);

        ParameterDescriptor param10 = mock(ParameterDescriptor.class);
        when(param10.getId()).thenReturn("param10");
        when(param10.getName()).thenReturn("param10Name");
        when(param10.getGroupDescriptor()).thenReturn(group2);
        when(param10.getOrder()).thenReturn(8);
        when(param10.getDisplayType()).thenReturn(String.class);

        ParameterDescriptor param11 = mock(ParameterDescriptor.class);
        when(param11.getId()).thenReturn("param11");
        when(param11.getName()).thenReturn("param11Name");
        when(param11.isDisplayHidden()).thenReturn(true);
        when(param11.getOrder()).thenReturn(9);
        when(param11.getDisplayType()).thenReturn(String.class);

        ParameterDescriptor param12 = mock(ParameterDescriptor.class);
        when(param12.getId()).thenReturn("param12");
        when(param12.getName()).thenReturn("param12Name");
        when(param12.getOrder()).thenReturn(10);
        when(param12.isAdvanced()).thenReturn(true);
        when(param12.getDisplayType()).thenReturn(String.class);

        ParameterDescriptor param13 = mock(ParameterDescriptor.class);
        when(param13.getId()).thenReturn("param13");
        when(param13.getName()).thenReturn("param13Name");
        when(param13.getGroupDescriptor()).thenReturn(group2);
        when(param13.getOrder()).thenReturn(11);
        when(param13.getDisplayType()).thenReturn(String.class);

        ParameterDescriptor param14 = mock(ParameterDescriptor.class);
        when(param14.getId()).thenReturn("param14");
        when(param14.getName()).thenReturn("param14Name");
        when(param14.isAdvanced()).thenReturn(true);
        when(param14.getOrder()).thenReturn(-1);
        when(param14.getDisplayType()).thenReturn(String.class);

        ParameterDescriptor param15 = mock(ParameterDescriptor.class);
        when(param15.getId()).thenReturn("param15");
        when(param15.getName()).thenReturn("param15Name");
        when(param15.getOrder()).thenReturn(-1);
        when(param15.getDisplayType()).thenReturn(String.class);

        ParameterDescriptor param16 = mock(ParameterDescriptor.class);
        when(param16.getId()).thenReturn("param16");
        when(param16.getName()).thenReturn("param16Name");
        when(param16.getOrder()).thenReturn(12);
        when(param16.getDisplayType()).thenReturn(String.class);

        MacroDescriptor macroDescriptor = mock(MacroDescriptor.class);
        when(macroDescriptor.getId()).thenReturn(new MacroId("myMacro"));
        ContentDescriptor contentDescriptor = mock(ContentDescriptor.class);
        when(macroDescriptor.getContentDescriptor()).thenReturn(contentDescriptor);
        when(contentDescriptor.isMandatory()).thenReturn(false);
        when(contentDescriptor.getType()).thenReturn(new DefaultParameterizedType(null, List.class, Block.class));

        Map<String, ParameterDescriptor> descriptorMap = new LinkedHashMap<>();
        descriptorMap.put("param1", param1);
        descriptorMap.put("param2", param2);
        descriptorMap.put("param3", param3);
        descriptorMap.put("param4", param4);
        descriptorMap.put("param5", param5);
        descriptorMap.put("param6", param6);
        descriptorMap.put("param7", param7);
        descriptorMap.put("param8", param8);
        descriptorMap.put("param9", param9);
        descriptorMap.put("param10", param10);
        descriptorMap.put("param11", param11);
        descriptorMap.put("param12", param12);
        descriptorMap.put("param13", param13);
        descriptorMap.put("param14", param14);
        descriptorMap.put("param15", param15);
        descriptorMap.put("param16", param16);

        when(macroDescriptor.getParameterDescriptorMap()).thenReturn(descriptorMap);

        when(this.localizationManager.getTranslationPlain(anyString()))
            .then(invocationOnMock -> invocationOnMock.getArgument(0) + "Translated");
        MacroUINodeParameter nodeParam1 = new MacroUINodeParameter("param1");
        nodeParam1.setName("rendering.macro.myMacro.parameter.param1.nameTranslated");
        nodeParam1.setDescription("rendering.macro.myMacro.parameter.param1.descriptionTranslated");
        nodeParam1.setMandatory(true);
        nodeParam1.setOrder(1);
        nodeParam1.setDisplayType("java.lang.String");
        nodeParam1.setEditTemplate("<input type=\"text\" name=\"param1\" />");

        MacroUINodeParameter nodeParam2 = new MacroUINodeParameter("param2");
        nodeParam2.setName("rendering.macro.myMacro.parameter.param2.nameTranslated");
        nodeParam2.setDescription("rendering.macro.myMacro.parameter.param2.descriptionTranslated");
        nodeParam2.setOrder(2);
        nodeParam2.setDisplayType("java.lang.Boolean");
        nodeParam2.setEditTemplate("<input type=\"checkbox\" name=\"param2\" value=\"true\"/>\n"
            + "<input type=\"hidden\" name=\"param2\" value=\"false\"/>");

        MacroUINodeParameter nodeParam3 = new MacroUINodeParameter("param3");
        nodeParam3.setName("rendering.macro.myMacro.parameter.param3.nameTranslated");
        nodeParam3.setDescription("rendering.macro.myMacro.parameter.param3.descriptionTranslated");
        nodeParam3.setOrder(3);
        nodeParam3.setDeprecated(true);
        nodeParam3.setDisplayType("java.lang.String");
        nodeParam3.setEditTemplate("<input type=\"text\" name=\"param3\" />");

        MacroUINodeParameter nodeParam4 = new MacroUINodeParameter("param4");
        nodeParam4.setName("rendering.macro.myMacro.parameter.param4.nameTranslated");
        nodeParam4.setDescription("rendering.macro.myMacro.parameter.param4.descriptionTranslated");
        nodeParam4.setOrder(4);
        nodeParam4.setDisplayType("java.lang.String");
        nodeParam4.setEditTemplate("<input type=\"text\" name=\"param4\" />");

        MacroUINodeParameter nodeParam5 = new MacroUINodeParameter("param5");
        nodeParam5.setName("rendering.macro.myMacro.parameter.param5.nameTranslated");
        nodeParam5.setDescription("rendering.macro.myMacro.parameter.param5.descriptionTranslated");
        nodeParam5.setOrder(5);
        nodeParam5.setMandatory(true);
        nodeParam5.setCaseInsensitive(true);
        nodeParam5.setDisplayType("org.xwiki.wysiwyg.internal.macro.MacroDescriptorUIFactoryTest$MyEnum");
        nodeParam5.setEditTemplate("<select name=\"param5\">"
            + "<option value=\"FOO\">rendering.macro.myMacro.parameter.param5.value.FOOTranslated</option>"
            + "<option value=\"BAR\">rendering.macro.myMacro.parameter.param5.value.BARTranslated</option>"
            + "<option value=\"THING\">rendering.macro.myMacro.parameter.param5.value.THINGTranslated</option>"
            + "</select>");

        MacroUINodeParameter nodeParam6 = new MacroUINodeParameter("param6");
        nodeParam6.setName("rendering.macro.myMacro.parameter.param6.nameTranslated");
        nodeParam6.setDescription("rendering.macro.myMacro.parameter.param6.descriptionTranslated");
        nodeParam6.setOrder(6);
        nodeParam6.setDisplayType("java.lang.String");
        nodeParam6.setEditTemplate("<input type=\"text\" name=\"param6\" />");

        MacroUINodeParameter nodeParam7 = new MacroUINodeParameter("param7");
        nodeParam7.setName("rendering.macro.myMacro.parameter.param7.nameTranslated");
        nodeParam7.setDescription("rendering.macro.myMacro.parameter.param7.descriptionTranslated");
        nodeParam7.setDisplayType("java.lang.String");
        nodeParam7.setEditTemplate("<input type=\"text\" name=\"param7\" />");

        MacroUINodeParameter nodeParam8 = new MacroUINodeParameter("param8");
        nodeParam8.setName("rendering.macro.myMacro.parameter.param8.nameTranslated");
        nodeParam8.setDescription("rendering.macro.myMacro.parameter.param8.descriptionTranslated");
        nodeParam8.setMandatory(true);
        nodeParam8.setDisplayType("java.lang.String");
        nodeParam8.setEditTemplate("<input type=\"text\" name=\"param8\" />");

        MacroUINodeParameter nodeParam9 = new MacroUINodeParameter("param9");
        nodeParam9.setName("rendering.macro.myMacro.parameter.param9.nameTranslated");
        nodeParam9.setDescription("rendering.macro.myMacro.parameter.param9.descriptionTranslated");
        nodeParam9.setDisplayType("java.lang.String");
        nodeParam9.setEditTemplate("<input type=\"text\" name=\"param9\" />");

        MacroUINodeParameter nodeParam10 = new MacroUINodeParameter("param10");
        nodeParam10.setName("rendering.macro.myMacro.parameter.param10.nameTranslated");
        nodeParam10.setDescription("rendering.macro.myMacro.parameter.param10.descriptionTranslated");
        nodeParam10.setOrder(8);
        nodeParam10.setDisplayType("java.lang.String");
        nodeParam10.setEditTemplate("<input type=\"text\" name=\"param10\" />");

        MacroUINodeParameter nodeParam11 = new MacroUINodeParameter("param11");
        nodeParam11.setName("rendering.macro.myMacro.parameter.param11.nameTranslated");
        nodeParam11.setDescription("rendering.macro.myMacro.parameter.param11.descriptionTranslated");
        nodeParam11.setOrder(9);
        nodeParam11.setHidden(true);
        nodeParam11.setDisplayType("java.lang.String");
        nodeParam11.setEditTemplate("<input type=\"text\" name=\"param11\" />");

        MacroUINodeParameter nodeParam12 = new MacroUINodeParameter("param12");
        nodeParam12.setName("rendering.macro.myMacro.parameter.param12.nameTranslated");
        nodeParam12.setDescription("rendering.macro.myMacro.parameter.param12.descriptionTranslated");
        nodeParam12.setOrder(10);
        nodeParam12.setAdvanced(true);
        nodeParam12.setDisplayType("java.lang.String");
        nodeParam12.setEditTemplate("<input type=\"text\" name=\"param12\" />");

        MacroUINodeParameter nodeParam13 = new MacroUINodeParameter("param13");
        nodeParam13.setName("rendering.macro.myMacro.parameter.param13.nameTranslated");
        nodeParam13.setDescription("rendering.macro.myMacro.parameter.param13.descriptionTranslated");
        nodeParam13.setOrder(11);
        nodeParam13.setDisplayType("java.lang.String");
        nodeParam13.setEditTemplate("<input type=\"text\" name=\"param13\" />");

        MacroUINodeParameter nodeParam14 = new MacroUINodeParameter("param14");
        nodeParam14.setName("rendering.macro.myMacro.parameter.param14.nameTranslated");
        nodeParam14.setDescription("rendering.macro.myMacro.parameter.param14.descriptionTranslated");
        nodeParam14.setAdvanced(true);
        nodeParam14.setDisplayType("java.lang.String");
        nodeParam14.setEditTemplate("<input type=\"text\" name=\"param14\" />");

        MacroUINodeParameter nodeParam15 = new MacroUINodeParameter("param15");
        nodeParam15.setName("rendering.macro.myMacro.parameter.param15.nameTranslated");
        nodeParam15.setDescription("rendering.macro.myMacro.parameter.param15.descriptionTranslated");
        nodeParam15.setDisplayType("java.lang.String");
        nodeParam15.setEditTemplate("<input type=\"text\" name=\"param15\" />");

        MacroUINodeParameter nodeParam16 = new MacroUINodeParameter("param16");
        nodeParam16.setName("rendering.macro.myMacro.parameter.param16.nameTranslated");
        nodeParam16.setDescription("rendering.macro.myMacro.parameter.param16.descriptionTranslated");
        nodeParam16.setOrder(12);
        nodeParam16.setDisplayType("java.lang.String");
        nodeParam16.setEditTemplate("<input type=\"text\" name=\"param16\" />");

        MacroUINodeParameter nodeContent = new MacroUINodeParameter("$content");
        nodeContent.setName("rendering.macroContentTranslated");
        nodeContent.setDescription("rendering.macro.myMacrocontent.descriptionTranslated");
        nodeContent.setMandatory(false);
        nodeContent.setDisplayType("java.util.List<org.xwiki.rendering.block.Block>");
        nodeContent.setEditTemplate("<textarea name=\"$content\" rows=\"7\"></textarea>");
        nodeContent.setOrder(0);

        MacroUINodeGroup featureNode = new MacroUINodeGroup("myFeature");
        featureNode.setFeature(true);
        featureNode.setFeatureOnly(true);
        featureNode.setFeatureName("myFeature");
        featureNode.setName("rendering.macro.myMacro.group.myFeature.nameTranslated");
        featureNode.setOrder(4);
        featureNode.setMandatory(true);
        featureNode.setChildren(List.of(
            "PARAMETER:param4",
            "PARAMETER:param6"
        ));

        MacroUINodeGroup groupNode = new MacroUINodeGroup("someGroup");
        groupNode.setName("rendering.macro.myMacro.group.someGroup.nameTranslated");
        groupNode.setOrder(8);
        groupNode.setChildren(List.of(
            "PARAMETER:param10",
            "PARAMETER:param13",
            "PARAMETER:param9"
        ));

        MacroUINodeGroup defaultGroupNode = new MacroUINodeGroup("defaultOptionalGroup");
        defaultGroupNode.setName("rendering.macro.config.defaultOptionalGroup.nameTranslated");
        defaultGroupNode.setOrder(0);
        defaultGroupNode.setChildren(List.of(
            "PARAMETER:$content",
            "PARAMETER:param2",
            "PARAMETER:param3",
            "PARAMETER:param11",
            "PARAMETER:param12",
            "PARAMETER:param16",
            "PARAMETER:param15",
            "PARAMETER:param7",
            "PARAMETER:param14"
        ));

        MacroDescriptorUI expectedMacroDescriptorUI = new MacroDescriptorUI("myMacro")
            .setName("rendering.macro.myMacro.nameTranslated")
            .setDescription("rendering.macro.myMacro.descriptionTranslated");

        expectedMacroDescriptorUI.setMandatoryNodes(List.of(
            "PARAMETER:param1", // mandatory and order 1
            "FEATURE:myFeature", // mandatory feature with best order
            "PARAMETER:param5",
            "PARAMETER:param8"
        ));
        expectedMacroDescriptorUI.setOptionalNodes(List.of(
            "GROUP:defaultOptionalGroup",
            "GROUP:someGroup"
        ));

        Map<String, AbstractMacroUINode> parametersMap = new LinkedHashMap<>();
        parametersMap.put("PARAMETER:$content", nodeContent);
        parametersMap.put("PARAMETER:param1", nodeParam1);
        parametersMap.put("PARAMETER:param2", nodeParam2);
        parametersMap.put("PARAMETER:param3", nodeParam3);
        parametersMap.put("PARAMETER:param4", nodeParam4);
        parametersMap.put("PARAMETER:param5", nodeParam5);
        parametersMap.put("PARAMETER:param6", nodeParam6);
        parametersMap.put("PARAMETER:param7", nodeParam7);
        parametersMap.put("PARAMETER:param8", nodeParam8);
        parametersMap.put("PARAMETER:param9", nodeParam9);
        parametersMap.put("PARAMETER:param10", nodeParam10);
        parametersMap.put("PARAMETER:param11", nodeParam11);
        parametersMap.put("PARAMETER:param12", nodeParam12);
        parametersMap.put("PARAMETER:param13", nodeParam13);
        parametersMap.put("PARAMETER:param14", nodeParam14);
        parametersMap.put("PARAMETER:param15", nodeParam15);
        parametersMap.put("PARAMETER:param16", nodeParam16);
        parametersMap.put("FEATURE:myFeature", featureNode);
        parametersMap.put("GROUP:defaultOptionalGroup", defaultGroupNode);
        parametersMap.put("GROUP:someGroup", groupNode);

        expectedMacroDescriptorUI.setParametersMap(parametersMap);

        MacroDescriptorUI macroDescriptorUI = this.helper.buildMacroDescriptorUI(macroDescriptor);
        assertEquals(expectedMacroDescriptorUI, macroDescriptorUI);
    }

    enum TestEnum
    {
        VALUE1,
        VALUE2
    }

    @Test
    void buildMacroDescriptorUIWithIncludeMacroParameters()
    {
        // include macro is an interesting case of a mix of group and feature so we reuse it as a good real life example

        PropertyGroupDescriptor stringReferenceDescriptor = mock(PropertyGroupDescriptor.class);
        when(stringReferenceDescriptor.getGroup()).thenReturn(List.of("stringReference"));
        when(stringReferenceDescriptor.getFeature()).thenReturn("reference");

        ParameterDescriptor referenceParameter = mock(ParameterDescriptor.class);
        when(referenceParameter.getId()).thenReturn("reference");
        when(referenceParameter.getDisplayType()).thenReturn(EntityReferenceString.class);
        when(referenceParameter.getParameterType()).thenReturn(String.class);
        when(referenceParameter.getGroupDescriptor()).thenReturn(stringReferenceDescriptor);
        when(referenceParameter.getOrder()).thenReturn(-1);

        ParameterDescriptor sectionParameter = mock(ParameterDescriptor.class);
        when(sectionParameter.getId()).thenReturn("section");
        when(sectionParameter.isAdvanced()).thenReturn(true);
        when(sectionParameter.getDisplayType()).thenReturn(String.class);
        when(sectionParameter.getOrder()).thenReturn(-1);

        ParameterDescriptor excludeFirstHeadingParameter = mock(ParameterDescriptor.class);
        when(excludeFirstHeadingParameter.getId()).thenReturn("excludeFirstHeading");
        when(excludeFirstHeadingParameter.isAdvanced()).thenReturn(true);
        when(excludeFirstHeadingParameter.getDisplayType()).thenReturn(String.class);
        when(excludeFirstHeadingParameter.getOrder()).thenReturn(-1);

        ParameterDescriptor contextParameter = mock(ParameterDescriptor.class);
        when(contextParameter.getId()).thenReturn("context");
        when(contextParameter.isAdvanced()).thenReturn(true);
        when(contextParameter.isDeprecated()).thenReturn(true);
        when(contextParameter.getDisplayType()).thenReturn(String.class);
        when(contextParameter.getOrder()).thenReturn(-1);

        ParameterDescriptor typeParameter = mock(ParameterDescriptor.class);
        when(typeParameter.getId()).thenReturn("type");
        when(typeParameter.isAdvanced()).thenReturn(true);
        when(typeParameter.isDisplayHidden()).thenReturn(true);
        when(typeParameter.getParameterType()).thenReturn(EntityType.class);
        when(typeParameter.getGroupDescriptor()).thenReturn(stringReferenceDescriptor);
        when(typeParameter.getDisplayType()).thenReturn(EntityType.class);
        when(typeParameter.getOrder()).thenReturn(-1);

        PropertyGroupDescriptor featureReferenceDescriptor = mock(PropertyGroupDescriptor.class);
        when(featureReferenceDescriptor.getFeature()).thenReturn("reference");

        ParameterDescriptor pageParameter = mock(ParameterDescriptor.class);
        when(pageParameter.getId()).thenReturn("page");
        when(pageParameter.isDisplayHidden()).thenReturn(true);
        when(pageParameter.getGroupDescriptor()).thenReturn(featureReferenceDescriptor);
        when(pageParameter.getParameterType()).thenReturn(String.class);
        when(pageParameter.getDisplayType()).thenReturn(PageReference.class);
        when(pageParameter.getOrder()).thenReturn(-1);

        ParameterDescriptor authorParameter = mock(ParameterDescriptor.class);
        when(authorParameter.getId()).thenReturn("author");
        when(authorParameter.isAdvanced()).thenReturn(true);
        when(authorParameter.getParameterType()).thenReturn(TestEnum.class);
        when(authorParameter.getDisplayType()).thenReturn(TestEnum.class);
        when(authorParameter.getOrder()).thenReturn(-1);

        MacroDescriptor includeMacroDescriptor = mock(MacroDescriptor.class);
        when(includeMacroDescriptor.getId()).thenReturn(new MacroId("include"));
        when(includeMacroDescriptor.getParameterDescriptorMap()).thenReturn(Map.of(
            "reference", referenceParameter,
            "section", sectionParameter,
            "excludeFirstHeading", excludeFirstHeadingParameter,
            "context", contextParameter,
            "type", typeParameter,
            "page", pageParameter,
            "author", authorParameter
        ));
        when(includeMacroDescriptor.supportsInlineMode()).thenReturn(true);

        MacroDescriptorUI expectedMacroDescriptorUI = new MacroDescriptorUI("include")
            .setSupportsInlineMode(true);

        MacroUINodeParameter referenceNode = new MacroUINodeParameter("reference")
            .setEditTemplate("<input type=\"text\" name=\"reference\" />")
            .setDisplayType("org.xwiki.model.reference.EntityReferenceString");

        MacroUINodeParameter sectionNode = new MacroUINodeParameter("section")
            .setEditTemplate("<input type=\"text\" name=\"section\" />")
            .setDisplayType("java.lang.String")
            .setAdvanced(true);

        MacroUINodeParameter excludeFirstHeadingNode = new MacroUINodeParameter("excludeFirstHeading")
            .setEditTemplate("<input type=\"text\" name=\"excludeFirstHeading\" />")
            .setDisplayType("java.lang.String")
            .setAdvanced(true);

        MacroUINodeParameter contextNode = new MacroUINodeParameter("context")
            .setEditTemplate("<input type=\"text\" name=\"context\" />")
            .setDisplayType("java.lang.String")
            .setAdvanced(true)
            .setDeprecated(true);

        MacroUINodeParameter typeNode = new MacroUINodeParameter("type")
            .setDisplayType("org.xwiki.model.EntityType")
            .setEditTemplate("<select name=\"type\">"
                + "<option value=\"WIKI\">WIKI</option>"
                + "<option value=\"SPACE\">SPACE</option>"
                + "<option value=\"DOCUMENT\">DOCUMENT</option>"
                + "<option value=\"ATTACHMENT\">ATTACHMENT</option>"
                + "<option value=\"OBJECT\">OBJECT</option>"
                + "<option value=\"OBJECT_PROPERTY\">OBJECT_PROPERTY</option>"
                + "<option value=\"CLASS_PROPERTY\">CLASS_PROPERTY</option>"
                + "<option value=\"BLOCK\">BLOCK</option>"
                + "<option value=\"PAGE\">PAGE</option>"
                + "<option value=\"PAGE_ATTACHMENT\">PAGE_ATTACHMENT</option>"
                + "<option value=\"PAGE_OBJECT\">PAGE_OBJECT</option>"
                + "<option value=\"PAGE_OBJECT_PROPERTY\">PAGE_OBJECT_PROPERTY</option>"
                + "<option value=\"PAGE_CLASS_PROPERTY\">PAGE_CLASS_PROPERTY</option>"
                + "</select>")
            .setCaseInsensitive(true)
            .setAdvanced(true)
            .setHidden(true);

        MacroUINodeParameter pageNode = new MacroUINodeParameter("page")
            .setDisplayType("org.xwiki.model.reference.PageReference")
            .setEditTemplate("<input type=\"text\" name=\"page\" />")
            .setHidden(true);

        MacroUINodeParameter authorNode = new MacroUINodeParameter("author")
            .setDisplayType("org.xwiki.wysiwyg.internal.macro.MacroDescriptorUIFactoryTest$TestEnum")
            .setCaseInsensitive(true)
            .setEditTemplate("<select name=\"author\">"
                + "<option value=\"VALUE1\">VALUE1</option>"
                + "<option value=\"VALUE2\">VALUE2</option>"
                + "</select>")
            .setAdvanced(true);

        MacroUINodeGroup stringReferenceGroup = new MacroUINodeGroup("stringReference")
            .setFeature(true)
            .setFeatureName("reference")
            .setChildren(List.of(
                "PARAMETER:reference",
                "PARAMETER:type"
            ))
            .setName("stringReference");

        MacroUINodeGroup referenceFeature = new MacroUINodeGroup("reference")
            .setFeature(true)
            .setFeatureName("reference")
            .setFeatureOnly(true)
            .setChildren(List.of("GROUP:stringReference", "PARAMETER:page"))
            .setName("reference");

        MacroUINodeGroup defaultOptionalGroup = new MacroUINodeGroup("defaultOptionalGroup")
            .setChildren(List.of(
                "PARAMETER:author",
                "PARAMETER:excludeFirstHeading",
                "PARAMETER:section",
                "PARAMETER:context"
            ))
            .setName("Optional parameters")
            .setOrder(0);

        expectedMacroDescriptorUI.setParametersMap(Map.of(
            "PARAMETER:reference", referenceNode,
            "PARAMETER:section", sectionNode,
            "PARAMETER:excludeFirstHeading", excludeFirstHeadingNode,
            "PARAMETER:context", contextNode,
            "PARAMETER:type", typeNode,
            "PARAMETER:page", pageNode,
            "PARAMETER:author", authorNode,
            "GROUP:stringReference", stringReferenceGroup,
            "FEATURE:reference", referenceFeature,
            "GROUP:defaultOptionalGroup", defaultOptionalGroup
        )).setOptionalNodes(List.of(
            "GROUP:defaultOptionalGroup",
            "FEATURE:reference"
        )).setMandatoryNodes(List.of());

        MacroDescriptorUI macroDescriptorUI = this.helper.buildMacroDescriptorUI(includeMacroDescriptor);
        assertEquals(expectedMacroDescriptorUI, macroDescriptorUI);
    }
}