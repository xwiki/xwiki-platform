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
package org.xwiki.rendering.internal.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.properties.PropertyGroupDescriptor;
import org.xwiki.rendering.internal.util.ui.MacroDescriptorUI;
import org.xwiki.rendering.internal.util.ui.MacroParameterUINode;
import org.xwiki.rendering.internal.util.ui.MacroParameterUINodeType;
import org.xwiki.rendering.internal.util.ui.MacroParametersHelper;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class MacroParametersHelperTest
{
    @InjectMockComponents
    private MacroParametersHelper helper;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

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
        when(param2.getDisplayType()).thenReturn(String.class);

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
        when(param5.getDisplayType()).thenReturn(String.class);

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
        MacroParameterUINode nodeParam1 = new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param1");
        nodeParam1.setName("param1NameTranslated");
        nodeParam1.setMandatory(true);
        nodeParam1.setOrder(1);
        nodeParam1.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam2 = new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param2");
        nodeParam2.setName("param2NameTranslated");
        nodeParam2.setOrder(2);
        nodeParam2.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam3 = new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param3");
        nodeParam3.setName("param3NameTranslated");
        nodeParam3.setOrder(3);
        nodeParam3.setDeprecated(true);
        nodeParam3.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam4 = new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param4");
        nodeParam4.setName("param4NameTranslated");
        nodeParam4.setOrder(4);
        nodeParam4.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam5 = new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param5");
        nodeParam5.setName("param5NameTranslated");
        nodeParam5.setOrder(5);
        nodeParam5.setMandatory(true);
        nodeParam5.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam6 = new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param6");
        nodeParam6.setName("param6NameTranslated");
        nodeParam6.setOrder(6);
        nodeParam6.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam7 = new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param7");
        nodeParam7.setName("param7NameTranslated");
        nodeParam7.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam8 = new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param8");
        nodeParam8.setName("param8NameTranslated");
        nodeParam8.setMandatory(true);
        nodeParam8.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam9 = new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param9");
        nodeParam9.setName("param9NameTranslated");
        nodeParam9.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam10 =
            new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param10");
        nodeParam10.setName("param10NameTranslated");
        nodeParam10.setOrder(8);
        nodeParam10.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam11 =
            new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param11");
        nodeParam11.setName("param11NameTranslated");
        nodeParam11.setOrder(9);
        nodeParam11.setHidden(true);
        nodeParam11.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam12 =
            new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param12");
        nodeParam12.setName("param12NameTranslated");
        nodeParam12.setOrder(10);
        nodeParam12.setAdvanced(true);
        nodeParam12.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam13 =
            new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param13");
        nodeParam13.setName("param13NameTranslated");
        nodeParam13.setOrder(11);
        nodeParam13.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam14 =
            new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param14");
        nodeParam14.setName("param14NameTranslated");
        nodeParam14.setAdvanced(true);
        nodeParam14.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam15 =
            new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param15");
        nodeParam15.setName("param15NameTranslated");
        nodeParam15.setDisplayType("java.lang.String");

        MacroParameterUINode nodeParam16 =
            new MacroParameterUINode(MacroParameterUINodeType.PARAMETER, "param16");
        nodeParam16.setName("param16NameTranslated");
        nodeParam16.setOrder(12);
        nodeParam16.setDisplayType("java.lang.String");

        MacroParameterUINode featureNode =
            new MacroParameterUINode(MacroParameterUINodeType.FEATURE, "myFeature");
        featureNode.setName("myFeatureTranslated");
        featureNode.setOrder(4);
        featureNode.setMandatory(true);
        featureNode.addChild(nodeParam4);
        featureNode.addChild(nodeParam6);

        MacroParameterUINode groupNode = new MacroParameterUINode(MacroParameterUINodeType.GROUP, "someGroup");
        groupNode.setName("someGroupTranslated");
        groupNode.setOrder(8);
        groupNode.addChild(nodeParam9);
        groupNode.addChild(nodeParam10);
        groupNode.addChild(nodeParam13);

        MacroParameterUINode defaultGroupNode = new MacroParameterUINode(MacroParameterUINodeType.GROUP,
            "default");
        defaultGroupNode.setName("rendering.macro.config.defaultOptionalGroupTranslated");
        defaultGroupNode.setOrder(0);
        defaultGroupNode.addChild(nodeParam2);
        defaultGroupNode.addChild(nodeParam3);
        defaultGroupNode.addChild(nodeParam12);
        defaultGroupNode.addChild(nodeParam16);
        defaultGroupNode.addChild(nodeParam15);
        defaultGroupNode.addChild(nodeParam7);
        defaultGroupNode.addChild(nodeParam14);

        MacroDescriptorUI expectedTree = new MacroDescriptorUI("id");

        expectedTree.setMandatoryNodes(List.of(
            nodeParam1.getKey(), // mandatory and order 1
            featureNode.getKey(), // mandatory feature with best order
            nodeParam5.getKey(),
            nodeParam8.getKey()
        ));
        expectedTree.setOptionalNodes(List.of(defaultGroupNode.getKey(), groupNode.getKey()));

        assertEquals(expectedTree, this.helper.buildMacroDescriptorUI(macroDescriptor));
    }
}